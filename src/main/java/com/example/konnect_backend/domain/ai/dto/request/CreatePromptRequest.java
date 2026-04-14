package com.example.konnect_backend.domain.ai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePromptRequest(@NotBlank String moduleName, @NotBlank String template,
                                  @NotNull @Min(500) @Max(10000) Integer maxTokens, @NotBlank String model) {
}
