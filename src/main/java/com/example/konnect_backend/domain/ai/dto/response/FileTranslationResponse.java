package com.example.konnect_backend.domain.ai.dto.response;

import com.example.konnect_backend.domain.ai.dto.FileType;
import com.example.konnect_backend.domain.ai.dto.TargetLanguage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileTranslationResponse {
    
    private String extractedText;
    private String translatedText;
    private String summary;
    private String originalFileName;
    private FileType fileType;
    private TargetLanguage targetLanguage;
    private String targetLanguageName;
    private Boolean usedSimpleLanguage;
    private Long fileSize;
    private Integer originalTextLength;
    private Integer translatedTextLength;
    private Long totalProcessingTimeMs;
    private Integer pageCount;
    private String sourceLanguageHint;
}
