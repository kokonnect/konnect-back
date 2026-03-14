package com.example.konnect_backend.domain.ai.repository;

import com.example.konnect_backend.domain.ai.entity.log.AnalysisHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisHistoryRepository extends JpaRepository<AnalysisHistory, Long> {
    Page<AnalysisHistory> findByUserId(Long userId, Pageable pageable);
}
