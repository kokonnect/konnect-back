package com.example.konnect_backend.domain.message.entity;

import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserGeneratedMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    private String inputPrompt;

    @Column(columnDefinition = "TEXT")
    private String generatedKorean;

    @Column(name = "device_uuid")
    private String deviceUuid;

    @Builder
    public UserGeneratedMessage(
            User user,
            String deviceUuid,
            String inputPrompt,
            String generatedKorean
    ) {
        this.user = user;
        this.deviceUuid = deviceUuid;
        this.inputPrompt = inputPrompt;
        this.generatedKorean = generatedKorean;
    }
}
