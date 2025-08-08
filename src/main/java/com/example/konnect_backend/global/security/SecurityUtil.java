// src/main/java/com/example/konnect_backend/global/security/SecurityUtil.java
package com.example.konnect_backend.global.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    /**
     * SecurityContext 에서 현재 인증된 userId(Long) 반환
     * - JwtTokenProvider.getAuthentication() 에서 principal 을 userId 문자열로 세팅했을 때 동작
     */
    public static Long getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        try {
            return Long.valueOf(authentication.getName()); // principal → userId 문자열
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
