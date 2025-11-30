package com.example.konnect_backend.domain.auth.dto.response;

import com.example.konnect_backend.domain.user.entity.status.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "OAuth 로그인 응답 DTO")
public class OAuthLoginResponse {

    @Schema(description = "기존 회원 여부 (true: 로그인 완료, false: 회원가입 필요)")
    private boolean isMember;

    @Schema(description = "서비스 액세스 토큰 (회원인 경우에만 반환)")
    private String serviceAccessToken;

    @Schema(description = "서비스 리프레시 토큰 (회원인 경우에만 반환)")
    private String serviceRefreshToken;

    @Schema(description = "소셜 제공자 사용자 ID (비회원인 경우에만 반환, 회원가입 시 사용)")
    private String providerUserId;

    @Schema(description = "OAuth 제공자")
    private Provider provider;

    @Schema(description = "사용자 ID (회원인 경우에만 반환)")
    private Long userId;

    /**
     * 기존 회원 로그인 성공 응답
     */
    public static OAuthLoginResponse memberLogin(String accessToken, String refreshToken, Long userId, Provider provider) {
        return OAuthLoginResponse.builder()
                .isMember(true)
                .serviceAccessToken(accessToken)
                .serviceRefreshToken(refreshToken)
                .userId(userId)
                .provider(provider)
                .build();
    }

    /**
     * 신규 사용자 - 회원가입 필요 응답
     */
    public static OAuthLoginResponse signUpRequired(String providerUserId, Provider provider) {
        return OAuthLoginResponse.builder()
                .isMember(false)
                .providerUserId(providerUserId)
                .provider(provider)
                .build();
    }
}
