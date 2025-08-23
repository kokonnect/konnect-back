package com.example.konnect_backend.domain.ai.dto;

public enum TargetLanguage {
    KOREAN("한국어", "ko"),
    ENGLISH("영어", "en"),
    VIETNAMESE("베트남어", "vi"),
    CHINESE("중국어", "zh"),
    THAI("태국어", "th"),
    JAPANESE("일본어", "ja"),
    FILIPINO("필리핀어", "tl"),
    KHMER("크메르어", "km");
    
    private final String displayName;
    private final String languageCode;
    
    TargetLanguage(String displayName, String languageCode) {
        this.displayName = displayName;
        this.languageCode = languageCode;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getLanguageCode() {
        return languageCode;
    }
}
