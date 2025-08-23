package com.example.konnect_backend.domain.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationResponse {
    
    private String prompt;
    private String generatedContent;
    private String contentType;
    private Long processingTimeMs;
    private Double temperature;
    private Integer maxTokens;
}
