package com.example.konnect_backend.domain.ai.infra;

import com.example.konnect_backend.domain.ai.config.GeminiConfig;
import com.example.konnect_backend.domain.ai.domain.vo.TokenUsage;
import com.example.konnect_backend.domain.ai.dto.internal.GeminiCallResult;
import com.example.konnect_backend.domain.ai.exception.DocumentAnalysisException;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemini API 통합 서비스
 * <p>
 * ## 모델별 용도
 * - gemini-2.0-flash (Primary): 복잡한 분석, 문서 분류, 정보 추출, Vision
 * - gemini-2.0-flash-lite (Lite): 단순 번역, 요약, 간단한 텍스트 처리
 * <p>
 * ## 주요 기능
 * 1. 텍스트 생성 (generateContent)
 * 2. 이미지 분석 (generateContentWithImage) - Vision
 * 3. 자동 모델 선택 및 폴백
 * 4. 호출 횟수 추적
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final GeminiConfig config;
    // Todo 카페인 캐시는 쓰는 순간부터 24시간이 지나가므로 매일 정확히 0시에 value를 0으로 쓰지 않는 이상 정확하지 않음
    private final GeminiRateLimitService rateLimitService;
    private final RestTemplate geminiRestTemplate;
    private final ObjectMapper objectMapper;

    private final DiscordWebhookService discordService;
    private final LlmHealthTracker tracker = new LlmHealthTracker(5, 4, 3);

    /**
     * 텍스트 생성 (모델 선호도 지정)
     *
     * @param prompt        프롬프트
     * @param temperature   온도 (0.0 ~ 1.0)
     * @param maxTokens     최대 토큰 수
     * @param preferPrimary true: Primary 모델 선호, false: Lite 모델 선호
     */
    public GeminiCallResult generateContent(String prompt, double temperature, int maxTokens,
                                            boolean preferPrimary) {
        String model = rateLimitService.getAvailableModel(preferPrimary);

        if (model == null) {
            log.error("사용 가능한 Gemini 모델이 없습니다 (일일 제한 도달)");
            throw new DocumentAnalysisException(ErrorStatus.AI_SERVICE_UNAVAILABLE);
        }

        return callGeminiApi(model, prompt, null, temperature, maxTokens);
    }

    /**
     * 간단한 텍스트 생성 (Lite 모델 사용)
     * 단순 작업: 번역, 요약, 쉬운 표현 변환
     */
    public GeminiCallResult generateSimpleContent(String prompt, double temperature,
                                                  int maxTokens) {
        return generateContent(prompt, temperature, maxTokens, false);
    }

    /**
     * 이미지 분석 (Vision 모델)
     * OCR, 이미지 내 텍스트 추출
     *
     * @param prompt      프롬프트
     * @param imageBase64 Base64 인코딩된 이미지
     * @param mimeType    이미지 MIME 타입 (image/jpeg, image/png 등)
     */
    public GeminiCallResult generateContentWithImage(String prompt, String imageBase64,
                                                     String mimeType,
                                                     double temperature, int maxTokens) {
        String model = rateLimitService.getVisionModel();

        if (model == null) {
            log.error("Vision 모델 사용 불가 (일일 제한 도달)");
            throw new DocumentAnalysisException(ErrorStatus.AI_SERVICE_UNAVAILABLE);
        }

        return callGeminiApi(model, prompt, new ImageData(imageBase64, mimeType), temperature,
            maxTokens);
    }

    public GeminiCallResult call(String model, String prompt, double temperature, int maxTokens) {
        return callGeminiApi(model, prompt, null, temperature, maxTokens);
    }

    /**
     * Gemini API 호출
     */
    private GeminiCallResult callGeminiApi(String model, String prompt, ImageData imageData,
                                           double temperature, int maxTokens) {
        boolean success = false;

        String url = String.format("%s/models/%s:generateContent?key=%s",
            config.getApi().getBaseUrl(),
            model,
            config.getApi().getKey());

        try {
            // 요청 본문 구성
            Map<String, Object> requestBody = buildRequestBody(prompt, imageData, temperature,
                maxTokens);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Gemini API 호출: model={}, promptLength={}", model, prompt.length());

            ResponseEntity<String> response = geminiRestTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Gemini API 오류: status={}, body={}", response.getStatusCode(),
                    response.getBody());
                throw new DocumentAnalysisException(ErrorStatus.AI_SERVICE_UNAVAILABLE);
            }

            // 응답 파싱
            GeminiResponse geminiResponse = parseResponse(response.getBody());
            String modelRawResponse = geminiResponse.getCandidates().get(0).getContent().getParts()
                .get(0)
                .getText();
            long inputTokens = geminiResponse.getUsageMetadata().getPromptTokenCount();
            long outputTokens = geminiResponse.getUsageMetadata().getCandidatesTokenCount();
            String usedModel = geminiResponse.getModelVersion();
            String finishReason = geminiResponse.getCandidates().get(0).getFinishReason();
            GeminiCallResult result = new GeminiCallResult(modelRawResponse,
                new TokenUsage((int) inputTokens, (int) outputTokens), maxTokens, usedModel,
                finishReason);

            // 호출 기록
            rateLimitService.recordUsage(usedModel);

            log.debug("Gemini API 응답 완료: model={}, responseLength={}", model,
                result.response().length());
            success = true;

            return result;
        } catch (Exception e) {
            log.error("Gemini API 호출 실패: model={}, error={}", model, e.getMessage(), e);
            throw new DocumentAnalysisException(ErrorStatus.AI_SERVICE_UNAVAILABLE);
        } finally {
            LlmHealthTracker.StateChange change = tracker.recordAndCheck(success);

            // 이벤트로 분리하는 것도 가능
            if (change == LlmHealthTracker.StateChange.DOWN) {
                log.error("LLM 장애 발생 확인 - 시각: {}", OffsetDateTime.now());
                discordService.notifyStateChange(true);
            } else if (change == LlmHealthTracker.StateChange.UP) {
                log.info("LLM 장애 복구 확인 - 시각: {}", OffsetDateTime.now());
                discordService.notifyStateChange(false);
            }
        }
    }

    /**
     * 요청 본문 구성
     */
    private Map<String, Object> buildRequestBody(String prompt, ImageData imageData,
                                                 double temperature, int maxTokens) {
        Map<String, Object> requestBody = new HashMap<>();

        // contents 구성
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();

        // 텍스트 파트
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);
        parts.add(textPart);

        // 이미지 파트 (Vision인 경우)
        if (imageData != null) {
            Map<String, Object> imagePart = new HashMap<>();
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mimeType", imageData.mimeType());
            inlineData.put("data", imageData.base64Data());
            imagePart.put("inlineData", inlineData);
            parts.add(imagePart);
        }

        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);

        // generationConfig 구성
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", maxTokens);
        generationConfig.put("topP", 0.95);
        generationConfig.put("topK", 40);
        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    private GeminiResponse parseResponse(String responseBody) {
        try {
            log.debug(responseBody);
            return objectMapper.readValue(responseBody, GeminiResponse.class);
        } catch (DocumentAnalysisException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini 응답 파싱 실패: {}", e.getMessage(), e);
            throw new DocumentAnalysisException(ErrorStatus.AI_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * 이미지 데이터 레코드
     */
    private record ImageData(String base64Data, String mimeType) {
    }
}
