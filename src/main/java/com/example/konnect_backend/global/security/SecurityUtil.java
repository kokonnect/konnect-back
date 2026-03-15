// src/main/java/com/example/konnect_backend/global/security/SecurityUtil.java
package com.example.konnect_backend.global.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    public static Long getCurrentUserIdOrNull() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) return null;

        Object principal = authentication.getPrincipal();

        if (principal instanceof String str) {
            return Long.valueOf(str);
        }

        return null;
    }

    public static String getDeviceUuid(HttpServletRequest request) {
        return request.getHeader("X-Device-UUID");
    }
}
