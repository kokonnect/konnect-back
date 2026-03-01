package com.example.konnect_backend.domain.ai.service.prompt.module;

import com.example.konnect_backend.domain.ai.dto.internal.ClassificationResult;
import com.example.konnect_backend.domain.ai.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.infra.GeminiService;
import com.example.konnect_backend.domain.ai.service.pipeline.PipelineContext;
import com.example.konnect_backend.domain.ai.service.prompt.PromptTemplateResolver;
import com.example.konnect_backend.domain.ai.type.DocumentType;
import com.example.konnect_backend.domain.ai.util.PromptUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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
@Deprecated
public class DocumentClassifierModule implements PromptModule {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;
    private final PromptTemplateResolver resolver;

    // Lite 모델 사용 (단순 분류 작업)
    public static final String MODEL_NAME = "gemini-2.0-flash-lite";
    public static final double TEMPERATURE = 0.1;
    public static final int MAX_TOKENS = 500;

    @Override
    public void process(PromptTemplate promptTemplate, PipelineContext context) {
        Map<String, String> vars = prepareVars(context);
        String prompt = resolver.resolve(promptTemplate, vars);

        long startTime = System.currentTimeMillis();
        try {
            log.info("문서 유형 분류 시작 (Gemini Lite 모델 사용)");

            // Gemini Lite 모델 사용 (preferPrimary = false)
            String response = geminiService.generateSimpleContent(prompt, TEMPERATURE, MAX_TOKENS).response();

            ClassificationResult result = parseClassificationResult(response);
            context.setClassificationResult(result);
            context.setDocumentType(result.getDocumentType());
            context.addLog("문서 분류 완료: " + result.getDocumentType().getDisplayName() +
                    " (신뢰도: " + result.getConfidence() + ")");
            context.setCompletedStage(PipelineContext.PipelineStage.CLASSIFIED);

            log.info("문서 분류 결과: type={}, confidence={}, keywords={}, reasoning={}",
                    result.getDocumentType(),
                    result.getConfidence(),
                    result.getKeywords(),
                    PromptUtils.truncateText(result.getReasoning(), 100));
            log.info("문서 분류 소요시간: {} ms", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("문서 분류 실패", e);

            ClassificationResult defaultResult = ClassificationResult.defaultNotice();
            context.setDocumentType(defaultResult.getDocumentType());
            context.addLog("문서 분류 실패, 기본값(NOTICE) 사용");
            context.setClassificationResult(defaultResult);

            throw e;
        }
    }

    private Map<String, String> prepareVars(PipelineContext context) {
        Map<String, String> vars = new HashMap<>();
        vars.put("text", PromptUtils.truncateText(context.getExtractedText(), 4000));

        return vars;
    }

    public String getModuleName() {
        return "CLASSIFICATION";
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
