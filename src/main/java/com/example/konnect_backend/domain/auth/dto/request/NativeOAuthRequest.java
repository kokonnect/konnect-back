package com.example.konnect_backend.domain.auth.dto.request;

import com.example.konnect_backend.domain.user.entity.status.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "네이티브 앱 OAuth 로그인 요청 DTO")
public class NativeOAuthRequest {

    @Schema(description = "소셜 플랫폼에서 발급받은 액세스 토큰", example = "ya29.a0AfH6SMB...")
    private String accessToken;

    @NotNull(message = "OAuth 제공자는 필수입니다")
    @Schema(description = "OAuth 제공자", example = "KAKAO")
    private Provider provider;

    @Schema(description = "디바이스 uuid")
    private String deviceUuid;

    // NativeOAuthRequest.java에 추가
    private String idToken; // Apple 전용
}
