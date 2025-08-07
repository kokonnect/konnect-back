package com.example.konnect_backend.domain.auth.controller;

import com.example.konnect_backend.domain.auth.dto.request.SignInRequest;
import com.example.konnect_backend.domain.auth.dto.request.SignUpRequest;
import com.example.konnect_backend.domain.auth.dto.response.AuthResponse;
import com.example.konnect_backend.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 존재하는 사용자",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    public ResponseEntity<AuthResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        log.info("Sign up request received for socialId: {}", request.getSocialId());
        AuthResponse response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/signin")
    @Operation(
            summary = "로그인",
            description = "소셜 ID를 통한 로그인을 처리하고 JWT 토큰을 발급합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    public ResponseEntity<AuthResponse> signIn(@Valid @RequestBody SignInRequest request) {
        log.info("Sign in request received for socialId: {}", request.getSocialId());
        AuthResponse response = authService.signIn(request);
        return ResponseEntity.ok(response);
    }
}