package com.example.konnect_backend.domain.ai.dto.response;

import com.example.konnect_backend.domain.ai.dto.internal.PromptSummary;
import com.example.konnect_backend.domain.ai.type.PromptStatus;

import java.time.LocalDateTime;

public record PromptSummaryResponse(Long id, String name, Integer version, PromptStatus status,
                                    String model, LocalDateTime createdAt) {
    public static PromptSummaryResponse from (PromptSummary ps) {
        return new PromptSummaryResponse(ps.id(), ps.name(), ps.version(), ps.status(), ps.model(), ps.createdAt());
    }
}
