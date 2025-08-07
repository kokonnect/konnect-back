package com.example.konnect_backend.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 요청 DTO")
public class SignInRequest {

    @NotBlank(message = "소셜 ID는 필수입니다")
    @Schema(description = "소셜 로그인 ID", example = "kakao_123456789")
    private String socialId;
}