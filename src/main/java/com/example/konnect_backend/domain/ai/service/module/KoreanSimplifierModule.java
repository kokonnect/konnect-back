package com.example.konnect_backend.domain.ai.service.module;

import com.example.konnect_backend.domain.ai.dto.internal.GeminiCallResult;
import com.example.konnect_backend.domain.ai.domain.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.exception.DocumentAnalysisException;
import com.example.konnect_backend.domain.ai.infra.GeminiService;
import com.example.konnect_backend.domain.ai.domain.vo.TokenUsage;
import com.example.konnect_backend.domain.ai.domain.vo.PipelineContext;
import com.example.konnect_backend.domain.ai.service.prompt.PromptTemplateResolver;
import com.example.konnect_backend.domain.ai.util.PromptUtils;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 쉬운 한국어 변환 모듈 (Gemini API 사용)
 * <p>
 * ## 모델 선택: gemini-2.0-flash-lite
 * - 이유: 단순한 텍스트 변환 작업
 * - 어려운 한국어 → 쉬운 한국어 재작성
 * - RPD: 1,000회/일로 여유로움
 * - 빠른 응답 속도, 비용 효율적
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KoreanSimplifierModule implements PromptModule {

    private final GeminiService geminiService;
    private final PromptTemplateResolver resolver;

    // Lite 모델 사용 (단순 텍스트 변환)
    public static final String MODEL_NAME = "gemini-2.0-flash-lite";
    public static final double TEMPERATURE = 0.3;
    public static final int MAX_TOKENS = 4000;

    @Override
    public TokenUsage process(PromptTemplate promptTemplate, PipelineContext context) {
        Map<String, String> vars = getVars(context);
        String prompt = resolver.resolve(promptTemplate, vars);

        try {
            log.info("쉬운 한국어 재작성 시작 (Gemini Lite 모델)");
            long startTime = System.currentTimeMillis();

            // Gemini Lite 모델 사용 (preferPrimary = false)
            GeminiCallResult callResult = geminiService.generateSimpleContent(prompt, TEMPERATURE,
                MAX_TOKENS);
            String simplifiedText = callResult.response();

            if (simplifiedText == null || simplifiedText.isBlank()) {
                throw new DocumentAnalysisException(ErrorStatus.DOCUMENT_ANALYSIS_FAILED);
            }

            context.addLog("쉬운 한국어 재작성 완료: " + simplifiedText.length() + "자");
            context.setSimplifiedKorean(simplifiedText.trim());
            context.setCompletedStage(PipelineContext.PipelineStage.SIMPLIFIED);

            log.info("쉬운 한국어 재작성 소요시간 {} ms", System.currentTimeMillis() - startTime);

            return callResult.tokenUsage();
        } catch (DocumentAnalysisException e) {
            throw e;
        } catch (Exception e) {
            log.error("쉬운 한국어 재작성 실패", e);
            throw new DocumentAnalysisException(ErrorStatus.DOCUMENT_ANALYSIS_FAILED);
        }
    }

    public Map<String, String> getVars(PipelineContext context) {
        Map<String, String> vars = new HashMap<>();
        vars.put("text", PromptUtils.truncateText(context.getExtractedText(), 4000));

        return vars;
    }

    @Override
    public String getModuleName() {
        return "SIMPLIFICATION";
    }
}
