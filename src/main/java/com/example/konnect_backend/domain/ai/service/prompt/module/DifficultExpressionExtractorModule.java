package com.example.konnect_backend.domain.ai.service.prompt.module;

import com.example.konnect_backend.domain.ai.dto.response.DifficultExpressionDto;
import com.example.konnect_backend.domain.ai.infra.GeminiService;
import com.example.konnect_backend.domain.ai.service.pipeline.PipelineContext;
import com.example.konnect_backend.domain.ai.util.PromptUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 어려운 표현 추출 모듈 (Gemini API 사용)
 *
 * ## 모델 선택: gemini-2.0-flash-lite
 * - 이유: 단순한 텍스트 추출 작업
 * - 한국어 표현 → 설명 생성은 복잡하지 않음
 * - RPD: 1,000회/일로 여유로움
 * - 빠른 응답 속도, 비용 효율적
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DifficultExpressionExtractorModule implements PromptModule {

    private final GeminiService geminiService;

    @Getter
    private String lastRawResponse;
    @Getter
    private long lastProcessingTimeMs;

    // Lite 모델 사용 (단순 추출 작업)
    public static final String MODEL_NAME = "gemini-2.0-flash-lite";
    public static final double TEMPERATURE = 0.2;
    public static final int MAX_TOKENS = 1500;

    @Override
    public void process(String promptTemplate, PipelineContext context) {
        String extractedText = context.getExtractedText();

        long startTime = System.currentTimeMillis();
        try {
            String targetLanguage = context.getTargetLanguage() != null
                    ? context.getTargetLanguage().getDisplayName()
                    : "한국어";

            log.info("어려운 표현 추출 시작 (Gemini Lite 모델, 설명 언어: {})", targetLanguage);

            String promptContent = String.format(promptTemplate,
                    targetLanguage,
                    targetLanguage,
                    PromptUtils.truncateText(extractedText, 4000));

            // Gemini Lite 모델 사용 (preferPrimary = false)
            String response = geminiService.generateSimpleContent(promptContent, TEMPERATURE, MAX_TOKENS);

            this.lastRawResponse = response;
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;

            List<DifficultExpressionDto> expressions = parseResponse(response);

            context.addLog("어려운 표현 추출 완료: " + expressions.size() + "개");
            context.setDifficultExpressions(expressions);
            context.setCompletedStage(PipelineContext.PipelineStage.DIFFICULT_EXPRESSIONS_EXTRACTED);
        } catch (Exception e) {
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;
            log.error("어려운 표현 추출 실패", e);
            context.addLog("어려운 표현 추출 실패: " + e.getMessage());
        }
    }

    @Override
    public String getModuleName() {
        return "DIFFICULT_EXPRESSION_EXTRACTION";
    }

    private List<DifficultExpressionDto> parseResponse(String response) {
        try {
            String jsonStr = PromptUtils.extractJsonArray(response);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonStr, new TypeReference<List<DifficultExpressionDto>>() {});
        } catch (Exception e) {
            log.warn("어려운 표현 JSON 파싱 실패: {}", PromptUtils.truncateText(response, 200), e);
            return new ArrayList<>();
        }
    }
}
