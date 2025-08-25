package com.example.konnect_backend.domain.message.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageComposeResponse {
    
    private String originalMessage;
    private String translatedMessage;
    private String targetLanguage;
    private Long processingTimeMs;
}