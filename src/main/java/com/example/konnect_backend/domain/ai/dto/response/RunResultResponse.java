package com.example.konnect_backend.domain.ai.dto.response;

public record RunResultResponse(String response, Integer latencyMs, Integer inputTokens,
                                Integer outputTokens) {
}
