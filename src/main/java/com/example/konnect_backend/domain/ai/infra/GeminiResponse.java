package com.example.konnect_backend.domain.ai.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {
    // 1. 모델이 생성한 응답 후보들 (기본 1개)
    private List<Candidate> candidates;

    // 2. 토큰 사용량 정보
    private UsageMetadata usageMetadata;

    // 3. 사용된 모델 버전 (예: gemini-1.5-pro)
    private String modelVersion;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        private Content content;
        private String finishReason; // STOP, MAX_TOKENS, SAFETY 등
        private int index;
        private List<SafetyRating> safetyRatings;
        private CitationMetadata citationMetadata; // 인용 정보가 있을 경우
    }

    @Data
    public static class Content {
        private List<Part> parts;
        private String role; // 보통 "model"
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Part {
        private String text;       // 텍스트 응답일 경우
        private InlineData inlineData; // 이미지/파일 등 멀티모달 응답일 경우 (선택)
        private FunctionCall functionCall; // 함수 호출 시 (선택)
    }

    @Data
    public static class UsageMetadata {
        private int promptTokenCount;
        private int candidatesTokenCount;
        private int totalTokenCount;
        private int cachedContentTokenCount; // 캐시 사용 시 포함
    }

    @Data
    public static class SafetyRating {
        private String category;
        private String probability;
    }

    @Data
    public static class CitationMetadata {
        private List<CitationSource> citationSources;
    }

    @Data
    public static class CitationSource {
        private int startIndex;
        private int endIndex;
        private String uri;
        private String license;
    }

    // (참고) 이미지나 함수 호출 처리를 위한 추가 클래스
    @Data public static class InlineData { private String mimeType; private String data; }
    @Data public static class FunctionCall { private String name; private Object args; }
}