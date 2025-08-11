package com.example.konnect_backend.global.security.oauth;

import com.example.konnect_backend.global.security.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        Object idAttr = oAuth2User.getAttribute("userId");
        if (idAttr == null) {
            throw new IllegalStateException("OAuth2User does not contain userId attribute");
        }

        Long userId = ((Number) idAttr).longValue();
        String providerUserId = String.valueOf(oAuth2User.getAttribute("providerUserId"));

        // JWT 발급
        String accessToken = jwtTokenProvider.createToken(userId, "USER");

        // 토큰 전달 방법: 쿼리 파라미터 예시
        response.sendRedirect("/login/success?token=" + accessToken + "&providerUserId=" + providerUserId);
    }
}
