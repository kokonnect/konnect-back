package com.example.konnect_backend.domain.auth.dto.request;

import com.example.konnect_backend.domain.user.entity.status.Language;
import com.example.konnect_backend.domain.user.entity.status.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "네이티브 앱 회원가입 요청 DTO")
public class NativeSignUpRequest {

    @NotNull(message = "소셜 제공자 사용자 ID는 필수입니다")
    @Schema(description = "소셜 플랫폼 사용자 고유 ID (카카오=id, 구글=sub)", example = "1234567890")
    private String providerUserId;

    @NotNull(message = "OAuth 제공자는 필수입니다")
    @Schema(description = "OAuth 제공자", example = "KAKAO")
    private Provider provider;

    @NotBlank(message = "이름은 필수입니다")
    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;

    @Schema(description = "이메일 주소", example = "user@example.com")
    private String email;

    @NotNull(message = "언어 설정은 필수입니다")
    @Schema(description = "선호 언어", example = "KOREAN")
    private Language language;

    @Schema(description = "게스트 액세스 토큰 (선택, 게스트 데이터 병합용)")
    private String guestAccessToken;

    @Size(min = 1, message = "최소 한 명의 자녀 정보가 필요합니다")
    @Schema(description = "자녀 정보 목록")
    private List<ChildCreateDto> children;
}
