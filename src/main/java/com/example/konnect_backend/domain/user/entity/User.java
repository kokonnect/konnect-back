// src/main/java/com/example/konnect_backend/domain/user/entity/User.java
package com.example.konnect_backend.domain.user.entity;

import com.example.konnect_backend.domain.user.entity.status.Language;
import com.example.konnect_backend.domain.user.entity.status.Provider;
import com.example.konnect_backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter // 승격(update) 편의용. 싫으면 update 메서드 작성
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 게스트에선 null 허용. UNIQUE + NULL은 중복 허용(MySQL)
    @Column(unique = true)
    private String socialId;

    @Column
    private String name;

    @Enumerated(EnumType.STRING)
    private Provider provider; // 게스트면 null

    @Temporal(TemporalType.DATE)
    private Date birthDate;

    @Enumerated(EnumType.STRING)
    private Language language;

    // 승격 플래그
    @Column(nullable = false)
    @Builder.Default
    private boolean guest = true;

    // User 엔티티 내부
    public void upgradeToMember(String socialId, String name, Provider provider, Date birthDate, Language language) {
        this.socialId = socialId;
        this.name = name;
        this.provider = provider;
        this.birthDate = birthDate;
        this.language = language;
        this.guest = false;
    }

}
