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

    @Schema(description = "생성된 사용자 ID")
    private Long userId;

    @Schema(description = "서비스 액세스 토큰")
    private String serviceAccessToken;

    @Schema(description = "서비스 리프레시 토큰")
    private String serviceRefreshToken;

    public static SignUpResponse of(Long userId, String accessToken, String refreshToken) {
        return SignUpResponse.builder()
                .userId(userId)
                .serviceAccessToken(accessToken)
                .serviceRefreshToken(refreshToken)
                .build();
    }
}
