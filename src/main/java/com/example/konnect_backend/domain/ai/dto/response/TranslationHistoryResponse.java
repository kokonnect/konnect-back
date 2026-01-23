package com.example.konnect_backend.domain.ai.dto.response;

import com.example.konnect_backend.domain.ai.type.FileType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationHistoryResponse {
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranslationHistoryItem {
        private Long documentId;
        private String title;
        private String description;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
        
        // 파일 정보
        private String fileName;
        private FileType fileType;
        private Long fileSize;
        private Integer pageCount;
        private String extractedText;
        
        // 번역 정보
        private String translatedLanguage;
        private String translatedText;
        private String summary;
    }
    
    private List<TranslationHistoryItem> histories;
    private int totalCount;
}