package com.example.konnect_backend.domain.document.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_step_log")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AnalysisStepLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private DocumentAnalysis documentAnalysis;

    @Column(name = "step_name", length = 50, nullable = false)
    private String stepName;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "input_text", columnDefinition = "TEXT")
    private String inputText;

    @Column(name = "input_length")
    private Integer inputLength;

    @Column(name = "prompt_template", length = 100)
    private String promptTemplate;

    @Column(name = "model_used", length = 50)
    private String modelUsed;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "parsed_result", columnDefinition = "TEXT")
    private String parsedResult;

    @Column(name = "output_summary", length = 500)
    private String outputSummary;

    @Column(name = "classification_type", length = 20)
    private String classificationType;

    @Column(name = "classification_confidence")
    private Double classificationConfidence;

    @Column(name = "classification_keywords", length = 500)
    private String classificationKeywords;

    @Column(name = "classification_reasoning", columnDefinition = "TEXT")
    private String classificationReasoning;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private StepStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum StepStatus {
        SUCCESS,
        FAILED,
        SKIPPED
    }

    public static AnalysisStepLogBuilder successBuilder() {
        return AnalysisStepLog.builder().status(StepStatus.SUCCESS);
    }

    public static AnalysisStepLogBuilder failedBuilder() {
        return AnalysisStepLog.builder().status(StepStatus.FAILED);
    }

    public static AnalysisStepLogBuilder skippedBuilder() {
        return AnalysisStepLog.builder().status(StepStatus.SKIPPED);
    }
}