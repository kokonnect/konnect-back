package com.example.konnect_backend.global.security.oauth;

import com.example.konnect_backend.domain.user.entity.SocialAccount;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.entity.status.Provider;
import com.example.konnect_backend.domain.user.repository.SocialAccountRepository;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.domain.user.service.DeviceService;
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

        // 3. 기본값 처리
        String email = (p.email() != null && !p.email().isBlank())
                ? p.email()
                : "test_" + p.providerUserId() + "@example.com";
        String displayName = (p.displayName() != null && !p.displayName().isBlank())
                ? p.displayName()
                : "User-" + p.providerUserId();

        User user = findOrCreateUser(provider, p.providerUserId(), email, displayName);

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

    @Transactional
    protected User findOrCreateUser(Provider provider,
                                    String providerUserId,
                                    String email,
                                    String name) {

        Optional<SocialAccount> saOpt =
                socialRepo.findByProviderAndProviderUserId(provider, providerUserId);

        if (saOpt.isPresent()) {
            return saOpt.get().getUser();
        }

        User newUser = User.builder()
                .email(email)
                .name(name)
                .guest(false)
                .build();

        newUser = userRepository.save(newUser);

        socialRepo.save(
                SocialAccount.builder()
                        .user(newUser)
                        .provider(provider)
                        .providerUserId(providerUserId)
                        .build()
        );

        return newUser;
    }

    /** 소셜 프로필 DTO */
    private record SocialProfile(String providerUserId, String email, String displayName) {}
}

/*

OAuth profile 읽기
↓
User 생성 / 조회
↓
OAuth2User 반환

*/