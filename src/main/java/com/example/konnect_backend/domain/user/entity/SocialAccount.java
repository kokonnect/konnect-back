package com.example.konnect_backend.domain.user.entity;

import com.example.konnect_backend.domain.user.entity.status.Provider;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "social_account",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "providerUserId"}))
public class SocialAccount {
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

}
