package com.example.konnect_backend.domain.ai.service.module;

import com.example.konnect_backend.domain.ai.domain.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.domain.vo.PipelineContext;
import com.example.konnect_backend.domain.ai.domain.vo.TokenUsage;
import com.example.konnect_backend.domain.ai.dto.internal.ExtractionResult;
import com.example.konnect_backend.domain.ai.dto.internal.GeminiCallResult;
import com.example.konnect_backend.domain.ai.dto.response.ExtractedScheduleDto;
import com.example.konnect_backend.domain.ai.infra.GeminiService;
import com.example.konnect_backend.domain.ai.service.prompt.PromptTemplateResolver;
import com.example.konnect_backend.domain.ai.util.PromptUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 통합 정보 추출 모듈 (Gemini API 사용)
 * <p>
 * ## 모델 선택: gemini-2.0-flash (Primary)
 * - 이유: 복잡한 JSON 구조 추출에 높은 정확도 필요
 * - 다양한 정보(일정, 행사, 벌점, 공지) 통합 추출
 * - 날짜 파싱 및 구조화된 데이터 생성
 * - RPD: 200회/일 (일일 호출량 관리 필요)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnifiedExtractorModule implements PromptModule {

    private final GeminiService geminiService;
    private final PromptTemplateResolver resolver;

    // Primary 모델 사용 (복잡한 JSON 추출, 정확도 중요)
    public static final String MODEL_NAME = "gemini-2.0-flash";
    public static final double TEMPERATURE = 0.2;
    public static final int MAX_TOKENS = 3000;

    @Override
    public TokenUsage process(PromptTemplate promptTemplate, PipelineContext context) {
        Map<String, String> vars = getVars(context);
        String prompt = resolver.resolve(promptTemplate, vars);

        try {
            log.info("통합 정보 추출 시작 (Gemini Primary 모델, 출력 언어: {})",
                context.getTargetLanguage().getDisplayName());
            long startTime = System.currentTimeMillis();

            // Gemini Primary 모델 사용 (preferPrimary = true)
            GeminiCallResult callResult = geminiService.generateContent(prompt, TEMPERATURE,
                MAX_TOKENS, true);
            String response = callResult.response();

            ExtractionResult result = parseUnifiedResult(response);
            int scheduleCount = result.getSchedules() != null ? result.getSchedules().size() : 0;

            context.addLog(String.format("정보 추출 완료: %d개 일정, 추가정보 %d개 항목",
                scheduleCount, result.getAdditionalInfo().size()));
            context.setExtractionResult(result);
            context.setCompletedStage(PipelineContext.PipelineStage.EXTRACTED);

            log.info("통합 정보 추출 소요시간 {} ms", System.currentTimeMillis() - startTime);

            return callResult.tokenUsage();
        } catch (Exception e) {
            log.error("통합 정보 추출 실패", e);
            context.addLog("정보 추출 실패: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public String getModuleName() {
        return "UNIFIED_EXTRACTION";
    }

    public Map<String, String> getVars(PipelineContext context) {
        Map<String, String> vars = new HashMap<>();
        vars.put("target_language", context.getTargetLanguage().getDisplayName());
        vars.put("today", LocalDate.now().toString());
        vars.put("text", PromptUtils.truncateText(context.getExtractedText(), 5000));

        return vars;
    }

    @SuppressWarnings("unchecked")
    private ExtractionResult parseUnifiedResult(String response) {
        try {
            String jsonStr = PromptUtils.extractJsonObject(response);

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            Map<String, Object> resultMap = mapper.readValue(jsonStr, Map.class);

            // 일정 추출
            List<ExtractedScheduleDto> schedules = new ArrayList<>();
            if (resultMap.containsKey("schedules") && resultMap.get("schedules") != null) {
                String schedulesJson = mapper.writeValueAsString(resultMap.get("schedules"));
                schedules = mapper.readValue(schedulesJson,
                    new TypeReference<List<ExtractedScheduleDto>>() {
                    });
            }

            // 추가 정보 수집
            Map<String, Object> additionalInfo = new HashMap<>();

            if (resultMap.containsKey("eventDetails") && resultMap.get("eventDetails") != null) {
                additionalInfo.put("eventDetails", resultMap.get("eventDetails"));
            }

            if (resultMap.containsKey("penaltyInfo") && resultMap.get("penaltyInfo") != null) {
                additionalInfo.put("penaltyInfo", resultMap.get("penaltyInfo"));
            }

            if (resultMap.containsKey("noticeDetails") && resultMap.get("noticeDetails") != null) {
                additionalInfo.put("noticeDetails", resultMap.get("noticeDetails"));
            }

            log.debug("추출 결과: {}개 일정, {}개 추가정보", schedules.size(), additionalInfo.size());

            return ExtractionResult.builder()
                .schedules(schedules)
                .additionalInfo(additionalInfo)
                .build();

        } catch (Exception e) {
            log.warn("통합 추출 결과 JSON 파싱 실패: {}", PromptUtils.truncateText(response, 200), e);
            return ExtractionResult.empty();
        }
    }
}
