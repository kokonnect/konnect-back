package com.example.konnect_backend.domain.auth.controller;

import com.example.konnect_backend.domain.auth.dto.request.NativeOAuthRequest;
import com.example.konnect_backend.domain.auth.dto.request.NativeSignUpRequest;
import com.example.konnect_backend.domain.auth.dto.response.OAuthLoginResponse;
import com.example.konnect_backend.domain.auth.dto.response.SignUpResponse;
import com.example.konnect_backend.domain.auth.service.NativeAuthService;
import com.example.konnect_backend.global.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Native Authentication", description = "네이티브 앱용 인증 API")
public class NativeAuthController {

    private final NativeAuthService nativeAuthService;

    @PostMapping("/oauth/login")
    @Operation(
            summary = "소셜 로그인 (네이티브 앱용)",
            description = """
                    네이티브 앱에서 소셜 SDK로 받은 액세스 토큰으로 로그인합니다.

                    **응답 케이스:**
                    - `isMember: true` → 기존 회원, 서비스 JWT 토큰 반환
                    - `isMember: false` → 신규 사용자, 회원가입 필요 (providerUserId 반환)

                    **지원 제공자:** KAKAO, GOOGLE
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 소셜 토큰",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ApiResponse<OAuthLoginResponse> socialLogin(@Valid @RequestBody NativeOAuthRequest request) {
        return ApiResponse.onSuccess(nativeAuthService.socialLogin(request));
    }

    @PostMapping("/oauth/signup")
    @Operation(
            summary = "회원가입 (네이티브 앱용)",
            description = """
                    소셜 로그인 후 신규 사용자 회원가입을 완료합니다.

                    `/auth/oauth/login`에서 `isMember: false`를 받은 후,
                    사용자로부터 추가 정보를 입력받아 회원가입을 진행합니다.

                    **필수 정보:**
                    - providerUserId: 소셜 로그인 시 받은 ID
                    - provider: KAKAO 또는 GOOGLE
                    - name: 사용자 이름

                    **선택 정보:**
                    - email: 이메일
                    - language: 선호 언어
                    - guestToken: 게스트 토큰 (데이터 병합용)

                    ※ 자녀 추가는 별도 API를 사용하세요.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 가입된 소셜 계정",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody NativeSignUpRequest request) {
        return ApiResponse.onSuccess(nativeAuthService.signUp(request));
    }
}
