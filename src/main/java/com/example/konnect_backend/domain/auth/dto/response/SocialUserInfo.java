package com.example.konnect_backend.domain.auth.dto.response;

import lombok.*;

/**
 * 소셜 플랫폼에서 가져온 사용자 정보
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialUserInfo {

    /**
     * 소셜 플랫폼 사용자 고유 ID
     * - 카카오: id (Long → String 변환)
     * - 구글: sub
     */
    private String providerUserId;

    /**
     * 이메일 (선택, 소셜 플랫폼 설정에 따라 null일 수 있음)
     */
    private String email;

    /**
     * 닉네임/이름
     */
    private String nickname;
}
