package com.example.konnect_backend.domain.ai.aop;

import com.example.konnect_backend.domain.ai.domain.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.domain.vo.PipelineContext;
import com.example.konnect_backend.domain.ai.dto.internal.GeminiCallResult;
import com.example.konnect_backend.domain.ai.exception.DocumentAnalysisException;
import com.example.konnect_backend.domain.ai.service.log.GeminiLogService;
import com.example.konnect_backend.domain.ai.service.module.PromptModule;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

import static com.example.konnect_backend.domain.ai.interceptor.AnalysisInterceptor.REQUEST_ID_KEY;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LlmLoggingAspect {

    private static final ThreadLocal<String> attemptedModelHolder = new ThreadLocal<>();

    private final GeminiLogService logService;

    /**
     * GeminiService 반환 시 로깅에 사용할 수 있도록 ThreadLocal 에 모듈명과 프롬프트 버전을 저장한다.
     */
    @Around(value = "execution(* com.example.konnect_backend.domain.ai.service.module.PromptModule.process(..)) && args(promptTemplate, context)",
            argNames = "joinPoint,promptTemplate,context")
    public Object saveModuleNameAndPromptVersionInContext(ProceedingJoinPoint joinPoint,
                                                          PromptTemplate promptTemplate,
                                                          PipelineContext context) throws Throwable {
        PromptModule module = (PromptModule) joinPoint.getTarget();
        PromptContextHolder.set(
            new PromptContext(
                module.getModuleName(),
                promptTemplate.getVersion(),
                module.getVars(context)
            )
        );

        try {
            return joinPoint.proceed();
        } finally {
            PromptContextHolder.clear();
        }
    }

    /**
     * 파이프라인 외부에서 GeminiService를 호출하는 메서드에 @LlmContext를 붙이면
     * AOP가 PromptContextHolder를 세팅하고 메서드 종료 후 정리한다.
     */
    @Around("@annotation(llmCtx)")
    public Object setContextFromAnnotation(ProceedingJoinPoint pjp, LlmContext llmCtx)
            throws Throwable {
        Map<String, String> vars = evaluateVarsExpression(pjp, llmCtx.varsExpression());
        PromptContextHolder.set(new PromptContext(llmCtx.moduleName(), llmCtx.promptVersion(), vars));
        try {
            return pjp.proceed();
        } finally {
            PromptContextHolder.clear();
        }
    }

    /**
     * GeminiRateLimitService의 모델 선택 결과를 ThreadLocal에 캡처한다.
     * call(String model, ...) 처럼 모델을 직접 지정하는 경우는 logGeminiCall에서 별도 처리.
     */
    @Around("execution(* com.example.konnect_backend.domain.ai.infra.GeminiRateLimitService.get*Model(..))")
    public Object captureAttemptedModel(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        if (result instanceof String model) {
            attemptedModelHolder.set(model);
        }
        return result;
    }

    /**
     * GeminiService 의 모든 API 호출은 public 메소드이다. </br>
     * LlmCallMetadata, LlmCallDetail 을 저장한다.
     */
    @Around(value = "execution(public * com.example.konnect_backend.domain.ai.infra.GeminiService.*(..))")
    public Object logGeminiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        PromptContext promptContext = PromptContextHolder.get();
        String requestIdString = MDC.get(REQUEST_ID_KEY);
        UUID requestId;
        try {
            if (requestIdString == null || requestIdString.isBlank()) {
                log.warn("MDC에 requestId 없음 — 새 UUID 생성. @LlmContext 또는 인터셉터 누락 여부 확인 필요");
                requestId = UUID.randomUUID();
            } else {
                requestId = UUID.fromString(requestIdString);
            }
        } catch (IllegalArgumentException e) {
            requestId = UUID.randomUUID();
        }

        // call(String model, ...) 은 rateLimitService를 거치지 않으므로 인자에서 직접 캡처
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        if ("call".equals(sig.getMethod().getName())) {
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] instanceof String modelArg) {
                attemptedModelHolder.set(modelArg);
            }
        }

        try {
            GeminiCallResult callResult = (GeminiCallResult) joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            logService.saveLog(requestId, callResult, promptContext, (int) elapsed);
            if (!"STOP".equals(callResult.finishReason())) {
                throw new DocumentAnalysisException(ErrorStatus.MAX_TOKEN_EXCEEDED);
            }
            return callResult;
        } catch (DocumentAnalysisException e) {
            throw e;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            logService.saveLog(requestId, null, promptContext, (int) elapsed, attemptedModelHolder.get());
            throw e;
        } finally {
            attemptedModelHolder.remove();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> evaluateVarsExpression(ProceedingJoinPoint pjp, String expression) {
        try {
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            String[] paramNames = sig.getParameterNames();
            Object[] args = pjp.getArgs();

            StandardEvaluationContext ctx = new StandardEvaluationContext();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    ctx.setVariable(paramNames[i], args[i]);
                }
            }
            Object result = new SpelExpressionParser().parseExpression(expression).getValue(ctx);
            return result instanceof Map ? (Map<String, String>) result : Map.of();
        } catch (Exception e) {
            log.warn("@LlmContext varsExpression 평가 실패: {}", expression, e);
            return Map.of();
        }
    }
}
