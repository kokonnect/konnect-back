package com.example.konnect_backend.domain.ai.dto.internal;

import com.example.konnect_backend.domain.ai.entity.PromptTemplate;

public record PromptTemplateWithModelName(PromptTemplate prompt, String modelName) {}