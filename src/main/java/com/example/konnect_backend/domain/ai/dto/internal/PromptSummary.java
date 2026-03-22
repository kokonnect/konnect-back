package com.example.konnect_backend.domain.ai.dto.internal;

import com.example.konnect_backend.domain.ai.type.PromptStatus;

import java.time.LocalDateTime;

public record PromptSummary(Long id, String name, Integer version, PromptStatus status,
                            String model, LocalDateTime createdAt) {
}
