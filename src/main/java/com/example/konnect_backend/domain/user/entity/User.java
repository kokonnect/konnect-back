package com.example.konnect_backend.domain.user.entity;

import com.example.konnect_backend.domain.user.entity.status.Provider;
import com.example.konnect_backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String socialId;

    @Enumerated(EnumType.STRING)
    private Provider provider; // ENUM: GOOGLE, KAKAO, NAVER

    private String nickname;

    private LocalDateTime registeredAt;
}
