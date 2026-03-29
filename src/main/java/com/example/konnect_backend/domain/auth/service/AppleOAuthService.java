package com.example.konnect_backend.domain.auth.service;

import com.example.konnect_backend.domain.auth.dto.response.SocialUserInfo;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.config.AppleOAuthProperties;
import com.example.konnect_backend.global.exception.GeneralException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleOAuthService {

    private final AppleOAuthProperties properties;
    private final ApplePublicKeyProvider keyProvider;

    public SocialUserInfo getUserInfo(String idToken) {
        try {
            Claims claims = validateToken(idToken);

            String providerUserId = claims.getSubject();
            String email = claims.get("email", String.class);

            log.info("애플 로그인 성공: {}", providerUserId);

            return SocialUserInfo.builder()
                    .providerUserId(providerUserId)
                    .email(email)
                    .nickname("AppleUser")
                    .build();

        } catch (Exception e) {
            log.error("Apple 로그인 실패", e);
            throw new GeneralException(ErrorStatus.OAUTH_USER_INFO_FAILED);
        }
    }

    private Claims validateToken(String idToken) {
        try {
            // 1. 헤더 파싱
            String[] parts = idToken.split("\\.");
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));

            Map<String, Object> header = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(headerJson, Map.class);

            String kid = (String) header.get("kid");

            // 2. Apple public key 가져오기
            Map<String, Object> keys = keyProvider.getApplePublicKeys();

            PublicKey publicKey = ApplePublicKeyGenerator.generate(keys, kid);

            // 3. JWT 검증
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(idToken)
                    .getBody();

            // 4. 검증
            validateClaims(claims);

            return claims;

        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.OAUTH_TOKEN_INVALID);
        }
    }

    // AppleOAuthService.validateClaims() 수정
    private void validateClaims(Claims claims) {
        if (!properties.getIssuer().equals(claims.getIssuer())) {
            throw new GeneralException(ErrorStatus.OAUTH_TOKEN_INVALID);
        }

        String audience = claims.getAudience();
        // iOS(Bundle ID) 또는 Android(Services ID) 둘 다 허용
        boolean validAudience = properties.getClientId().equals(audience)
                || properties.getServiceId().equals(audience);

        if (!validAudience) {
            throw new GeneralException(ErrorStatus.OAUTH_TOKEN_INVALID);
        }
    }
}