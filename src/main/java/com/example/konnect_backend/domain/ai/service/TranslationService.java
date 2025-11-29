package com.example.konnect_backend.domain.ai.service;

import com.example.konnect_backend.domain.ai.dto.request.TranslationRequest;
import com.example.konnect_backend.domain.ai.dto.response.TranslationResponse;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 텍스트 번역 서비스 (Gemini API 사용)
 *
 * ## 모델 선택: gemini-2.0-flash-lite
 * - 이유: 번역은 단순한 텍스트 변환 작업
 * - RPD: 1,000회/일로 여유로움
 * - 빠른 응답 속도, 비용 효율적
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {

    private final GeminiService geminiService;

    private static final String TRANSLATION_PROMPT_TEMPLATE = """
            다음 %s 텍스트를 한국어로 번역해주세요.
            %s

            원본 텍스트:
            %s

            번역 지침:
            - 자연스럽고 정확한 번역을 해주세요
            - 문맥과 의미를 충분히 고려해주세요
            - 번역문만 출력하고 다른 설명은 하지 마세요

            번역 결과:
            """;

    public TranslationResponse translate(TranslationRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("텍스트 번역 시작 (Gemini Lite): {} -> 한국어", request.getSourceLanguage().getDisplayName());

            String simpleLanguageNote = request.isUseSimpleLanguage()
                ? "가능한 한 간단하고 이해하기 쉬운 언어로 번역해주세요."
                : "";

            String prompt = String.format(TRANSLATION_PROMPT_TEMPLATE,
                    request.getSourceLanguage().getDisplayName(),
                    simpleLanguageNote,
                    request.getText());

            // Gemini Lite 모델 사용 (단순 번역)
            String translatedText = geminiService.generateSimpleContent(prompt, 0.3, 4000);

            if (translatedText == null || translatedText.trim().isEmpty()) {
                log.error("번역 결과가 비어있음");
                throw new GeneralException(ErrorStatus.TRANSLATION_FAILED);
            }

            long endTime = System.currentTimeMillis();
            log.info("번역 완료: {}ms", endTime - startTime);

            return TranslationResponse.builder()
                    .originalText(request.getText())
                    .translatedText(translatedText.trim())
                    .sourceLanguage(request.getSourceLanguage())
                    .sourceLanguageName(request.getSourceLanguage().getDisplayName())
                    .targetLanguage("한국어")
                    .usedSimpleLanguage(request.isUseSimpleLanguage())
                    .originalTextLength(request.getText().length())
                    .translatedTextLength(translatedText.trim().length())
                    .processingTimeMs(endTime - startTime)
                    .build();

        } catch (Exception e) {
            log.error("번역 중 오류 발생", e);
            throw new GeneralException(ErrorStatus.TRANSLATION_FAILED);
        }
    }
}
