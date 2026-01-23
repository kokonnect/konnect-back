package com.example.konnect_backend.domain.ai.dto.response;

import com.example.konnect_backend.domain.ai.type.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationResult {

    private DocumentType documentType;

    private Double confidence;

    private List<String> keywords;

    private String reasoning;

    public static ClassificationResult defaultNotice() {
        return ClassificationResult.builder()
                .documentType(DocumentType.NOTICE)
                .confidence(0.5)
                .keywords(List.of())
                .reasoning("기본값: 분류 실패 시 일반 공지로 처리")
                .build();
    }
}
