package com.example.konnect_backend.domain.ai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record RunPromptRequest(@NotBlank String promptTemplate, @NotNull @Min(500) @Max(10000) Integer maxTokens,
                               @NotBlank String modelName, @NotNull Map<String, String> vars) {
}
