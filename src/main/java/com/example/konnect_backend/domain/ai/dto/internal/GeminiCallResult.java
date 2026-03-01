package com.example.konnect_backend.domain.ai.dto.internal;

public record GeminiCallResult(String response, long inputTokens, long outputTokens, String model) {
}
