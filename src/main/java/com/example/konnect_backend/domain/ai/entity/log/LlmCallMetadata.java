package com.example.konnect_backend.domain.ai.entity.log;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "llm_call_metadata", indexes = {@Index(name = "idx_created_at", columnList = "created_at"), @Index(name = "idx_status_created", columnList = "status, created_at"), @Index(name = "idx_prompt_module_name_version_created", columnList = "prompt_module_name, prompt_version, created_at")})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LlmCallMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.BINARY) // UUID - BINARY(16)
    @Column(name = "request_uuid", nullable = false, columnDefinition = "BINARY(16)")
    private UUID requestUuid;

    @Column(nullable = false, length = 30)
    private String model;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(length = 32)
    private String status;

    @Column(name = "prompt_version")
    private Integer promptVersion;

    @Column(name = "prompt_module_name", length = 100)
    private String promptModuleName;

    @Column(name = "finish_reason", length = 32)
    private String finishReason;

    // 원문 로그와의 시각 통일을 위해 직접 주입
    @Column(updatable = false, nullable = false)
    @JsonFormat(timezone = "Asia/Seoul")
    protected LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    public LlmCallMetadata(UUID requestUuid, String model, Integer maxTokens, Integer inputTokens,
                           Integer outputTokens, Integer latencyMs, String status,
                           Integer promptVersion, String promptModuleName, String finishReason,
                           LocalDateTime createdAt) {
        this.requestUuid = requestUuid;
        this.model = model;
        this.maxTokens = maxTokens;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.latencyMs = latencyMs;
        this.status = status;
        this.promptVersion = promptVersion;
        this.promptModuleName = promptModuleName;
        this.finishReason = finishReason;
        this.createdAt = createdAt;
    }

    public static LlmCallMetadata succeed(UUID requestId, String model, int maxTokens, int inputTokens,
                                          int outputTokens, int latency, int promptVersion,
                                          String promptModuleName, String finishReason,
                                          LocalDateTime logTime) {
        return LlmCallMetadata.builder().requestUuid(requestId).model(model).maxTokens(maxTokens)
            .inputTokens(inputTokens).outputTokens(outputTokens).latencyMs(latency)
            .status("SUCCESS").promptVersion(promptVersion).promptModuleName(promptModuleName)
            .finishReason(finishReason).createdAt(logTime).build();
    }

    public static LlmCallMetadata fail(UUID requestId, int latency, String promptModuleName,
                                       int promptVersion, LocalDateTime logTime) {
        return LlmCallMetadata.builder().requestUuid(requestId).model(null).inputTokens(null)
            .outputTokens(null).latencyMs(latency).status("FAIL").promptVersion(promptVersion)
            .promptModuleName(promptModuleName).finishReason(null).createdAt(logTime).build();
    }
}