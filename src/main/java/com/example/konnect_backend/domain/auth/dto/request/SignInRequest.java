// src/main/java/com/example/konnect_backend/domain/auth/dto/request/SignInRequest.java
package com.example.konnect_backend.domain.auth.dto.request;

import com.example.konnect_backend.domain.user.entity.status.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "소셜 로그인(테스트용) 요청 DTO")
public class SignInRequest {

    @NotNull
    @Schema(example = "GOOGLE", description = "소셜 제공자")
    private Provider provider;

    @NotBlank
    @Schema(example = "1234567890", description = "provider 고유 식별자 (Google=sub, Kakao=id)")
    private String providerUserId;

    @Schema(example = "user@email.com", description = "선택. 있을 때 동일 이메일 유저와 연결")
    private String email;

    @Schema(example = "홍길동")
    private String name;
}
