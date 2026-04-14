package com.example.konnect_backend.domain.ai.aop;

import com.example.konnect_backend.domain.ai.domain.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.domain.vo.PipelineContext;
import com.example.konnect_backend.domain.ai.dto.internal.GeminiCallResult;
import com.example.konnect_backend.domain.ai.service.log.GeminiLogService;
import com.example.konnect_backend.domain.ai.service.module.PromptModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.example.konnect_backend.domain.ai.interceptor.AnalysisInterceptor.REQUEST_ID_KEY;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LlmLoggingAspect {

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
     * GeminiService 의 모둔 API 호출은 public 메소드이다. </br>
     * LlmCallMetadata, LlmCallRawData 를 저장한다.
     */
    @Around(value = "execution(public * com.example.konnect_backend.domain.ai.infra.GeminiService.*(..))")
    public Object logGeminiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        PromptContext promptContext = PromptContextHolder.get();
        String requestIdString = MDC.get(REQUEST_ID_KEY); // 모듈 자체를 비동기로 호출 시 주입 또는 전파 필요
        UUID requestId = UUID.fromString(requestIdString);

        try {
            GeminiCallResult callResult = (GeminiCallResult) joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            // 트랜잭션 분리
            logService.saveLog(requestId, callResult, promptContext, (int) elapsed);
            return callResult;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            logService.saveLog(requestId, null, promptContext, (int) elapsed);
            throw e;
        }
    }
}
