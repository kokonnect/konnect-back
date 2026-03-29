package com.example.konnect_backend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ApplePublicKeyProvider {

    private final RestTemplate restTemplate;

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";

    @Cacheable("applePublicKeys")  // Spring Cache 적용
    public Map<String, Object> getApplePublicKeys() {
        return restTemplate.getForObject(APPLE_KEYS_URL, Map.class);
    }
}