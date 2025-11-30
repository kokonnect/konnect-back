package com.example.konnect_backend.domain.auth.controller;

import com.example.konnect_backend.domain.auth.dto.request.GuestTokenRequest;
import com.example.konnect_backend.domain.auth.dto.request.RefreshTokenRequest;
import com.example.konnect_backend.domain.auth.dto.response.AuthResponse;
import com.example.konnect_backend.domain.auth.service.AuthService;
import com.example.konnect_backend.global.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/guest")
    @Operation(
            summary = "게스트 토큰 발급",
            description = "게스트용 계정을 생성하고 토큰을 발급합니다. 앱에서 로그인 전에 사용할 수 있습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<AuthResponse> issueGuest(@Valid @RequestBody GuestTokenRequest request) {
        return ApiResponse.onSuccess(authService.issueGuest(request.getLanguage()));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "토큰 재발급",
            description = "Refresh Token으로 새로운 Access Token과 Refresh Token을 발급받습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token입니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.onSuccess(authService.refreshToken(request.getRefreshToken()));
    }
}
