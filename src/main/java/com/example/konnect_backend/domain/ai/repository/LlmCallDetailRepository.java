package com.example.konnect_backend.domain.ai.repository;

import com.example.konnect_backend.domain.ai.domain.entity.log.LlmCallDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmCallDetailRepository extends JpaRepository<LlmCallDetail, Long> {
}
