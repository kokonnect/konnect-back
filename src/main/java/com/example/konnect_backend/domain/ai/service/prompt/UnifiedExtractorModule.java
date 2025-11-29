package com.example.konnect_backend.domain.ai.service.prompt;

import com.example.konnect_backend.domain.ai.dto.internal.ExtractionResult;
import com.example.konnect_backend.domain.ai.dto.response.ExtractedScheduleDto;
import com.example.konnect_backend.domain.ai.service.GeminiService;
import com.example.konnect_backend.domain.ai.service.pipeline.PipelineContext;
import com.example.konnect_backend.domain.ai.util.PromptUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
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
 *
 * ## 모델 선택: gemini-2.0-flash (Primary)
 * - 이유: 복잡한 JSON 구조 추출에 높은 정확도 필요
 * - 다양한 정보(일정, 행사, 벌점, 공지) 통합 추출
 * - 날짜 파싱 및 구조화된 데이터 생성
 * - RPD: 200회/일 (일일 호출량 관리 필요)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnifiedExtractorModule implements PromptModule<String, ExtractionResult> {

    private final GeminiService geminiService;

    @Getter
    private String lastRawResponse;
    @Getter
    private long lastProcessingTimeMs;

    // Primary 모델 사용 (복잡한 JSON 추출, 정확도 중요)
    public static final String MODEL_NAME = "gemini-2.0-flash";
    public static final double TEMPERATURE = 0.2;
    public static final int MAX_TOKENS = 3000;

    private static final String UNIFIED_EXTRACTION_PROMPT = """
            다음 학교 가정통신문에서 모든 관련 정보를 추출해주세요.
            문서에 해당 정보가 없으면 빈 배열 또는 null로 남겨두세요.

            ## 추출할 정보

            ### 1. 일정 정보 (schedules)
            날짜가 명시된 모든 일정/행사/이벤트/마감일을 추출합니다.
            - title: 일정 제목 (20자 이내)
            - memo: 관련 준비물, 장소, 주의사항
            - startDate: 시작일시 (ISO 8601 형식: yyyy-MM-ddTHH:mm:ss)
            - endDate: 종료일시
            - isAllDay: 시간 정보가 없으면 true

            ### 2. 행사 세부정보 (eventDetails) - 해당되는 경우만
            - eventName: 행사명
            - targetGrade: 참가 대상
            - location: 장소
            - cost: 비용 (숫자)
            - requirements: 준비물 배열
            - consentRequired: 동의서 필요 여부
            - consentDeadline: 동의서 제출 마감일

            ### 3. 벌점/규정 정보 (penaltyInfo) - 해당되는 경우만
            - violations: 위반 항목 배열 [{item, points, description}]
            - cumulativePenalties: 누적 처분 배열 [{points, action}]
            - appealDeadline: 이의 제기 기한
            - warnings: 주의사항 배열

            ### 4. 공지 세부정보 (noticeDetails) - 해당되는 경우만
            - title: 공지 제목
            - requirements: 필요 서류/준비물 배열
            - deadline: 마감일
            - contact: 연락처
            - warnings: 주의사항 배열

            ## 중요: 출력 언어
            - 모든 텍스트 필드는 반드시 %s로 작성해주세요
            - 원본이 한국어여도 %s로 번역하여 출력

            ## 오늘 날짜 (연도 추론에 활용)
            %s

            ## 분석할 텍스트
            %s

            ## 응답 형식 (JSON만 출력, 다른 텍스트 없이)
            {
              "schedules": [
                {"title":"Final Exam","memo":"Required: ID card","startDate":"2024-12-15T09:00:00","endDate":"2024-12-17T12:00:00","isAllDay":false}
              ],
              "eventDetails": null,
              "penaltyInfo": null,
              "noticeDetails": {
                "title":"Health Checkup",
                "requirements":["Health form"],
                "deadline":"2024-11-01",
                "contact":"School nurse",
                "warnings":[]
              }
            }
            """;

    @Override
    public ExtractionResult process(String text, PipelineContext context) {
        long startTime = System.currentTimeMillis();
        try {
            String targetLanguage = context.getTargetLanguage() != null
                    ? context.getTargetLanguage().getDisplayName()
                    : "한국어";

            log.info("통합 정보 추출 시작 (Gemini Primary 모델, 출력 언어: {})", targetLanguage);

            String promptContent = String.format(UNIFIED_EXTRACTION_PROMPT,
                    targetLanguage,
                    targetLanguage,
                    LocalDate.now().toString(),
                    PromptUtils.truncateText(text, 5000));

            // Gemini Primary 모델 사용 (preferPrimary = true)
            String response = geminiService.generateContent(promptContent, TEMPERATURE, MAX_TOKENS, true);

            this.lastRawResponse = response;
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;

            ExtractionResult result = parseUnifiedResult(response);

            int scheduleCount = result.getSchedules() != null ? result.getSchedules().size() : 0;
            context.addLog(String.format("정보 추출 완료: %d개 일정, 추가정보 %d개 항목",
                    scheduleCount, result.getAdditionalInfo().size()));

            return result;

        } catch (Exception e) {
            this.lastProcessingTimeMs = System.currentTimeMillis() - startTime;
            log.error("통합 정보 추출 실패", e);
            context.addLog("정보 추출 실패: " + e.getMessage());
            return ExtractionResult.empty();
        }
    }

    @Override
    public String getModuleName() {
        return "UnifiedExtractor";
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
                schedules = mapper.readValue(schedulesJson, new TypeReference<List<ExtractedScheduleDto>>() {});
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
