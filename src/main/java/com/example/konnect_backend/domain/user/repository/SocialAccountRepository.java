package com.example.konnect_backend.domain.user.repository;

import com.example.konnect_backend.domain.user.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.konnect_backend.domain.user.entity.status.Provider;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndProviderUserId(Provider p, String providerUserId);
}
