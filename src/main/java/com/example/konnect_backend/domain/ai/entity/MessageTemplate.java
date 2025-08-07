package com.example.konnect_backend.domain.ai.entity;

import com.example.konnect_backend.domain.ai.entity.status.ScenarioCategory;
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
public class MessageTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageTemplateId;

    private String title;

    @Enumerated(EnumType.STRING)
    private ScenarioCategory scenarioCategory;

    private String koreanContent;
    private String content; // 외국어 내용
}
