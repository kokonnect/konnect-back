package com.example.konnect_backend.domain.auth.dto.request;

import com.example.konnect_backend.domain.user.entity.status.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원가입 요청 DTO")
public class SignUpRequest {

    @NotBlank(message = "소셜 ID는 필수입니다")
    @Schema(description = "소셜 로그인 ID", example = "kakao_123456789")
    private String socialId;

    @NotNull(message = "Provider는 필수입니다")
    @Schema(description = "소셜 로그인 제공자", example = "KAKAO")
    private Provider provider;

    @Schema(description = "사용자 닉네임", example = "홍길동")
    private String nickname;
}