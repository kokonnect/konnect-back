package com.example.konnect_backend.domain.user.entity;

import com.example.konnect_backend.domain.user.entity.status.Provider;
import com.example.konnect_backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "social_account",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "providerUserId"}))
public class SocialAccount extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;       // GOOGLE, KAKAO

    @Column(nullable = false)
    private String providerUserId;   // Google=sub, Kakao=id

    public void changeUser(User newUser) {
        this.user = newUser;
    }

}
