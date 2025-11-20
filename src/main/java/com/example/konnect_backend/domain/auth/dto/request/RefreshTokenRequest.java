package com.example.konnect_backend.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토큰 재발급 요청")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh Token은 필수입니다.")
    @Schema(description = "Refresh Token", required = true)
    private String refreshToken;
}