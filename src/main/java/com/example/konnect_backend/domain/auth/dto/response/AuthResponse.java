// src/main/java/com/example/konnect_backend/domain/auth/dto/response/AuthResponse.java
package com.example.konnect_backend.domain.auth.dto.response;

import com.example.konnect_backend.domain.user.entity.status.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "인증 응답 DTO")
public class AuthResponse {
    @Schema(description = "JWT 액세스 토큰")
    private String accessToken;

    @Schema(description = "Refresh Token")
    private String refreshToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType;

    private Long userId;
    private String role;     // GUEST or USER

    public static AuthResponse of(String accessToken, Long userId, String role) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(userId)
                .role(role)
                .build();
    }

    public static AuthResponse of(String accessToken, String refreshToken, Long userId, String role) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(userId)
                .role(role)
                .build();
    }
}
