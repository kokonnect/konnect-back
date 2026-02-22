package com.example.konnect_backend.domain.ai.service.prompt.module;

import com.example.konnect_backend.domain.ai.exception.DocumentAnalysisException;
import com.example.konnect_backend.domain.ai.infra.GeminiService;
import com.example.konnect_backend.domain.ai.service.pipeline.PipelineContext;
import com.example.konnect_backend.domain.ai.util.PromptUtils;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 요약 모듈 (Gemini API 사용)
 *
 * ## 모델 선택: gemini-2.0-flash-lite
 * - 이유: 요약은 단순한 텍스트 처리 작업
 * - 핵심 내용 추출 및 간략화
 * - RPD: 1,000회/일로 여유로움
 * - 빠른 응답 속도, 비용 효율적
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SummarizerModule implements PromptModule {

    private final GeminiService geminiService;

    @Getter
    private String lastRawResponse;
    @Getter
    private long lastProcessingTimeMs;

    // Lite 모델 사용 (단순 요약)
    public static final String MODEL_NAME = "gemini-2.0-flash-lite";
    public static final double TEMPERATURE = 0.3;
    public static final int MAX_TOKENS = 500;

    @Override
    public void process(String promptTemplate, PipelineContext context) {
        String simplifiedText = context.getSimplifiedKorean();
        long startTime = System.currentTimeMillis();
        try {
            String targetLanguage = context.getTargetLanguage() != null
                    ? context.getTargetLanguage().getDisplayName()
                    : "한국어";

            log.info("요약 생성 시작 (Gemini Lite 모델): {}", targetLanguage);

            String promptContent = String.format(promptTemplate,
                    targetLanguage,
                    targetLanguage,
                    PromptUtils.truncateText(simplifiedText, 6000));

            // Gemini Lite 모델 사용 (preferPrimary = false)
            String summary = geminiService.generateSimpleContent(promptContent, TEMPERATURE, MAX_TOKENS);

            this.lastRawResponse = summary;
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;

            if (summary == null || summary.trim().isEmpty()) {
                throw new DocumentAnalysisException(ErrorStatus.DOCUMENT_ANALYSIS_FAILED);
            }

            context.addLog("요약 생성 완료: " + summary.length() + "자");
            context.setSummary(summary);
            context.setCompletedStage(PipelineContext.PipelineStage.SUMMARIZED);
        } catch (DocumentAnalysisException e) {
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;
            throw e;
        } catch (Exception e) {
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;
            log.error("요약 생성 실패", e);
            throw new DocumentAnalysisException(ErrorStatus.DOCUMENT_ANALYSIS_FAILED);
        }
    }

    @Override
    public String getModuleName() {
        return "SUMMARIZATION";
    }
}
