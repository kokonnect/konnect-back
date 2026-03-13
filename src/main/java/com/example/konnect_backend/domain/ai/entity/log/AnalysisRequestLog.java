package com.example.konnect_backend.domain.ai.entity.log;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

    private AnalysisRequestLog(UUID requestUuid, Long userId, String status, Integer latencyMs) {
        this.requestUuid = requestUuid;
        this.userId = userId;
        this.status = status;
        this.latencyMs = latencyMs;
    }

    public static AnalysisRequestLog succeed(UUID requestUuid, Long userId, Integer latencyMs) {
        return new AnalysisRequestLog(requestUuid, userId, "SUCCESS", latencyMs);
    }

    public static AnalysisRequestLog fail(UUID requestUuid, Long userId, Integer latencyMs) {
        return new AnalysisRequestLog(requestUuid, userId, "FAIL", latencyMs);
    }
}