package com.example.konnect_backend.domain.ai.repository;

import com.example.konnect_backend.domain.ai.entity.log.LlmCallMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmCallMetadataRepository extends JpaRepository<LlmCallMetadata, Long> {
}
