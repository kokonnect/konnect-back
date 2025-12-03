package com.example.konnect_backend.domain.ai.service.prompt;

import com.example.konnect_backend.domain.ai.exception.DocumentAnalysisException;
import com.example.konnect_backend.domain.ai.service.GeminiService;
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
public class SummarizerModule implements PromptModule<String, String> {

    private final GeminiService geminiService;

    @Getter
    private String lastRawResponse;
    @Getter
    private long lastProcessingTimeMs;

    // Lite 모델 사용 (단순 요약)
    public static final String MODEL_NAME = "gemini-2.0-flash-lite";
    public static final double TEMPERATURE = 0.3;
    public static final int MAX_TOKENS = 500;
    public static final String PROMPT_TEMPLATE_NAME = "SUMMARIZER_PROMPT_V1";

    private static final String SUMMARY_PROMPT_TEMPLATE = """
            다음 번역된 학교 가정통신문을 %s로 요약해주세요.

            ## 요약 지침
            - 핵심 내용을 3-5줄로 요약
            - 중요한 날짜, 장소, 준비물 정보는 반드시 포함
            - 학부모가 바로 이해할 수 있게 명확하게 작성
            - %s로 요약문만 출력하고 다른 설명은 하지 마세요

            ## 출력 형식 규칙 (필수)
            - 마크다운 문법 사용 금지 (###, **, *, -, |, 표 등 사용하지 않기)
            - 순수 텍스트로만 작성
            - 줄바꿈은 허용하되, 특수 기호나 서식 없이 일반 문장으로 작성

            ## 번역문
            %s

            ## 요약:
            """;

    @Override
    public String process(String text, PipelineContext context) {
        long startTime = System.currentTimeMillis();
        try {
            String targetLanguage = context.getTargetLanguage() != null
                    ? context.getTargetLanguage().getDisplayName()
                    : "한국어";

            log.info("요약 생성 시작 (Gemini Lite 모델): {}", targetLanguage);

            String promptContent = String.format(SUMMARY_PROMPT_TEMPLATE,
                    targetLanguage,
                    targetLanguage,
                    PromptUtils.truncateText(text, 6000));

            // Gemini Lite 모델 사용 (preferPrimary = false)
            String summary = geminiService.generateSimpleContent(promptContent, TEMPERATURE, MAX_TOKENS);

            this.lastRawResponse = summary;
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;

            if (summary == null || summary.trim().isEmpty()) {
                throw new DocumentAnalysisException(ErrorStatus.DOCUMENT_ANALYSIS_FAILED);
            }

            context.addLog("요약 생성 완료: " + summary.length() + "자");

            return summary.trim();

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
        return "Summarizer";
    }
}
