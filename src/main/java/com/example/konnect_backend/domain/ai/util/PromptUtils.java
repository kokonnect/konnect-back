package com.example.konnect_backend.domain.ai.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PromptUtils {

    private PromptUtils() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }

    /**
     * 텍스트를 지정된 최대 길이로 잘라냅니다.
     * 잘린 경우 "..." 를 추가합니다.
     */
    public static String truncateText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    /**
     * AI 응답에서 JSON 객체를 추출합니다.
     * 첫 번째 '{' 부터 마지막 '}' 까지를 추출합니다.
     */
    public static String extractJsonObject(String response) {
        if (response == null) {
            return "{}";
        }
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        log.warn("JSON 객체를 찾을 수 없음: {}", truncateText(response, 100));
        return "{}";
    }

    /**
     * AI 응답에서 JSON 배열을 추출합니다.
     * 첫 번째 '[' 부터 마지막 ']' 까지를 추출합니다.
     */
    public static String extractJsonArray(String response) {
        if (response == null) {
            return "[]";
        }
        int start = response.indexOf("[");
        int end = response.lastIndexOf("]");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        log.warn("JSON 배열을 찾을 수 없음: {}", truncateText(response, 100));
        return "[]";
    }

    /**
     * 입력 텍스트의 원본 길이를 반환합니다.
     */
    public static int getOriginalLength(String text) {
        return text == null ? 0 : text.length();
    }

    /**
     * 결과 요약 문자열을 생성합니다.
     */
    public static String createOutputSummary(String stepName, Object... details) {
        StringBuilder sb = new StringBuilder(stepName);
        if (details != null && details.length > 0) {
            sb.append(": ");
            for (int i = 0; i < details.length; i += 2) {
                if (i > 0) sb.append(", ");
                sb.append(details[i]).append("=").append(details[i + 1]);
            }
        }
        return sb.toString();
    }
}
