package com.example.konnect_backend.domain.user.repository;

import com.example.konnect_backend.domain.user.entity.Usage;
import com.example.konnect_backend.domain.user.entity.status.IdentityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface UsageRepository extends JpaRepository<Usage, Long> {

    Optional<Usage> findByIdentityTypeAndIdentityKeyAndDate(
            IdentityType identityType,
            String identityKey,
            LocalDate date
    );

}