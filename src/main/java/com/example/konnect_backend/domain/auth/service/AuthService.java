package com.example.konnect_backend.domain.auth.service;

import com.example.konnect_backend.domain.auth.dto.request.SignInRequest;
import com.example.konnect_backend.domain.auth.dto.request.SignUpRequest;
import com.example.konnect_backend.domain.auth.dto.response.AuthResponse;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.code.status.ErrorStatus;

import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        // 이미 존재하는 사용자인지 확인
        if (userRepository.existsBySocialId(request.getSocialId())) {
            throw new GeneralException(ErrorStatus.USER_ALREADY_EXISTS);
        }

        // 새 사용자 생성
        User newUser = User.builder()
                .socialId(request.getSocialId())
                .provider(request.getProvider())
                .nickname(request.getNickname())
                .registeredAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(newUser);
        
        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createToken(savedUser.getSocialId());

        log.info("New user registered: socialId={}, provider={}", savedUser.getSocialId(), savedUser.getProvider());

        return AuthResponse.of(
                accessToken,
                savedUser.getId(),
                savedUser.getNickname(),
                savedUser.getProvider().name()
        );
    }

    public AuthResponse signIn(SignInRequest request) {
        // 사용자 조회
        User user = userRepository.findBySocialId(request.getSocialId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createToken(user.getSocialId());

        log.info("User signed in: socialId={}, provider={}", user.getSocialId(), user.getProvider());

        return AuthResponse.of(
                accessToken,
                user.getId(),
                user.getNickname(),
                user.getProvider() != null ? user.getProvider().name() : null
        );
    }

}