package com.example.konnect_backend.domain.auth.service;

import com.example.konnect_backend.domain.auth.dto.request.NativeOAuthRequest;
import com.example.konnect_backend.domain.auth.dto.request.NativeSignUpRequest;
import com.example.konnect_backend.domain.auth.dto.response.OAuthLoginResponse;
import com.example.konnect_backend.domain.auth.dto.response.SignUpResponse;
import com.example.konnect_backend.domain.auth.dto.response.SocialUserInfo;
import com.example.konnect_backend.domain.user.entity.SocialAccount;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.entity.status.Provider;
import com.example.konnect_backend.domain.user.repository.SocialAccountRepository;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 네이티브 앱용 인증 서비스
 * - 소셜 로그인/회원가입 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NativeAuthService {

    private final NativeOAuthService nativeOAuthService;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final DataMergeService dataMergeService;

    /**
     * 소셜 로그인 처리
     * 1. 소셜 토큰으로 사용자 정보 조회
     * 2. 기존 회원이면 JWT 발급
     * 3. 신규 사용자면 회원가입 필요 응답
     */
    @Transactional
    public OAuthLoginResponse socialLogin(NativeOAuthRequest request) {
        Provider provider = request.getProvider();

        // 1. 소셜 플랫폼에서 사용자 정보 조회
        SocialUserInfo socialUserInfo = nativeOAuthService.getUserInfo(request.getAccessToken(), provider);
        String providerUserId = socialUserInfo.getProviderUserId();

        log.info("소셜 로그인 시도: provider={}, providerUserId={}", provider, providerUserId);

        // 2. 기존 소셜 계정 조회
        Optional<SocialAccount> existingSocialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(provider, providerUserId);

        if (existingSocialAccount.isPresent()) {
            // 기존 회원 - JWT 발급
            User user = existingSocialAccount.get().getUser();

            // 게스트 토큰이 있으면 데이터 병합
            mergeGuestDataIfPresent(request.getGuestAccessToken(), user);

            String accessToken = jwtTokenProvider.createToken(user.getId(), "USER");
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

            log.info("기존 회원 로그인 성공: userId={}", user.getId());
            return OAuthLoginResponse.memberLogin(accessToken, refreshToken, user.getId(), provider);
        }

        // 3. 신규 사용자 - 회원가입 필요
        log.info("신규 사용자 - 회원가입 필요: providerUserId={}", providerUserId);
        return OAuthLoginResponse.signUpRequired(providerUserId, provider);
    }

    /**
     * 회원가입 처리
     * 1. providerUserId 중복 확인
     * 2. User 생성 + SocialAccount 연결
     * 3. 게스트 데이터 병합 (선택)
     * 4. JWT 발급
     */
    @Transactional
    public SignUpResponse signUp(NativeSignUpRequest request) {
        Provider provider = request.getProvider();
        String providerUserId = request.getProviderUserId();

        log.info("회원가입 시도: provider={}, providerUserId={}", provider, providerUserId);

        // 1. 이미 가입된 소셜 계정인지 확인
        if (socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId).isPresent()) {
            throw new GeneralException(ErrorStatus.SOCIAL_ACCOUNT_ALREADY_EXISTS);
        }

        // 2. User 생성
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .language(request.getLanguage())
                .guest(false)
                .build();
        user = userRepository.save(user);

        // 3. SocialAccount 생성 및 연결
        SocialAccount socialAccount = SocialAccount.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .build();
        socialAccountRepository.save(socialAccount);

        // 4. 게스트 데이터 병합 (선택)
        mergeGuestDataIfPresent(request.getGuestToken(), user);

        // 5. JWT 발급
        String accessToken = jwtTokenProvider.createToken(user.getId(), "USER");
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        log.info("회원가입 완료: userId={}", user.getId());
        return SignUpResponse.of(user.getId(), accessToken, refreshToken);
    }

    /**
     * 게스트 토큰이 있으면 게스트 데이터를 새 사용자로 병합
     */
    private void mergeGuestDataIfPresent(String guestAccessToken, User targetUser) {
        if (guestAccessToken == null || guestAccessToken.isBlank()) {
            return;
        }

        try {
            String rawToken = guestAccessToken.startsWith("Bearer ")
                    ? guestAccessToken.substring(7)
                    : guestAccessToken;

            Long guestId = jwtTokenProvider.getUserId(rawToken);
            Optional<User> guestOpt = userRepository.findById(guestId);

            if (guestOpt.isPresent() && guestOpt.get().isGuest()) {
                User guest = guestOpt.get();
                dataMergeService.mergeGuestToUser(guest, targetUser);
                log.info("게스트 데이터 병합 완료: guestId={} -> userId={}", guestId, targetUser.getId());
            }
        } catch (Exception e) {
            log.warn("게스트 데이터 병합 실패 (무시): {}", e.getMessage());
        }
    }
}
