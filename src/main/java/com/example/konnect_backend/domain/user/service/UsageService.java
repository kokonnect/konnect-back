package com.example.konnect_backend.domain.user.service;

import com.example.konnect_backend.domain.user.entity.Usage;
import com.example.konnect_backend.domain.user.entity.status.IdentityType;
import com.example.konnect_backend.domain.user.entity.status.UsageType;
import com.example.konnect_backend.domain.user.repository.UsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UsageService {

    private final UsageRepository usageRepository;

    @Transactional
    public boolean checkAndIncreaseUsage(
            IdentityType type,
            String key,
            UsageType usageType,
            int limit
    ) {
        LocalDate today = LocalDate.now();

        // 1. row 없으면 생성 (race condition 방어)
        usageRepository.findByIdentityTypeAndIdentityKeyAndUsageTypeAndDate(
                type, key, usageType, today
        ).orElseGet(() -> {
            try {
                return usageRepository.save(
                        Usage.builder()
                                .identityType(type)
                                .identityKey(key)
                                .usageType(usageType)
                                .date(today)
                                .count(0)
                                .build()
                );
            } catch (DataIntegrityViolationException e) {
                // 다른 트랜잭션이 이미 insert한 경우
                return usageRepository.findByIdentityTypeAndIdentityKeyAndUsageTypeAndDate(
                        type, key, usageType, today
                ).orElseThrow();
            }
        });

        // 2. 증가 시도 (atomic)
        int updated = usageRepository.increaseIfUnderLimit(
                type, key, usageType, today, limit
        );

        return updated > 0;
    }
}
