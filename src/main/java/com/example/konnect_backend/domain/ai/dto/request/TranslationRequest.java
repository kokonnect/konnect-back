package com.example.konnect_backend.domain.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TranslationRequest {
    
    @NotBlank(message = "번역할 텍스트는 필수입니다.")
    private String text;
    
    @NotNull(message = "원본 언어는 필수입니다.")
    private LanguageType sourceLanguage;
    
    // 간단한 언어로 번역할지 여부
    private boolean useSimpleLanguage = true;
    
    public enum LanguageType {
        KOREAN("한국어"),
        ENGLISH("영어"),
        VIETNAMESE("베트남어"),
        CHINESE("중국어"),
        THAI("태국어"),
        JAPANESE("일본어"),
        FILIPINO("필리핀어"),
        KHMER("크메르어");
        
        private final String displayName;
        
        LanguageType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
