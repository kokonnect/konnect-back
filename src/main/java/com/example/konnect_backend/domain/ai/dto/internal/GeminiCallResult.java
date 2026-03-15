package com.example.konnect_backend.domain.ai.dto.internal;

import com.example.konnect_backend.domain.ai.model.vo.TokenUsage;

public record GeminiCallResult(String response, TokenUsage tokenUsage, long maxTokens, String model, String finishReason) {
}
