package com.example.konnect_backend.domain.ai.service;

import com.example.konnect_backend.domain.ai.config.GeminiConfig;
import com.example.konnect_backend.domain.ai.exception.DocumentAnalysisException;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gemini API 통합 서비스
 *
 * ## 모델별 용도
 * - gemini-2.0-flash (Primary): 복잡한 분석, 문서 분류, 정보 추출, Vision
 * - gemini-2.0-flash-lite (Lite): 단순 번역, 요약, 간단한 텍스트 처리
 *
 * ## 주요 기능
 * 1. 텍스트 생성 (generateContent)
 * 2. 이미지 분석 (generateContentWithImage) - Vision
 * 3. 자동 모델 선택 및 폴백
 * 4. 호출 횟수 추적
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final GeminiConfig config;
    private final GeminiRateLimitService rateLimitService;
    private final RestTemplate geminiRestTemplate;
    private final ObjectMapper objectMapper;

    // 세션별 토큰 사용량 추적
    private final AtomicLong sessionInputTokens = new AtomicLong(0);
    private final AtomicLong sessionOutputTokens = new AtomicLong(0);
    private final AtomicLong sessionTotalTokens = new AtomicLong(0);

    // 마지막 호출 토큰 정보
    private volatile TokenUsage lastTokenUsage;

    /**
     * 텍스트 생성 (Primary 모델 선호)
     * 복잡한 작업: 문서 분류, 정보 추출
     */
    public String generateContent(String prompt, double temperature, int maxTokens) {
        return generateContent(prompt, temperature, maxTokens, true);
    }

    /**
     * 텍스트 생성 (모델 선호도 지정)
     *
     * @param prompt 프롬프트
     * @param temperature 온도 (0.0 ~ 1.0)
     * @param maxTokens 최대 토큰 수
     * @param preferPrimary true: Primary 모델 선호, false: Lite 모델 선호
     */
    public String generateContent(String prompt, double temperature, int maxTokens, boolean preferPrimary) {
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
    public String generateSimpleContent(String prompt, double temperature, int maxTokens) {
        return generateContent(prompt, temperature, maxTokens, false);
    }

    /**
     * 이미지 분석 (Vision 모델)
     * OCR, 이미지 내 텍스트 추출
     *
     * @param prompt 프롬프트
     * @param imageBase64 Base64 인코딩된 이미지
     * @param mimeType 이미지 MIME 타입 (image/jpeg, image/png 등)
     */
    public String generateContentWithImage(String prompt, String imageBase64, String mimeType,
                                           double temperature, int maxTokens) {
        String model = rateLimitService.getVisionModel();

        if (model == null) {
            log.error("Vision 모델 사용 불가 (일일 제한 도달)");
            throw new DocumentAnalysisException(ErrorStatus.AI_SERVICE_UNAVAILABLE);
        }

        return callGeminiApi(model, prompt, new ImageData(imageBase64, mimeType), temperature, maxTokens);
    }

    /**
     * Gemini API 호출
     */
    private String callGeminiApi(String model, String prompt, ImageData imageData,
                                  double temperature, int maxTokens) {
        String url = String.format("%s/models/%s:generateContent?key=%s",
                config.getApi().getBaseUrl(),
                model,
                config.getApi().getKey());

        try {
            // 요청 본문 구성
            Map<String, Object> requestBody = buildRequestBody(prompt, imageData, temperature, maxTokens);

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
                log.error("Gemini API 오류: status={}, body={}", response.getStatusCode(), response.getBody());
                throw new DocumentAnalysisException(ErrorStatus.AI_SERVICE_UNAVAILABLE);
            }

            // 응답 파싱 및 토큰 로깅
            String content = parseResponse(response.getBody(), model);

            // 호출 기록
            rateLimitService.recordUsage(model);

            log.debug("Gemini API 응답 완료: model={}, responseLength={}", model, content.length());

            return content;

        } catch (DocumentAnalysisException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini API 호출 실패: model={}, error={}", model, e.getMessage(), e);
            throw new DocumentAnalysisException(ErrorStatus.AI_SERVICE_UNAVAILABLE);
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

    /**
     * 응답 파싱 및 토큰 사용량 추출
     */
    private String parseResponse(String responseBody, String model) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // 토큰 사용량 추출 및 로깅
            extractAndLogTokenUsage(root, model);

            // candidates[0].content.parts[0].text
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode contentNode = firstCandidate.path("content");
                JsonNode partsNode = contentNode.path("parts");

                if (partsNode.isArray() && partsNode.size() > 0) {
                    JsonNode textNode = partsNode.get(0).path("text");
                    if (!textNode.isMissingNode()) {
                        return textNode.asText();
                    }
                }
            }

            // 응답 구조가 예상과 다른 경우
            log.warn("예상치 못한 Gemini 응답 구조: {}", responseBody);
            throw new DocumentAnalysisException(ErrorStatus.AI_SERVICE_UNAVAILABLE);

        } catch (DocumentAnalysisException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini 응답 파싱 실패: {}", e.getMessage(), e);
            throw new DocumentAnalysisException(ErrorStatus.AI_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * 토큰 사용량 추출 및 로깅
     */
    private void extractAndLogTokenUsage(JsonNode root, String model) {
        try {
            JsonNode usageMetadata = root.path("usageMetadata");
            if (!usageMetadata.isMissingNode()) {
                int promptTokens = usageMetadata.path("promptTokenCount").asInt(0);
                int candidatesTokens = usageMetadata.path("candidatesTokenCount").asInt(0);
                int totalTokens = usageMetadata.path("totalTokenCount").asInt(0);

                // 세션 누적
                sessionInputTokens.addAndGet(promptTokens);
                sessionOutputTokens.addAndGet(candidatesTokens);
                sessionTotalTokens.addAndGet(totalTokens);

                // 마지막 호출 정보 저장
                lastTokenUsage = new TokenUsage(model, promptTokens, candidatesTokens, totalTokens);

                log.info("📊 토큰 사용량 [{}] - 입력: {}, 출력: {}, 합계: {} | 세션 누적: {}",
                        model,
                        promptTokens,
                        candidatesTokens,
                        totalTokens,
                        sessionTotalTokens.get());
            }
        } catch (Exception e) {
            log.debug("토큰 사용량 추출 실패 (무시): {}", e.getMessage());
        }
    }

    /**
     * 현재 API 사용량 조회
     */
    public GeminiRateLimitService.UsageStatus getUsageStatus() {
        return rateLimitService.getUsageStatus();
    }

    /**
     * 세션 토큰 사용량 조회
     */
    public SessionTokenUsage getSessionTokenUsage() {
        return new SessionTokenUsage(
                sessionInputTokens.get(),
                sessionOutputTokens.get(),
                sessionTotalTokens.get()
        );
    }

    /**
     * 마지막 API 호출 토큰 정보 조회
     */
    public TokenUsage getLastTokenUsage() {
        return lastTokenUsage;
    }

    /**
     * 세션 토큰 사용량 초기화
     */
    public void resetSessionTokenUsage() {
        sessionInputTokens.set(0);
        sessionOutputTokens.set(0);
        sessionTotalTokens.set(0);
        lastTokenUsage = null;
        log.info("세션 토큰 사용량 초기화됨");
    }

    /**
     * 이미지 데이터 레코드
     */
    private record ImageData(String base64Data, String mimeType) {}

    /**
     * 단일 호출 토큰 사용량
     */
    public record TokenUsage(
            String model,
            int inputTokens,
            int outputTokens,
            int totalTokens
    ) {}

    /**
     * 세션 누적 토큰 사용량
     */
    public record SessionTokenUsage(
            long inputTokens,
            long outputTokens,
            long totalTokens
    ) {}
}
