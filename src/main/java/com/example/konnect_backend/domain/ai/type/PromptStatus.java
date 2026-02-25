package com.example.konnect_backend.domain.ai.type;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum PromptStatus {
    DRAFT("생성 후 적용 전"),
    ACTIVE("현재 활성화"),
    DEPRECATED("이전 사용");

    private String description;
}
