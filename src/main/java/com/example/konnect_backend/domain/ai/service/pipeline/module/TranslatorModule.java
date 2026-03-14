package com.example.konnect_backend.domain.ai.service.pipeline.module;

import com.example.konnect_backend.domain.ai.dto.internal.GeminiCallResult;
import com.example.konnect_backend.domain.ai.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.exception.DocumentAnalysisException;
import com.example.konnect_backend.domain.ai.infra.GeminiService;
import com.example.konnect_backend.domain.ai.model.vo.TokenUsage;
import com.example.konnect_backend.domain.ai.service.pipeline.PipelineContext;
import com.example.konnect_backend.domain.ai.service.prompt.PromptTemplateResolver;
import com.example.konnect_backend.domain.ai.util.PromptUtils;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 번역 모듈 (Gemini API 사용)
 * <p>
 * ## 모델 선택: gemini-2.0-flash-lite
 * - 이유: 번역은 상대적으로 단순한 작업
 * - 한국어 → 대상 언어 직접 번역
 * - RPD: 1,000회/일로 여유로움
 * - 빠른 응답 속도, 비용 효율적
 * - Gemini는 다국어 번역 품질이 우수함
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TranslatorModule implements PromptModule {

    private final GeminiService geminiService;
    private final PromptTemplateResolver resolver;

    // Lite 모델 사용 (단순 번역)
    public static final String MODEL_NAME = "gemini-2.0-flash-lite";
    public static final double TEMPERATURE = 0.3;
    public static final int MAX_TOKENS = 4000;

    @Override
    public TokenUsage process(PromptTemplate promptTemplate, PipelineContext context) {
        Map<String, String> vars = getVars(context);
        String prompt = resolver.resolve(promptTemplate, vars);

        try {
            log.info("번역 시작 (Gemini Lite 모델): 한국어 -> {}",
                context.getTargetLanguage().getDisplayName());
            long startTime = System.currentTimeMillis();

            // Gemini Lite 모델 사용 (preferPrimary = false)
            GeminiCallResult callResult = geminiService.generateSimpleContent(prompt, TEMPERATURE,
                MAX_TOKENS);
            String translatedText = callResult.response();

            if (translatedText == null || translatedText.isBlank()) {
                throw new DocumentAnalysisException(ErrorStatus.TRANSLATION_FAILED);
            }

            context.addLog("번역 완료: " + translatedText.length() + "자");
            context.setTranslatedText(translatedText);
            context.setCompletedStage(PipelineContext.PipelineStage.TRANSLATED);

            log.info("번역 소요시간 {} ms", System.currentTimeMillis() - startTime);

            return callResult.tokenUsage();
        } catch (DocumentAnalysisException e) {
            throw e;
        } catch (Exception e) {
            log.error("번역 실패", e);
            throw new DocumentAnalysisException(ErrorStatus.TRANSLATION_FAILED);
        }
    }

    @Override
    public String getModuleName() {
        return "TRANSLATION";
    }

    public Map<String, String> getVars(PipelineContext context) {
        String simplifiedKorean = context.getSimplifiedKorean();
        String targetLanguage = context.getTargetLanguage().getDisplayName();

        Map<String, String> vars = new HashMap<>();
        vars.put("text", PromptUtils.truncateText(simplifiedKorean, 4000));
        vars.put("target_language", targetLanguage);

        return vars;
    }
}
