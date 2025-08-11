// src/main/java/com/example/konnect_backend/domain/user/entity/User.java
package com.example.konnect_backend.domain.user.entity;

import com.example.konnect_backend.domain.user.entity.status.Language;
import com.example.konnect_backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.time.LocalDateTime;

@Entity
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "user")
public class User extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true) // NULL 중복 허용(MySQL)
    private String email;

    private String name;          // 선택
    @Enumerated(EnumType.STRING)
    private Language language;    // 선택

    @Builder.Default
    private boolean guest = false; // 게스트 쓰면 true로

    public void upgradeToMember(String name,String email, Language language) {
        this.name = name;
        this.email = email;
        this.language = language;
        this.guest = false;
    }
    
    public void upgradeToUser() {
        this.guest = false;
    }
    
    public void updateEmail(String email) {
        this.email = email;
    }
    
    public void updateName(String name) {
        this.name = name;
    }
    
    public void updateProfile(String email, String name) {
        if (email != null && !email.isBlank()) {
            this.email = email;
        }
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }
}
