// src/main/java/com/example/konnect_backend/domain/auth/service/AuthService.java
package com.example.konnect_backend.domain.auth.service;

import com.example.konnect_backend.domain.auth.dto.request.ChildCreateDto;
import com.example.konnect_backend.domain.auth.dto.request.SignInRequest;
import com.example.konnect_backend.domain.auth.dto.request.SignUpRequest;
import com.example.konnect_backend.domain.auth.dto.response.AuthResponse;
import com.example.konnect_backend.domain.user.entity.Child;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.repository.ChildRepository;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.security.JwtTokenProvider;
import com.example.konnect_backend.global.security.SecurityUtil;
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
    private final ChildRepository childRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 1) 게스트 발급: User 레코드만 만들고 guest=true
     */
    @Transactional
    public AuthResponse issueGuest() {
        User guest = User.builder()
                .guest(true)
                .build();
        User saved = userRepository.save(guest);
        String token = jwtTokenProvider.createToken(saved.getId(), "GUEST");
        return AuthResponse.of(token, saved.getId(), "GUEST", null);
    }

    /**
     * 2) 회원가입(승격): 현재 토큰의 userId(게스트)를 정식 회원으로 승격 + Child 생성
     */
    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserIdOrNull();
        if (currentUserId == null) {
            throw new GeneralException(ErrorStatus.GUEST_TOKEN_REQUIRED);
        }

        User u = userRepository.findById(currentUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GUEST_NOT_FOUND));

        if (!u.isGuest()) {
            throw new GeneralException(ErrorStatus.ALREADY_MEMBER);
        }
        if (userRepository.existsBySocialId(request.getSocialId())) {
            throw new GeneralException(ErrorStatus.SOCIAL_ID_DUPLICATE);
        }


        // 승격
        u.upgradeToMember(
                request.getSocialId(),
                request.getName(),
                request.getProvider(),
                request.getBirthDate(),
                request.getLanguage()
        );

        // 자녀 생성
        if (request.getChildren() != null) {
            for (ChildCreateDto c : request.getChildren()) {
                Child child = Child.builder()
                        .user(u)
                        .name(c.getName())
                        .school(c.getSchool())
                        .grade(c.getGrade())
                        .birthDate(c.getBirthDate())
                        .build();
                childRepository.save(child);
            }
        }

        // USER 토큰 재발급
        String access = jwtTokenProvider.createToken(u.getId(), "USER");
        return AuthResponse.of(access, u.getId(), "USER", u.getProvider());
    }

    /**
     * 3) 로그인: socialId로 조회 후 USER 토큰 발급
     */
    public AuthResponse signIn(SignInRequest request) {
        User user = userRepository.findBySocialId(request.getSocialId())
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        if (user.isGuest()) {
            throw new IllegalStateException("아직 게스트 상태입니다. 회원가입을 완료하세요.");
        }

        String access = jwtTokenProvider.createToken(user.getId(), "USER");
        return AuthResponse.of(access, user.getId(), "USER", user.getProvider());
    }
}
