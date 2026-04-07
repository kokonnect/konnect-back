package com.example.konnect_backend.domain.ai.dto.response;

import com.example.konnect_backend.domain.ai.dto.internal.PromptTemplateWithModelName;
import com.example.konnect_backend.domain.ai.domain.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.type.PromptStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PromptResponse(Long id, String name, Integer version, String template,
                             PromptStatus status, Integer maxTokens, String model, List<SlotResponse> slots,
                             LocalDateTime createdAt) {

    public static PromptResponse from(PromptTemplateWithModelName pm) {
        PromptTemplate p = pm.prompt();
        List<SlotResponse> slotResponses = p.getSlots().stream()
            .map(s -> new SlotResponse(s.getSlotKey(), s.getSlotOrder())).toList();

        return new PromptResponse(p.getId(), p.getModuleName(), p.getVersion(), p.getTemplate(),
            p.getStatus(), p.getMaxTokens(), pm.modelName(), slotResponses, p.getCreatedAt());
    }
}
