package com.example.konnect_backend.domain.ai.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "ai_model"
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name="use_case")
    private String useCase;
}
