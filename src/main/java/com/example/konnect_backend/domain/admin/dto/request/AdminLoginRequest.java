package com.example.konnect_backend.domain.admin.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "관리자 로그인 요청")
public class AdminLoginRequest {

    @NotBlank
    @JsonProperty("id")
    @Schema(description = "로그인 ID", example = "root")
    private String loginId;

    @NotBlank
    @Schema(description = "비밀번호")
    private String password;
}
