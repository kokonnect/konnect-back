package com.example.konnect_backend.domain.document.entity;

import com.example.konnect_backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_translation")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DocumentTranslation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @Setter
    private Document document;

    @Column(name = "translated_language", length = 10)
    @Builder.Default
    private String translatedLanguage = "en";

    @Column(name = "translated_text", columnDefinition = "TEXT")
    private String translatedText;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    public void updateTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }

    public void updateSummary(String summary) {
        this.summary = summary;
    }
}