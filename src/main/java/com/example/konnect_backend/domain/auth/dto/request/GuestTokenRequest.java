package com.example.konnect_backend.domain.auth.dto.request;

import com.example.konnect_backend.domain.user.entity.status.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "게스트 토큰 발급 요청")
public class GuestTokenRequest {
    
    @NotNull(message = "언어는 필수입니다.")
    @Schema(description = "사용자 언어", example = "KOREAN", required = true)
    private Language language;
}