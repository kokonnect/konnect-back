package com.example.konnect_backend.domain.user.service;

import com.example.konnect_backend.domain.user.entity.Usage;
import com.example.konnect_backend.domain.user.entity.status.IdentityType;
import com.example.konnect_backend.domain.user.entity.status.UsageType;
import com.example.konnect_backend.domain.user.repository.UsageRepository;
import lombok.RequiredArgsConstructor;
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
    )
    {
        LocalDate today = LocalDate.now();

        Usage usage = usageRepository
                .findByIdentityTypeAndIdentityKeyAndUsageTypeAndDate(type, key, usageType, today)
                .orElseGet(() ->
                        usageRepository.save(
                                Usage.builder()
                                        .identityType(type)
                                        .identityKey(key)
                                        .usageType(usageType)
                                        .date(today)
                                        .count(0)
                                        .build()
                        )
                );

        if (usage.getCount() >= limit) {
            return false;
        }

        usage.increase();

        return true;
    }

}
