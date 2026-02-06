package com.example.konnect_backend.domain.ai.dto.response;

import com.example.konnect_backend.domain.ai.type.DocumentType;
import com.example.konnect_backend.domain.ai.type.FileType;
import com.example.konnect_backend.domain.ai.type.ProcessingStatus;
import com.example.konnect_backend.domain.ai.type.TargetLanguage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAnalysisResponse {

    private Long analysisId;

    // 텍스트 정보
    private String extractedText;           // 원본 추출 텍스트 (OCR 결과)
    private String translatedText;          // 번역된 텍스트 (쉬운 한국어 기반)
    private String summary;                 // 요약

    // 어려운 표현 풀이
    private List<DifficultExpressionDto> difficultExpressions;

    // 추출된 일정
    private List<ExtractedScheduleDto> extractedSchedules;

    // 파일 메타데이터
    private String originalFileName;
}
