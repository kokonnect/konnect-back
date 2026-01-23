package com.example.konnect_backend.domain.document.entity;

import com.example.konnect_backend.domain.ai.type.DocumentType;
import com.example.konnect_backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "document_analysis")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DocumentAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 20)
    private DocumentType documentType;

    @Column(name = "classification_confidence")
    private Double classificationConfidence;

    @Column(name = "classification_keywords", length = 500)
    private String classificationKeywords;

    @Column(name = "classification_reasoning", columnDefinition = "TEXT")
    private String classificationReasoning;

    @Column(name = "extracted_schedules_json", columnDefinition = "TEXT")
    private String extractedSchedulesJson;

    @Column(name = "additional_info_json", columnDefinition = "TEXT")
    private String additionalInfoJson;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "ocr_method", length = 50)
    private String ocrMethod;

    @Column(name = "total_steps")
    @Builder.Default
    private Integer totalSteps = 0;

    @Column(name = "completed_steps")
    @Builder.Default
    private Integer completedSteps = 0;

    @Column(name = "failed_step", length = 50)
    private String failedStep;

    @OneToMany(mappedBy = "documentAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnalysisStepLog> stepLogs = new ArrayList<>();

    public void addStepLog(AnalysisStepLog stepLog) {
        this.stepLogs.add(stepLog);
    }

    public void updateStepCounts(int total, int completed) {
        this.totalSteps = total;
        this.completedSteps = completed;
    }

    public void setFailedStep(String stepName) {
        this.failedStep = stepName;
    }
}
