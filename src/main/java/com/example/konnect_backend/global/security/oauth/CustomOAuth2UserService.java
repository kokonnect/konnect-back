package com.example.konnect_backend.global.security.oauth;

import com.example.konnect_backend.domain.user.entity.SocialAccount;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.entity.status.Provider;
import com.example.konnect_backend.domain.user.repository.SocialAccountRepository;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialRepo;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        OAuth2User oauth = super.loadUser(req);
        Provider provider = Provider.valueOf(req.getClientRegistration().getRegistrationId().toUpperCase());

        // 1. 프로바이더별 프로필 추출
        SocialProfile p = extractProfile(provider, oauth.getAttributes());

        // 2. 게스트 토큰 여부 확인
        Long guestUserId = resolveGuestUserIdFromCookieOptional();

        // 3. 기본값 처리
        String email = (p.email() != null && !p.email().isBlank())
                ? p.email()
                : "test_" + p.providerUserId() + "@example.com";
        String displayName = (p.displayName() != null && !p.displayName().isBlank())
                ? p.displayName()
                : "User-" + p.providerUserId();

        // 4. 유저 생성 or 업데이트
        User user;
        if (guestUserId != null) {
            user = upsertToGuest(provider, p.providerUserId(), email, displayName, guestUserId);
        } else {
            user = createNewUserWithSocial(provider, p.providerUserId(), email, displayName);
        }

        // 5. ID null 방지 체크
        if (user.getId() == null) {
            throw new IllegalStateException("User ID is null after save. Check persistence logic.");
        }

        // 6. SecurityContext로 반환할 OAuth2User
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "userId", user.getId(),                  // Long (서비스 내부 PK)
                        "providerUserId", p.providerUserId()     // String (소셜 원본 ID)
                ),
                "userId"
        );
    }

    /** 프로바이더별 데이터 파싱 */
    @SuppressWarnings("unchecked")
    private SocialProfile extractProfile(Provider provider, Map<String, Object> a) {
        String providerUserId;
        String email = null;
        String displayName = null;

        if (provider == Provider.GOOGLE) {
            providerUserId = (String) a.get("sub");
            email = (String) a.get("email");
            displayName = (String) a.getOrDefault("name", a.get("given_name"));
        } else if (provider == Provider.KAKAO) {
            providerUserId = String.valueOf(a.get("id"));
            Map<String, Object> account = (Map<String, Object>) a.get("kakao_account");
            if (account != null) {
                email = (String) account.get("email");
                Map<String, Object> profile = (Map<String, Object>) account.get("profile");
                if (profile != null) displayName = (String) profile.get("nickname");
            }
        } else {
            throw new OAuth2AuthenticationException("Unsupported provider");
        }

        return new SocialProfile(providerUserId, email, displayName);
    }

    /** 게스트 토큰 쿠키에서 ID 추출 (없으면 null) */
    private Long resolveGuestUserIdFromCookieOptional() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        HttpServletRequest req = attrs.getRequest();
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;

        for (Cookie c : cookies) {
            if ("guestToken".equals(c.getName())) {
                try {
                    return jwtTokenProvider.getUserId(c.getValue());
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    /** 기존 게스트에 소셜 연결 */
    @Transactional
    protected User upsertToGuest(Provider provider, String providerUserId, String email, String name, Long guestUserId) {
        User guest = userRepository.findById(guestUserId)
                .orElseThrow(() -> new OAuth2AuthenticationException("GUEST_NOT_FOUND"));

        Optional<SocialAccount> saOpt = socialRepo.findByProviderAndProviderUserId(provider, providerUserId);
        if (saOpt.isPresent()) {
            SocialAccount sa = saOpt.get();
            if (!sa.getUser().getId().equals(guest.getId())) {
                sa.setUser(guest);
                socialRepo.save(sa);
            }
        } else {
            socialRepo.save(SocialAccount.builder()
                    .user(guest)
                    .provider(provider)
                    .providerUserId(providerUserId)
                    .build());
        }

        promoteAndFill(guest, email, name);
        return userRepository.save(guest);
    }

    /** 새 유저 + 소셜 계정 생성 */
    @Transactional
    protected User createNewUserWithSocial(Provider provider, String providerUserId, String email, String name) {
        User newUser = User.builder()
                .guest(false)
                .email(email)
                .name(name)
                .build();
        newUser = userRepository.save(newUser); // 저장된 엔티티 반환

        socialRepo.save(SocialAccount.builder()
                .user(newUser)
                .provider(provider)
                .providerUserId(providerUserId)
                .build());

        return newUser;
    }

    /** 기본 정보 채움 */
    private void promoteAndFill(User u, String email, String name) {
        if (u.isGuest()) u.setGuest(false);
        if (u.getEmail() == null || u.getEmail().isBlank()) {
            u.setEmail(email != null && !email.isBlank() ? email : "test_" + u.getId() + "@example.com");
        }
        if (u.getName() == null || u.getName().isBlank()) {
            u.setName(name != null && !name.isBlank() ? name : "User-" + u.getId());
        }
    }

    /** 소셜 프로필 DTO */
    private record SocialProfile(String providerUserId, String email, String displayName) {}
}
