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

    @NotBlank(message = "소셜 액세스 토큰은 필수입니다")
    @Schema(description = "소셜 플랫폼에서 발급받은 액세스 토큰", example = "ya29.a0AfH6SMB...")
    private String accessToken;

    @NotNull(message = "OAuth 제공자는 필수입니다")
    @Schema(description = "OAuth 제공자", example = "KAKAO")
    private Provider provider;

    @Schema(description = "게스트 액세스 토큰 (선택, 게스트 데이터 병합용)")
    private String guestAccessToken;
}
