package com.example.konnect_backend.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentType {
    SCHEDULE("일정 안내", "학교 행사, 시험, 방학 등 일정 관련 문서"),
    PENALTY("벌점/패널티", "교칙 위반, 벌점, 징계 관련 문서"),
    EVENT("행사 진행", "학교 행사, 체험학습, 소풍 등 행사 관련 문서"),
    NOTICE("일반 공지", "기타 일반적인 공지사항");

    private final String displayName;
    private final String description;
}
