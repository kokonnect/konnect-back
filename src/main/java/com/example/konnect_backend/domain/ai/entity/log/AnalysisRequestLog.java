package com.example.konnect_backend.domain.ai.entity.log;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "analysis_request_log",
    indexes = {
        @Index(name = "idx_user_created", columnList = "user_id, created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_request_uuid", columnNames = "request_uuid")
    }
)
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
public class AnalysisRequestLog extends CreatedAtBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.BINARY) // UUID - BINARY(16)
    @Column(name = "request_uuid", nullable = false, columnDefinition = "BINARY(16)")
    private UUID requestUuid;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 32)
    private String status;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    // 시각 통일을 위해 직접 주입
    @Column(updatable = false, nullable = false)
    @JsonFormat(timezone = "Asia/Seoul")
    protected LocalDateTime createdAt;

    private AnalysisRequestLog(UUID requestUuid, Long userId, String status, Integer latencyMs, LocalDateTime createdAt) {
        this.requestUuid = requestUuid;
        this.userId = userId;
        this.status = status;
        this.latencyMs = latencyMs;
        this.createdAt = createdAt;
    }

    public static AnalysisRequestLog succeed(UUID requestUuid, Long userId, Integer latencyMs, LocalDateTime createdAt) {
        return new AnalysisRequestLog(requestUuid, userId, "SUCCESS", latencyMs, createdAt);
    }

    public static AnalysisRequestLog fail(UUID requestUuid, Long userId, Integer latencyMs, LocalDateTime createdAt) {
        return new AnalysisRequestLog(requestUuid, userId, "FAIL", latencyMs, createdAt);
    }
}