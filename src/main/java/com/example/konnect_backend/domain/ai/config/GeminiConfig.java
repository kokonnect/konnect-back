package com.example.konnect_backend.domain.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Gemini API 설정
 *
 * ## 모델별 특성 및 선택 기준
 *
 * ### gemini-2.0-flash (Primary)
 * - RPD: 200회/일
 * - 용도: 복잡한 분석, 문서 분류, 정보 추출
 * - 특징: 높은 정확도, 멀티모달(이미지+텍스트) 지원
 * - 비용: 중간
 *
 * ### gemini-2.0-flash-lite (Lite)
 * - RPD: 1,000회/일
 * - 용도: 단순 번역, 요약, 간단한 텍스트 처리
 * - 특징: 빠른 응답, 저비용
 * - 비용: 낮음
 *
 * ## 모델 선택 전략
 * 1. 이미지/PDF OCR → gemini-2.0-flash (Vision 기능 필요)
 * 2. 문서 분류 → gemini-2.0-flash (정확도 중요)
 * 3. 정보 추출 → gemini-2.0-flash (복잡한 JSON 파싱)
 * 4. 쉬운 한국어 변환 → gemini-2.0-flash-lite (단순 변환)
 * 5. 번역 → gemini-2.0-flash-lite (단순 변환)
 * 6. 요약 → gemini-2.0-flash-lite (단순 텍스트 처리)
 * 7. 어려운 표현 추출 → gemini-2.0-flash-lite (단순 추출)
 */
@Configuration
@ConfigurationProperties(prefix = "gemini")
@Getter
@Setter
public class GeminiConfig {

    private Api api = new Api();
    private Model model = new Model();
    private Limit limit = new Limit();

    @Getter
    @Setter
    public static class Api {
        private String key;
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    }

    @Getter
    @Setter
    public static class Model {
        // 복잡한 작업용 (문서 분류, 정보 추출, Vision)
        private String primary = "gemini-2.0-flash";
        // 간단한 작업용 (번역, 요약)
        private String lite = "gemini-2.0-flash-lite";
        // Vision (이미지 분석)
        private String vision = "gemini-2.0-flash";
    }

    @Getter
    @Setter
    public static class Limit {
        private Primary primary = new Primary();
        private Lite lite = new Lite();

        @Getter
        @Setter
        public static class Primary {
            private int rpd = 200;
        }

        @Getter
        @Setter
        public static class Lite {
            private int rpd = 1000;
        }
    }

    @Bean
    public RestTemplate geminiRestTemplate() {
        return new RestTemplate();
    }
}
