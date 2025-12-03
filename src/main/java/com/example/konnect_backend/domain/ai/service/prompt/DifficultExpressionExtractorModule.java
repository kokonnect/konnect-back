package com.example.konnect_backend.domain.ai.service.prompt;

import com.example.konnect_backend.domain.ai.dto.response.DifficultExpressionDto;
import com.example.konnect_backend.domain.ai.service.GeminiService;
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
public class DifficultExpressionExtractorModule implements PromptModule<String, List<DifficultExpressionDto>> {

    private final GeminiService geminiService;

    @Getter
    private String lastRawResponse;
    @Getter
    private long lastProcessingTimeMs;

    // Lite 모델 사용 (단순 추출 작업)
    public static final String MODEL_NAME = "gemini-2.0-flash-lite";
    public static final double TEMPERATURE = 0.2;
    public static final int MAX_TOKENS = 1500;
    public static final String PROMPT_TEMPLATE_NAME = "DIFFICULT_EXPRESSION_PROMPT_V1";

    private static final String EXTRACTION_PROMPT_TEMPLATE = """
            다음 한국어 학교 가정통신문에서 외국인이 이해하기 어려울 수 있는 한국어 표현들을 추출하고,
            각 표현에 대해 %s로 쉽게 설명해주세요.

            ## 추출 대상
            - 한자어 기반 어려운 단어 (예: 여가선용, 건전하지 못한)
            - 학교/행정 관련 전문 용어
            - 관용적 표현이나 축약어
            - 문화적 맥락이 필요한 표현

            ## 중요
            - original: 원본 한국어 표현 그대로
            - explanation: %s로 쉽게 풀어서 설명 (간단명료하게, 순수 텍스트만)
            - 최대 10개까지만 추출
            - 정말 어려운 표현만 선별

            ## 출력 형식 규칙 (필수)
            - 마크다운 문법 사용 금지 (###, **, *, -, |, 표 등 사용하지 않기)
            - explanation 필드는 순수 텍스트로만 작성

            ## 분석할 텍스트
            %s

            ## 응답 형식 (JSON 배열만 출력, 다른 텍스트 없이)
            [{"original":"여가선용","explanation":"Using free time wisely"},{"original":"건전하지 못한","explanation":"Unhealthy or inappropriate"}]
            """;

    @Override
    public List<DifficultExpressionDto> process(String text, PipelineContext context) {
        long startTime = System.currentTimeMillis();
        try {
            String targetLanguage = context.getTargetLanguage() != null
                    ? context.getTargetLanguage().getDisplayName()
                    : "한국어";

            log.info("어려운 표현 추출 시작 (Gemini Lite 모델, 설명 언어: {})", targetLanguage);

            String promptContent = String.format(EXTRACTION_PROMPT_TEMPLATE,
                    targetLanguage,
                    targetLanguage,
                    PromptUtils.truncateText(text, 4000));

            // Gemini Lite 모델 사용 (preferPrimary = false)
            String response = geminiService.generateSimpleContent(promptContent, TEMPERATURE, MAX_TOKENS);

            this.lastRawResponse = response;
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;

            List<DifficultExpressionDto> expressions = parseResponse(response);
            context.addLog("어려운 표현 추출 완료: " + expressions.size() + "개");

            return expressions;

        } catch (Exception e) {
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;
            log.error("어려운 표현 추출 실패", e);
            context.addLog("어려운 표현 추출 실패: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public String getModuleName() {
        return "DifficultExpressionExtractor";
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
