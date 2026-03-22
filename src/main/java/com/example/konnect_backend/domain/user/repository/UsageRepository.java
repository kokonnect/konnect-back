package com.example.konnect_backend.domain.user.repository;

import com.example.konnect_backend.domain.user.entity.Usage;
import com.example.konnect_backend.domain.user.entity.status.IdentityType;
import com.example.konnect_backend.domain.user.entity.status.UsageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface UsageRepository extends JpaRepository<Usage, Long> {

    Optional<Usage> findByIdentityTypeAndIdentityKeyAndDate(
            IdentityType identityType,
            String identityKey,
            LocalDate date
    );

    Optional<Usage> findByIdentityTypeAndIdentityKeyAndUsageTypeAndDate(
            IdentityType identityType,
            String identityKey,
            UsageType usageType,
            LocalDate date
    );

    @Modifying
    @Query("""
    UPDATE Usage u
    SET u.count = u.count + 1
    WHERE u.identityType = :type
      AND u.identityKey = :key
      AND u.usageType = :usageType
      AND u.date = :date
      AND u.count < :limit
    """)
        int increaseIfUnderLimit(
                @Param("type") IdentityType type,
                @Param("key") String key,
                @Param("usageType") UsageType usageType,
                @Param("date") LocalDate date,
                @Param("limit") int limit
        );

}