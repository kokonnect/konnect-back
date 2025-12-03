package com.example.konnect_backend.domain.notification.entity;

import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "fcm_token", indexes = {
        @Index(name = "idx_fcm_token_user", columnList = "user_id")
})
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    // 디바이스 식별자 (동일 디바이스에서 토큰 갱신 시 사용)
    private String deviceId;

    // 디바이스 타입 (iOS, Android 등)
    private String deviceType;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    public void updateToken(String newToken) {
        this.token = newToken;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
}
