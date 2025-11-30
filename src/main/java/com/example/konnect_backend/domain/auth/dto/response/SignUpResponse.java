package com.example.konnect_backend.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "회원가입 응답 DTO")
public class SignUpResponse {

    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType;

    @Schema(description = "생성된 사용자 ID", example = "456")
    private Long userId;

    @Schema(description = "사용자 역할", example = "USER")
    private String role;

    public static SignUpResponse of(Long userId, String accessToken, String refreshToken) {
        return SignUpResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(userId)
                .role("USER")
                .build();
    }
}
