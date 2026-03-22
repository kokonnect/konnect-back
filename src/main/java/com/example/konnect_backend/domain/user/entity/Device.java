package com.example.konnect_backend.domain.user.entity;

import com.example.konnect_backend.domain.user.entity.status.Language;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "device")
public class Device {

    @Id
    private String deviceUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private Language language;    // 선택

    private LocalDateTime createdAt;

    private LocalDateTime lastUsedAt;

    public void updateUser(User user){
        this.user = user;
    }

    public void updateLanguage(Language language) {
        this.language = language;
    }
}