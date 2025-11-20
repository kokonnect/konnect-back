package com.example.konnect_backend.global.security.oauth;

import com.example.konnect_backend.global.security.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Long userId = ((Number) oAuth2User.getAttribute("userId")).longValue();
        String providerUserId = String.valueOf(oAuth2User.getAttribute("providerUserId"));

        // Access Token과 Refresh Token 모두 발급
        String accessToken = jwtTokenProvider.createToken(userId, "USER");
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        // 프론트엔드 URL로 리다이렉트
        String redirectUrl = String.format("%s/auth/callback?accessToken=%s&refreshToken=%s&providerUserId=%s",
                frontendUrl, accessToken, refreshToken, providerUserId);

        response.sendRedirect(redirectUrl);
    }

}
