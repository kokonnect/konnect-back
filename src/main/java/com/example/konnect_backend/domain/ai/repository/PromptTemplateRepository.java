package com.example.konnect_backend.domain.ai.repository;

import com.example.konnect_backend.domain.ai.dto.internal.PromptSummary;
import com.example.konnect_backend.domain.ai.dto.internal.PromptTemplateWithModelName;
import com.example.konnect_backend.domain.ai.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.type.PromptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {

    // Unique 제약 조건으로 1개만 반환
    @Query("SELECT p FROM PromptTemplate p WHERE p.moduleName = :moduleName AND p.status = :status")
    List<PromptTemplate> findByModuleNameAndStatus(@Param("moduleName") String moduleName, @Param("status") PromptStatus status);

    @Query("""
            SELECT new com.example.konnect_backend.domain.ai.dto.internal.PromptTemplateWithModelName(p, am.name)
            FROM PromptTemplate p JOIN AiModel am ON p.modelId = am.id
            WHERE p.id = :id 
        """)
    Optional<PromptTemplateWithModelName> findPromptById(@Param("id") Long id);

    @Query("""
            SELECT new com.example.konnect_backend.domain.ai.dto.internal.PromptSummary(p.id, p.moduleName, p.version, p.status, am.name, p.createdAt)
            FROM PromptTemplate p JOIN AiModel am ON p.modelId = am.id
            WHERE (:status IS NULL OR p.status = :status) AND (:moduleName IS NULL OR p.moduleName = :moduleName)
        """)
    List<PromptSummary> findPrompts(@Param("status") PromptStatus status,
                                    @Param("moduleName") String moduleName);

    @Query("""
        SELECT p
        FROM PromptTemplate p 
        WHERE p.moduleName = :moduleName 
            AND p.version = (
                    SELECT MAX(p1.version) 
                    FROM PromptTemplate p1
                    WHERE p1.moduleName = :moduleName
                )
    """)
    PromptTemplate getMaxVersionOfModule(@Param("moduleName") String moduleName);
}