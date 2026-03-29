package com.example.konnect_backend.global.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class AppleOAuthProperties {

    @Value("${apple.client-id}")
    private String clientId;

    @Value("${apple.team-id}")
    private String teamId;

    @Value("${apple.key-id}")
    private String keyId;

    @Value("${apple.service-id}")  // Android용 Services ID
    private String serviceId;

    @Value("${apple.private-key}")
    private String privateKey;

    @Value("${apple.issuer}")
    private String issuer;

    public String getPrivateKeyFormatted() {
        return privateKey.replace("\\n", "\n");
    }
}