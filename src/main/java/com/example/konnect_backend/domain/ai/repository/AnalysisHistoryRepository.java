package com.example.konnect_backend.domain.ai.repository;

import com.example.konnect_backend.domain.ai.entity.log.AnalysisHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalysisHistoryRepository extends JpaRepository<AnalysisHistory, Long> {
    Page<AnalysisHistory> findByUserId(Long userId, Pageable pageable);
    Page<AnalysisHistory> findByDeviceUuidAndUserIdIsNull(String deviceUuid, Pageable pageable);
    @Modifying
    @Query("""
    UPDATE AnalysisHistory h
    SET h.userId = :userId
    WHERE h.deviceUuid = :deviceUuid
      AND h.userId IS NULL
    """)
    int migrateGuestToUser(@Param("userId") Long userId,  @Param("deviceUuid") String deviceUuid);
}
