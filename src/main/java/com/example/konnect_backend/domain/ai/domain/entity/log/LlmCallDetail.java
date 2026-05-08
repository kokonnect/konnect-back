package com.example.konnect_backend.domain.ai.domain.entity.log;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "llm_call_detail", indexes = {
    @Index(name = "idx_request_uuid", columnList = "request_uuid")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LlmCallDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "request_uuid", nullable = false, columnDefinition = "BINARY(16)")
    private UUID requestUuid;

    @Column(name = "prompt_module_name", length = 100)
    private String promptModuleName;

    @Lob
    @Column(name = "input_vars", columnDefinition = "TEXT")
    private String inputVars;

    @Lob
    @Column(name = "response_text", columnDefinition = "TEXT")
    private String responseText;

    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    private LlmCallDetail(UUID requestUuid, String promptModuleName, String inputVars,
                          String responseText, LocalDateTime createdAt) {
        this.requestUuid = requestUuid;
        this.promptModuleName = promptModuleName;
        this.inputVars = inputVars;
        this.responseText = responseText;
        this.createdAt = createdAt;
    }

    public static LlmCallDetail of(UUID requestUuid, String promptModuleName, String inputVars,
                                   String responseText, LocalDateTime createdAt) {
        return new LlmCallDetail(requestUuid, promptModuleName, inputVars, responseText, createdAt);
    }
}
