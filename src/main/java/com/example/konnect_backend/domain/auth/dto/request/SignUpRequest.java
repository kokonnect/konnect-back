// src/main/java/com/example/konnect_backend/domain/auth/dto/request/SignUpRequest.java
package com.example.konnect_backend.domain.auth.dto.request;

import com.example.konnect_backend.domain.user.entity.status.Language;
import com.example.konnect_backend.domain.user.entity.status.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Date;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "회원가입 요청 DTO(게스트 승격)")
public class SignUpRequest {


    @NotBlank(message = "이름은 필수입니다")
    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;


    private String email;

    @NotNull
    @Schema(description = "선호 언어", example = "KOREAN")
    private Language language;

    @Size(min = 1, message = "최소 한 명의 자녀 정보가 필요합니다")
    private List<ChildCreateDto> children;
}
