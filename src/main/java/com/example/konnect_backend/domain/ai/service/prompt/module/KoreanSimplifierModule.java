package com.example.konnect_backend.domain.ai.service.prompt.module;

import com.example.konnect_backend.domain.ai.exception.DocumentAnalysisException;
import com.example.konnect_backend.domain.ai.infra.GeminiService;
import com.example.konnect_backend.domain.ai.service.pipeline.PipelineContext;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 쉬운 한국어 변환 모듈 (Gemini API 사용)
 *
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

    @Getter
    private String lastRawResponse;
    @Getter
    private long lastProcessingTimeMs;

    // Lite 모델 사용 (단순 텍스트 변환)
    public static final String MODEL_NAME = "gemini-2.0-flash-lite";
    public static final double TEMPERATURE = 0.3;
    public static final int MAX_TOKENS = 4000;

    @Override
    public void process(String promptTemplate, PipelineContext context) {
        String extractedText = context.getExtractedText();

        long startTime = System.currentTimeMillis();
        try {
            log.info("쉬운 한국어 재작성 시작 (Gemini Lite 모델)");

            String promptContent = String.format(promptTemplate, extractedText);

            // Gemini Lite 모델 사용 (preferPrimary = false)
            String simplifiedText = geminiService.generateSimpleContent(promptContent, TEMPERATURE, MAX_TOKENS).trim();

            this.lastRawResponse = simplifiedText;
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;

            if (simplifiedText == null || simplifiedText.isEmpty()) {
                throw new DocumentAnalysisException(ErrorStatus.DOCUMENT_ANALYSIS_FAILED);
            }

            context.addLog("쉬운 한국어 재작성 완료: " + simplifiedText.length() + "자");

            context.setSimplifiedKorean(simplifiedText);
            context.setCompletedStage(PipelineContext.PipelineStage.SIMPLIFIED);
        } catch (DocumentAnalysisException e) {
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;
            throw e;
        } catch (Exception e) {
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;
            log.error("쉬운 한국어 재작성 실패", e);
            throw new DocumentAnalysisException(ErrorStatus.DOCUMENT_ANALYSIS_FAILED);
        }
    }

    @Override
    public String getModuleName() {
        return "SIMPLIFICATION";
    }
}
