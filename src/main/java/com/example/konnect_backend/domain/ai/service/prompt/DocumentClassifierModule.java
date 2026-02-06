package com.example.konnect_backend.domain.ai.service.prompt;

import com.example.konnect_backend.domain.ai.dto.response.ClassificationResult;
import com.example.konnect_backend.domain.ai.service.GeminiService;
import com.example.konnect_backend.domain.ai.service.pipeline.PipelineContext;
import com.example.konnect_backend.domain.ai.type.DocumentType;
import com.example.konnect_backend.domain.ai.util.PromptUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 문서 분류 모듈 (Gemini API 사용)
 *
 * ## 모델 선택: gemini-2.0-flash-lite
 * - 이유: 문서 분류는 상대적으로 단순한 작업
 * - RPD: 1,000회/일로 여유로움
 * - 빠른 응답 속도
 * - 비용 효율적
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentClassifierModule implements PromptModule<String, ClassificationResult> {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @Getter
    private String lastRawResponse;
    @Getter
    private long lastProcessingTimeMs;

    // Lite 모델 사용 (단순 분류 작업)
    public static final String MODEL_NAME = "gemini-2.0-flash-lite";
    public static final double TEMPERATURE = 0.1;
    public static final int MAX_TOKENS = 500;
    public static final String PROMPT_TEMPLATE_NAME = "CLASSIFICATION_PROMPT_V2";

    private static final String CLASSIFICATION_PROMPT_TEMPLATE = """
            다음 학교 가정통신문 텍스트를 분석하여 문서 유형을 분류해주세요.

            ## 문서 유형 (반드시 아래 4가지 중 하나만 선택)
            1. SCHEDULE - 일정 안내: 시험일정, 방학일정, 등교일, 휴업일, 행사일 등 특정 날짜/기간이 명시된 일정 관련 문서
            2. PENALTY - 벌점/패널티: 교칙 위반, 벌점, 징계, 상벌점, 학교폭력, 규정 위반 관련 문서
            3. EVENT - 행사 진행: 학교 행사, 체험학습, 소풍, 운동회, 발표회, 대회 등 행사 참여/진행 관련 문서
            4. NOTICE - 일반 공지: 준비물 안내, 건강검진, 급식, 안전교육 등 위 3가지에 해당하지 않는 일반 공지

            ## 분류 기준
            - 날짜와 시간이 핵심 정보이면 → SCHEDULE
            - 위반/처벌/규정이 핵심이면 → PENALTY
            - 행사 참여/동의서가 핵심이면 → EVENT
            - 위 조건에 해당하지 않으면 → NOTICE

            ## 분석할 텍스트
            %s

            ## 출력 형식 규칙 (필수)
            - 마크다운 문법 사용 금지 (###, **, *, -, |, 표 등 사용하지 않기)
            - 모든 텍스트 필드는 순수 텍스트로만 작성

            ## 응답 형식 (반드시 아래 JSON 형식으로만 출력, 다른 텍스트 없이)
            분류 과정을 단계별로 설명해주세요:
            {
              "documentType": "SCHEDULE",
              "confidence": 0.95,
              "keywords": ["여름방학", "일정", "등교"],
              "reasoning": "1) 핵심 키워드 분석: '방학기간', '개학일', '등교' 발견 2) 날짜 정보: 2025.7.23~8.17 기간 명시 3) 결론: 구체적인 일정 정보가 문서의 핵심이므로 SCHEDULE로 분류"
            }
            """;

    @Override
    public ClassificationResult process(String text, PipelineContext context) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("문서 유형 분류 시작 (Gemini Lite 모델 사용)");

            String truncatedText = PromptUtils.truncateText(text, 3000);
            String promptContent = String.format(CLASSIFICATION_PROMPT_TEMPLATE, truncatedText);

            // Gemini Lite 모델 사용 (preferPrimary = false)
            String response = geminiService.generateSimpleContent(promptContent, TEMPERATURE, MAX_TOKENS);

            this.lastRawResponse = response;
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;

            ClassificationResult result = parseClassificationResult(response);
            context.setDocumentType(result.getDocumentType());
            context.addLog("문서 분류 완료: " + result.getDocumentType().getDisplayName() +
                    " (신뢰도: " + result.getConfidence() + ")");

            log.info("문서 분류 결과: type={}, confidence={}, keywords={}, reasoning={}",
                    result.getDocumentType(),
                    result.getConfidence(),
                    result.getKeywords(),
                    PromptUtils.truncateText(result.getReasoning(), 100));

            return result;

        } catch (Exception e) {
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;
            log.error("문서 분류 실패", e);
            ClassificationResult defaultResult = ClassificationResult.defaultNotice();
            context.setDocumentType(defaultResult.getDocumentType());
            context.addLog("문서 분류 실패, 기본값(NOTICE) 사용");
            return defaultResult;
        }
    }

    @Override
    public String getModuleName() {
        return "DocumentClassifier";
    }

    @SuppressWarnings("unchecked")
    private ClassificationResult parseClassificationResult(String response) {
        try {
            String jsonStr = PromptUtils.extractJsonObject(response);
            Map<String, Object> map = objectMapper.readValue(jsonStr, Map.class);

            DocumentType documentType;
            try {
                documentType = DocumentType.valueOf((String) map.get("documentType"));
            } catch (Exception e) {
                log.warn("알 수 없는 문서 유형: {}", map.get("documentType"));
                documentType = DocumentType.NOTICE;
            }

            Double confidence = map.get("confidence") instanceof Number
                    ? ((Number) map.get("confidence")).doubleValue()
                    : 0.5;

            List<String> keywords = map.get("keywords") instanceof List
                    ? (List<String>) map.get("keywords")
                    : List.of();

            String reasoning = (String) map.getOrDefault("reasoning", "");

            return ClassificationResult.builder()
                    .documentType(documentType)
                    .confidence(confidence)
                    .keywords(keywords)
                    .reasoning(reasoning)
                    .build();

        } catch (Exception e) {
            log.warn("분류 결과 JSON 파싱 실패: {}", PromptUtils.truncateText(response, 200), e);
            return ClassificationResult.defaultNotice();
        }
    }
}
