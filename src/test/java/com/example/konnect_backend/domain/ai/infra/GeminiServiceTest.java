package com.example.konnect_backend.domain.ai.infra;

import com.example.konnect_backend.domain.ai.config.GeminiConfig;
import com.example.konnect_backend.domain.ai.exception.DocumentAnalysisException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    GeminiConfig config;
    @Mock
    GeminiConfig.Api api;
    @Mock
    GeminiRateLimitService rateLimitService;
    @Mock
    RestTemplate geminiRestTemplate;
    @Mock
    ObjectMapper objectMapper;
    @Mock
    DiscordWebhookService discordService;
    @Mock
    LlmHealthTracker tracker;

    GeminiService geminiService;

    @BeforeEach
    void setUp() {
        geminiService = new GeminiService(config, rateLimitService, geminiRestTemplate,
            objectMapper, discordService, tracker);

        given(config.getApi()).willReturn(api);
        given(api.getBaseUrl()).willReturn("https://gemini.test");
        given(api.getKey()).willReturn("test-key");
        given(rateLimitService.getAvailableModel(anyBoolean())).willReturn("gemini-pro");
        given(tracker.recordAndCheck(anyBoolean())).willReturn(
            LlmHealthTracker.StateChange.NO_CHANGE);
    }

    @Test
    @DisplayName("Read Timeout 발생 시 tracker.recordAndCheck(false) 호출")
    void Should_RecordFailure_When_ReadTimeoutOccurs() {
        given(
            geminiRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
            .willThrow(new ResourceAccessException("Read timed out"));

        assertThatThrownBy(() -> geminiService.generateContent("prompt", 0.5, 1000, true))
            .isInstanceOf(DocumentAnalysisException.class);

        then(tracker).should().recordAndCheck(false);
    }

    @Test
    @DisplayName("TCP Connection Timeout 발생 시 tracker.recordAndCheck(false) 호출")
    void Should_RecordFailure_When_ConnectionTimeoutOccurs() {
        given(
            geminiRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
            .willThrow(new ResourceAccessException("Connection timed out"));

        assertThatThrownBy(() -> geminiService.generateContent("prompt", 0.5, 1000, true))
            .isInstanceOf(DocumentAnalysisException.class);

        then(tracker).should().recordAndCheck(false);
    }

    @Test
    @DisplayName("5xx 서버 에러 응답 시 tracker.recordAndCheck(false) 호출")
    void Should_RecordFailure_When_ServerErrorOccurs() {
        given(
            geminiRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
            .willThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> geminiService.generateContent("prompt", 0.5, 1000, true))
            .isInstanceOf(DocumentAnalysisException.class);

        then(tracker).should().recordAndCheck(false);
    }

    @Test
    @DisplayName("정상 응답 시 tracker.recordAndCheck(true) 호출")
    void Should_RecordSuccess_When_ResponseIsSuccessful() throws Exception {
        GeminiResponse.Part part = new GeminiResponse.Part();
        part.setText("response");

        GeminiResponse.Content content = new GeminiResponse.Content();
        content.setParts(List.of(part));

        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate();
        candidate.setContent(content);
        candidate.setFinishReason("STOP");

        GeminiResponse.UsageMetadata usage = new GeminiResponse.UsageMetadata();
        usage.setPromptTokenCount(10);
        usage.setCandidatesTokenCount(20);

        GeminiResponse response = new GeminiResponse();
        response.setCandidates(List.of(candidate));
        response.setUsageMetadata(usage);
        response.setModelVersion("gemini-pro");

        given(geminiRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
            .willReturn(ResponseEntity.ok("{}"));
        given(objectMapper.readValue(anyString(), eq(GeminiResponse.class)))
            .willReturn(response);

        geminiService.generateContent("prompt", 0.5, 1000, true);

        then(tracker).should().recordAndCheck(true);
    }
}