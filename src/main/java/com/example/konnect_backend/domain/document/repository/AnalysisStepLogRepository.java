package com.example.konnect_backend.domain.document.repository;

import com.example.konnect_backend.domain.document.entity.AnalysisStepLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisStepLogRepository extends JpaRepository<AnalysisStepLog, Long> {

    List<AnalysisStepLog> findByDocumentAnalysisIdOrderByStepOrderAsc(Long analysisId);

    List<AnalysisStepLog> findByDocumentAnalysisIdAndStepName(Long analysisId, String stepName);

    long countByDocumentAnalysisIdAndStatus(Long analysisId, AnalysisStepLog.StepStatus status);
}
