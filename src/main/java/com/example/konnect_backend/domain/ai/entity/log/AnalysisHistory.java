package com.example.konnect_backend.domain.ai.entity.log;

import com.example.konnect_backend.domain.ai.type.FileType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "analysis_history", indexes = {
    @Index(name = "idx_user_created_at", columnList = "user_id, created_at")
})
public class AnalysisHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_log_id")
    private Long requestLogId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    @Enumerated(EnumType.STRING)
    private FileType fileType;

    @Lob
    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "translated_language")
    private String translatedLanguage;

    @Lob
    @Column(name = "translated_text", columnDefinition = "TEXT")
    private String translatedText;

    @Column(name = "summary", length = 1000)
    private String summary;

    // 시각 통일을 위해 직접 주입
    @Column(updatable = false, nullable = false)
    @JsonFormat(timezone = "Asia/Seoul")
    protected LocalDateTime createdAt;

    @Builder
    public AnalysisHistory(Long requestLogId, Long userId, String fileName, FileType fileType,
                           String extractedText, String translatedLanguage, String translatedText,
                           String summary, LocalDateTime createdAt) {
        this.requestLogId = requestLogId;
        this.userId = userId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.extractedText = extractedText;
        this.translatedLanguage = translatedLanguage;
        this.translatedText = translatedText;
        this.summary = summary;
        this.createdAt = createdAt;
    }
}
