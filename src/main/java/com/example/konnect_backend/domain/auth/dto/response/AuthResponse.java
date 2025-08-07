package com.example.konnect_backend.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "인증 응답 DTO")
public class AuthResponse {

    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "사용자 닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "소셜 로그인 제공자", example = "KAKAO")
    private String provider;

    public static AuthResponse of(String accessToken, Long userId, String nickname, String provider) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(userId)
                .nickname(nickname)
                .provider(provider)
                .build();
    }
}