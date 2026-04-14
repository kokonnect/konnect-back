package com.example.konnect_backend.domain.ai.dto.response;

import com.example.konnect_backend.domain.ai.domain.entity.AiModel;

public record ModelResponse(Long id, String name, String useCase) {
    public static ModelResponse from(AiModel m) {
        return new ModelResponse(m.getId(), m.getName(), m.getUseCase());
    }
}
