package com.example.konnect_backend.domain.ai.dto.response;

import com.example.konnect_backend.domain.ai.domain.entity.log.AnalysisHistory;
import com.example.konnect_backend.domain.ai.type.FileType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class AnalysisHistoryResponse {
    private List<TranslationHistoryItem> histories;

    @Getter
    @Builder
    public static class TranslationHistoryItem {
        private Long documentId;
        private String title;

        // 파일 정보
        private String fileName;
        private FileType fileType;

        private String extractedText;

        // 번역 정보
        private String translatedLanguage;
        private String translatedText;

        private String summary;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
    }

    public static AnalysisHistoryResponse emptyResponse() {
        return new AnalysisHistoryResponse(List.of());
    }

    public static AnalysisHistoryResponse from(List<AnalysisHistory> histories) {
        List<TranslationHistoryItem> items = histories.stream().map(
            h -> new TranslationHistoryItem(h.getId(), h.getFileName(), h.getFileName(),
                h.getFileType(),
                h.getExtractedText(), h.getTranslatedLanguage(), h.getTranslatedText(),
                h.getSummary(), h.getCreatedAt())).toList();

        return new AnalysisHistoryResponse(items);
    }
}