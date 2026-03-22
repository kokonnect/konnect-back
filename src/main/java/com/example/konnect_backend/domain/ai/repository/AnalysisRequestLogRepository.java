package com.example.konnect_backend.domain.ai.repository;

import com.example.konnect_backend.domain.ai.entity.log.AnalysisRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRequestLogRepository extends JpaRepository<AnalysisRequestLog, Long> {
}
