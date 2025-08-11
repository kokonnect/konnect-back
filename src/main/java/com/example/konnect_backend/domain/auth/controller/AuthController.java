// src/main/java/com/example/konnect_backend/domain/auth/controller/AuthController.java
package com.example.konnect_backend.domain.auth.controller;

import com.example.konnect_backend.domain.auth.dto.request.SignInRequest;
import com.example.konnect_backend.domain.auth.dto.request.SignUpRequest;
import com.example.konnect_backend.domain.auth.dto.response.AuthResponse;
import com.example.konnect_backend.domain.auth.service.AuthService;
import com.example.konnect_backend.domain.user.entity.status.Provider;
import com.example.konnect_backend.global.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
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
    @Operation(summary = "게스트 토큰 발급", description = "게스트용 계정을 생성하고 토큰을 발급합니다(guestToken 쿠키 저장).")
    public ApiResponse<AuthResponse> issueGuest(HttpServletResponse res) {
        AuthResponse rsp = authService.issueGuest();
        // 게스트 토큰을 HttpOnly 쿠키에 저장 → OAuth 성공시 머지에 사용
        Cookie guest = new Cookie("guestToken", rsp.getAccessToken());
        guest.setHttpOnly(true);
        guest.setPath("/");
        guest.setMaxAge(60 * 60 * 24 * 7); // 7일
        res.addCookie(guest);
        return ApiResponse.onSuccess(rsp);
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입(게스트 승격)", description = "현재 토큰의 게스트를 정식 회원으로 승격하고 자녀 정보를 저장합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON400", description = "잘못된 요청입니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<AuthResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ApiResponse.onSuccess(authService.signUp(request));
    }

    /**
     * 테스트용 API - 실제 운영에서는 OAuth2 핸들러가 처리
     * OAuth 없이 소셜 로그인 플로우 테스트
     */
    @PostMapping("/signin")
    @Operation(summary = "소셜 로그인(테스트용)", description = "OAuth 없이 provider+providerUserId로 로그인/가입 처리. 실제 운영에서는 OAuth2를 사용하세요.")
    public ApiResponse<AuthResponse> signIn(@Valid @RequestBody SignInRequest request,
                                            @RequestParam(value = "guestId", required = false) Long guestId) {
        return ApiResponse.onSuccess(authService.signIn(request, guestId));
    }

    /**
     * 테스트용 API - 실제 운영에서는 OAuth2 핸들러가 처리
     * 게스트 토큰을 받아서 소셜 계정과 연결
     */
    @PostMapping("/social-login")
    @Operation(summary = "소셜 로그인 연동(테스트용)", description = "게스트 토큰으로 소셜 계정 연동. 실제 운영에서는 OAuth2를 사용하세요.")
    public ApiResponse<AuthResponse> socialLogin(
            @RequestHeader("Authorization") String guestToken,
            @RequestParam("provider") Provider provider,
            @RequestParam("providerUserId") String providerUserId
    ) {
        return ApiResponse.onSuccess(
                authService.socialLogin(guestToken, provider, providerUserId)
        );
    }
}
