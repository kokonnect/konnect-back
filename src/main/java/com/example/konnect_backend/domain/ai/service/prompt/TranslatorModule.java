package com.example.konnect_backend.domain.ai.service.prompt;

import com.example.konnect_backend.domain.ai.exception.DocumentAnalysisException;
import com.example.konnect_backend.domain.ai.service.GeminiService;
import com.example.konnect_backend.domain.ai.service.pipeline.PipelineContext;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 번역 모듈 (Gemini API 사용)
 *
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
public class TranslatorModule implements PromptModule<String, String> {

    private final GeminiService geminiService;

    @Getter
    private String lastRawResponse;
    @Getter
    private long lastProcessingTimeMs;

    // Lite 모델 사용 (단순 번역)
    public static final String MODEL_NAME = "gemini-2.0-flash-lite";
    public static final double TEMPERATURE = 0.3;
    public static final int MAX_TOKENS = 4000;
    public static final String PROMPT_TEMPLATE_NAME = "TRANSLATOR_PROMPT_V1";

    private static final String TRANSLATION_PROMPT_TEMPLATE = """
            다음 한국어 학교 가정통신문을 %s로 번역해주세요.
            %s

            ## 번역 지침
            - 자연스럽고 이해하기 쉬운 표현 사용
            - 학교 관련 전문 용어는 해당 국가의 일반적인 표현으로 번역
            - 날짜 형식은 현지 표기법 유지
            - 문단 구조 유지
            - 번역문만 출력하고 다른 설명은 하지 마세요

            ## 원문
            %s

            ## 번역:
            """;

    @Override
    public String process(String text, PipelineContext context) {
        long startTime = System.currentTimeMillis();
        try {
            String targetLanguage = context.getTargetLanguage() != null
                    ? context.getTargetLanguage().getDisplayName()
                    : "한국어";

            log.info("번역 시작 (Gemini Lite 모델): 한국어 -> {}", targetLanguage);

            String simpleLanguageNote = "";
            Object useSimple = context.getMetadata().get("useSimpleLanguage");
            if (Boolean.TRUE.equals(useSimple)) {
                simpleLanguageNote = "가능한 한 간단하고 이해하기 쉬운 언어로 번역해주세요.";
            }

            String promptContent = String.format(TRANSLATION_PROMPT_TEMPLATE,
                    targetLanguage,
                    simpleLanguageNote,
                    text);

            // Gemini Lite 모델 사용 (preferPrimary = false)
            String translatedText = geminiService.generateSimpleContent(promptContent, TEMPERATURE, MAX_TOKENS);

            this.lastRawResponse = translatedText;
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;

            if (translatedText == null || translatedText.trim().isEmpty()) {
                throw new DocumentAnalysisException(ErrorStatus.TRANSLATION_FAILED);
            }

            context.setTranslatedText(translatedText.trim());
            context.addLog("번역 완료: " + translatedText.length() + "자");

            return translatedText.trim();

        } catch (DocumentAnalysisException e) {
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;
            throw e;
        } catch (Exception e) {
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;
            log.error("번역 실패", e);
            throw new DocumentAnalysisException(ErrorStatus.TRANSLATION_FAILED);
        }
    }

    @Override
    public String getModuleName() {
        return "Translator";
    }
}
