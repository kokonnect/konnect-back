package com.example.konnect_backend.domain.ai.repository;

import com.example.konnect_backend.domain.ai.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {
    // Unique 제약 조건으로 1개만 반환
    Optional<PromptTemplate> findByModuleNameAndVersion(String moduleName, Integer version);
}
