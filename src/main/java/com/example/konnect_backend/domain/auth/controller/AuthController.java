package com.example.konnect_backend.domain.auth.controller;

import com.example.konnect_backend.domain.auth.dto.request.SignInRequest;
import com.example.konnect_backend.domain.auth.dto.request.SignUpRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(
            summary = "회원가입",
            description = "소셜 로그인 정보를 통한 회원가입을 처리합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON400", description = "잘못된 요청입니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<AuthResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        log.info("Sign up request received for socialId: {}", request.getSocialId());
        AuthResponse response = authService.signUp(request);
        return ApiResponse.onSuccess(response);
    }

    @PostMapping("/guest")
    @Operation(summary = "게스트 토큰 발급", description = "회원가입 전, 게스트용 계정을 생성하고 토큰을 발급합니다.")
    public ApiResponse<AuthResponse> issueGuest() {
        return ApiResponse.onSuccess(authService.issueGuest());
    }


    @PostMapping("/signin")
    @Operation(
            summary = "로그인",
            description = "소셜 ID를 통한 로그인을 처리하고 JWT 토큰을 발급합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    public ApiResponse<AuthResponse> signIn(@Valid @RequestBody SignInRequest request) {
        log.info("Sign in request received for socialId: {}", request.getSocialId());
        AuthResponse response = authService.signIn(request);
        return ApiResponse.onSuccess(response);
    }
}