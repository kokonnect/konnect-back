// src/main/java/com/example/konnect_backend/domain/auth/service/AuthService.java
package com.example.konnect_backend.domain.auth.service;

import com.example.konnect_backend.domain.auth.dto.request.ChildCreateDto;
import com.example.konnect_backend.domain.auth.dto.request.SignInRequest;
import com.example.konnect_backend.domain.auth.dto.request.SignUpRequest;
import com.example.konnect_backend.domain.auth.dto.response.AuthResponse;
import com.example.konnect_backend.domain.user.entity.Child;
import com.example.konnect_backend.domain.user.entity.SocialAccount;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.entity.status.Language;
import com.example.konnect_backend.domain.user.entity.status.Provider;
import com.example.konnect_backend.domain.user.repository.ChildRepository;
import com.example.konnect_backend.domain.user.repository.SocialAccountRepository;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.security.JwtTokenProvider;
import com.example.konnect_backend.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final ChildRepository childRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final DataMergeService dataMergeService;

    /** 1) 게스트 발급: User 레코드만 만들고 guest=true + guestToken 쿠키 세팅은 컨트롤러에서 */
    @Transactional
    public AuthResponse issueGuest(Language language) {
        User guest = User.builder()
                .guest(true)
                .language(language)
                .build();
        User saved = userRepository.save(guest);
        String token = jwtTokenProvider.createToken(saved.getId(), "GUEST");
        return AuthResponse.of(token, saved.getId(), "GUEST");
    }

    /** 2) 회원가입(승격): 현재 토큰의 userId(게스트)를 정식 회원으로 승격 + Child 생성 */
    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserIdOrNull();
        if (currentUserId == null) throw new GeneralException(ErrorStatus.GUEST_TOKEN_REQUIRED);

        User u = userRepository.findById(currentUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GUEST_NOT_FOUND));

        if (!u.isGuest()) throw new GeneralException(ErrorStatus.ALREADY_MEMBER);

        // 승격
        u.upgradeToMember(request.getName(), request.getEmail(), request.getLanguage());

        // 자녀 생성
        if (request.getChildren() != null) {
            for (ChildCreateDto c : request.getChildren()) {
                Child child = Child.builder()
                        .user(u)
                        .name(c.getName())
                        .school(c.getSchool())
                        .grade(c.getGrade())
                        .birthDate(c.getBirthDate())
                        .className(c.getClassName())
                        .teacherName(c.getTeacherName())
                        .build();
                childRepository.save(child);
            }
        }

        String access = jwtTokenProvider.createToken(u.getId(), "USER");
        return AuthResponse.of(access, u.getId(), "USER");
    }

    /**
     * 3) (테스트용) 소셜 로그인 API: OAuth 없이도 레코드 생성/매핑 검증용
     *    실제 운영은 OAuth2SuccessHandler에서 동일 로직이 수행됨.
     */
    @Transactional
    public AuthResponse signIn(SignInRequest req, Long optionalGuestIdForMerge) {
        // 1) 이미 연결되어 있나?
        SocialAccount linked = socialAccountRepository
                .findByProviderAndProviderUserId(req.getProvider(), req.getProviderUserId())
                .orElse(null);

        User base;
        if (linked != null) {
            base = linked.getUser();
        } else {
            // 2) 이메일이 있으면 동일 이메일 유저에 연결
            base = (req.getEmail() != null) ? userRepository.findByEmail(req.getEmail()).orElse(null) : null;
            if (base == null) {
                base = userRepository.save(User.builder()
                        .email(req.getEmail())
                        .name(req.getName())
                        .guest(false)
                        .build());
            }
            // 소셜 연결 생성
            socialAccountRepository.save(SocialAccount.builder()
                    .user(base)
                    .provider(req.getProvider())
                    .providerUserId(req.getProviderUserId())
                    .build());
        }

        // 3) 게스트 머지(테스트 시 guestId를 파라미터로 넘겨서 확인 가능)
        if (optionalGuestIdForMerge != null) {
            var guestOpt = userRepository.findById(optionalGuestIdForMerge);
            if (guestOpt.isPresent()) {
                User guest = guestOpt.get();
                if (guest.isGuest()) {
                    dataMergeService.mergeGuestToUser(guest, base);
                }
            }
        }



        String access = jwtTokenProvider.createToken(base.getId(), "USER");
        return AuthResponse.of(access, base.getId(), "USER");
    }

    @Transactional
    public AuthResponse socialLogin(String guestToken, Provider provider, String providerUserId) {
        String rawToken = guestToken.startsWith("Bearer ")
                ? guestToken.substring(7)
                : guestToken;
        Long guestId = jwtTokenProvider.getUserId(rawToken);
        User guest = userRepository.findById(guestId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GUEST_NOT_FOUND));
        if (!guest.isGuest()) throw new GeneralException(ErrorStatus.ALREADY_MEMBER);

        Optional<SocialAccount> saOpt = socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId);
        if (saOpt.isPresent()) {
            SocialAccount sa = saOpt.get();
            if (!sa.getUser().getId().equals(guest.getId())) {
                sa.changeUser(guest); // ✅ 기존 연결을 게스트로 재지정
                socialAccountRepository.save(sa);
            }
        } else {
            socialAccountRepository.save(SocialAccount.builder()
                    .user(guest).provider(provider).providerUserId(providerUserId).build());
        }
        // 승격
        guest.upgradeToUser();
        userRepository.save(guest);

        String access = jwtTokenProvider.createToken(guest.getId(), "USER");
        return AuthResponse.of(access, guest.getId(), "USER");
    }


}
