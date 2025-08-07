package com.example.konnect_backend.domain.document.entity;

import com.example.konnect_backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTranslation extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long translationId;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    private String translatedLanguage = "en"; // default: 영어

    @Column(columnDefinition = "TEXT")
    private String translatedText;

    @Column(columnDefinition = "TEXT")
    private String summary;

}
