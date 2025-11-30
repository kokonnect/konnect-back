package com.example.konnect_backend.domain.auth.service;

import com.example.konnect_backend.domain.auth.dto.response.AuthResponse;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.entity.status.Language;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 게스트 토큰 발급
     * - 게스트 User 레코드 생성
     * - guest=true 상태로 생성
     */
    @Transactional
    public AuthResponse issueGuest(Language language) {
        User guest = User.builder()
                .guest(true)
                .language(language)
                .build();
        User saved = userRepository.save(guest);

        String accessToken = jwtTokenProvider.createToken(saved.getId(), "GUEST");
        String refreshToken = jwtTokenProvider.createRefreshToken(saved.getId());

        log.info("게스트 토큰 발급: userId={}", saved.getId());
        return AuthResponse.of(accessToken, refreshToken, saved.getId(), "GUEST");
    }

    /**
     * 토큰 재발급
     * - Refresh Token으로 새로운 Access Token과 Refresh Token 발급
     */
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        String newAccessToken = jwtTokenProvider.createToken(user.getId(), user.isGuest() ? "GUEST" : "USER");
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        log.info("토큰 재발급: userId={}", user.getId());
        return AuthResponse.of(newAccessToken, newRefreshToken, user.getId(), user.isGuest() ? "GUEST" : "USER");
    }
}
