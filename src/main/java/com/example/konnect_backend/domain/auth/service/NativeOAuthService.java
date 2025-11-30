package com.example.konnect_backend.domain.auth.service;

import com.example.konnect_backend.domain.auth.dto.response.SocialUserInfo;
import com.example.konnect_backend.domain.user.entity.status.Provider;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 네이티브 앱용 OAuth 서비스
 * - 소셜 플랫폼 API를 직접 호출하여 사용자 정보 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NativeOAuthService {

    private final RestTemplate restTemplate;

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    /**
     * 소셜 액세스 토큰으로 사용자 정보 조회
     */
    public SocialUserInfo getUserInfo(String accessToken, Provider provider) {
        return switch (provider) {
            case KAKAO -> getKakaoUserInfo(accessToken);
            case GOOGLE -> getGoogleUserInfo(accessToken);
            default -> throw new GeneralException(ErrorStatus.UNSUPPORTED_OAUTH_PROVIDER);
        };
    }

    /**
     * 카카오 API 호출하여 사용자 정보 조회
     */
    @SuppressWarnings("unchecked")
    private SocialUserInfo getKakaoUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new GeneralException(ErrorStatus.OAUTH_USER_INFO_FAILED);
            }

            // 카카오 응답 구조:
            // { "id": 1234567890, "kakao_account": { "email": "...", "profile": { "nickname": "..." } } }
            String providerUserId = String.valueOf(body.get("id"));

            String email = null;
            String nickname = null;

            Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");

                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    nickname = (String) profile.get("nickname");
                }
            }

            log.info("카카오 사용자 정보 조회 성공: providerUserId={}", providerUserId);

            return SocialUserInfo.builder()
                    .providerUserId(providerUserId)
                    .email(email)
                    .nickname(nickname)
                    .build();

        } catch (HttpClientErrorException e) {
            log.error("카카오 API 호출 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new GeneralException(ErrorStatus.OAUTH_TOKEN_INVALID);
            }
            throw new GeneralException(ErrorStatus.OAUTH_USER_INFO_FAILED);
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 중 오류 발생", e);
            throw new GeneralException(ErrorStatus.OAUTH_USER_INFO_FAILED);
        }
    }

    /**
     * 구글 API 호출하여 사용자 정보 조회
     */
    private SocialUserInfo getGoogleUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    GOOGLE_USER_INFO_URL,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new GeneralException(ErrorStatus.OAUTH_USER_INFO_FAILED);
            }

            // 구글 응답 구조:
            // { "sub": "1234567890", "email": "...", "name": "...", "given_name": "..." }
            String providerUserId = (String) body.get("sub");
            String email = (String) body.get("email");
            String nickname = (String) body.get("name");
            if (nickname == null) {
                nickname = (String) body.get("given_name");
            }

            log.info("구글 사용자 정보 조회 성공: providerUserId={}", providerUserId);

            return SocialUserInfo.builder()
                    .providerUserId(providerUserId)
                    .email(email)
                    .nickname(nickname)
                    .build();

        } catch (HttpClientErrorException e) {
            log.error("구글 API 호출 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new GeneralException(ErrorStatus.OAUTH_TOKEN_INVALID);
            }
            throw new GeneralException(ErrorStatus.OAUTH_USER_INFO_FAILED);
        } catch (Exception e) {
            log.error("구글 사용자 정보 조회 중 오류 발생", e);
            throw new GeneralException(ErrorStatus.OAUTH_USER_INFO_FAILED);
        }
    }
}
