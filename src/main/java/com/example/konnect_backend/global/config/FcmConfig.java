package com.example.konnect_backend.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FcmConfig {

    @Value("${firebase.type:service_account}")
    private String type;

    @Value("${firebase.project-id:}")
    private String projectId;

    @Value("${firebase.private-key-id:}")
    private String privateKeyId;

    @Value("${firebase.private-key:}")
    private String privateKey;

    @Value("${firebase.client-email:}")
    private String clientEmail;

    @Value("${firebase.client-id:}")
    private String clientId;

    @Value("${firebase.auth-uri:https://accounts.google.com/o/oauth2/auth}")
    private String authUri;

    @Value("${firebase.token-uri:https://oauth2.googleapis.com/token}")
    private String tokenUri;

    @Value("${firebase.auth-provider-x509-cert-url:https://www.googleapis.com/oauth2/v1/certs}")
    private String authProviderX509CertUrl;

    @Value("${firebase.client-x509-cert-url:}")
    private String clientX509CertUrl;

    @Value("${firebase.universe-domain:googleapis.com}")
    private String universeDomain;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                if (projectId.isEmpty() || privateKey.isEmpty() || clientEmail.isEmpty()) {
                    log.warn("Firebase 환경변수가 설정되지 않았습니다. FCM 기능이 비활성화됩니다.");
                    log.warn("필요한 환경변수: FIREBASE_PROJECT_ID, FIREBASE_PRIVATE_KEY, FIREBASE_CLIENT_EMAIL");
                    return;
                }

                String jsonCredentials = buildCredentialsJson();

                try (InputStream credentialsStream = new ByteArrayInputStream(
                        jsonCredentials.getBytes(StandardCharsets.UTF_8))) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                            .build();

                    FirebaseApp.initializeApp(options);
                    log.info("Firebase 초기화 완료 (환경변수 기반)");
                }
            }
        } catch (IOException e) {
            log.error("Firebase 초기화 실패: {}", e.getMessage());
        }
    }

    private String buildCredentialsJson() {
        // 환경변수에서 읽은 private key의 \n을 실제 줄바꿈으로 변환
        String formattedPrivateKey = privateKey.replace("\\n", "\n");

        return String.format("""
                {
                  "type": "%s",
                  "project_id": "%s",
                  "private_key_id": "%s",
                  "private_key": "%s",
                  "client_email": "%s",
                  "client_id": "%s",
                  "auth_uri": "%s",
                  "token_uri": "%s",
                  "auth_provider_x509_cert_url": "%s",
                  "client_x509_cert_url": "%s",
                  "universe_domain": "%s"
                }
                """,
                type,
                projectId,
                privateKeyId,
                formattedPrivateKey,
                clientEmail,
                clientId,
                authUri,
                tokenUri,
                authProviderX509CertUrl,
                clientX509CertUrl,
                universeDomain
        );
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("FirebaseApp이 초기화되지 않아 FirebaseMessaging Bean을 생성할 수 없습니다.");
            return null;
        }
        return FirebaseMessaging.getInstance();
    }
}
