package com.example.konnect_backend.domain.ai.dto.response;

import com.example.konnect_backend.domain.ai.dto.request.TranslationRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationResponse {
    
    private String originalText;
    private String translatedText;
    private TranslationRequest.LanguageType sourceLanguage;
    private String sourceLanguageName;
    private String targetLanguage; // 항상 한국어
    private Boolean usedSimpleLanguage;
    private Integer originalTextLength;
    private Integer translatedTextLength;
    private Long processingTimeMs;
}
