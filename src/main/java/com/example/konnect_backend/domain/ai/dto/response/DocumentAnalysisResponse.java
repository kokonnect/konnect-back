package com.example.konnect_backend.domain.ai.dto.response;

import com.example.konnect_backend.domain.ai.dto.DocumentType;
import com.example.konnect_backend.domain.ai.dto.FileType;
import com.example.konnect_backend.domain.ai.dto.TargetLanguage;
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

    // 분석 식별자 (재시도용)
    private Long analysisId;

    // 처리 상태
    private ProcessingStatus status;
    private String failedStage;        // 실패한 단계 (실패 시에만)
    private String errorMessage;       // 에러 메시지 (실패 시에만)

    // 텍스트 정보
    private String extractedText;           // 원본 추출 텍스트 (OCR 결과)
    private String simplifiedKorean;        // 쉬운 한국어로 재작성된 텍스트
    private String translatedText;          // 번역된 텍스트 (쉬운 한국어 기반)
    private String summary;                 // 요약

    // 어려운 표현 풀이
    private List<DifficultExpressionDto> difficultExpressions;

    // 분류 정보
    private DocumentType documentType;
    private String documentTypeName;
    private Double classificationConfidence;
    private List<String> classificationKeywords;
    private String classificationReasoning;

    // 추출된 일정
    private List<ExtractedScheduleDto> extractedSchedules;

    // 유형별 추가 정보
    private Map<String, Object> extractedInfo;

    // 파일 메타데이터
    private String originalFileName;
    private FileType fileType;
    private TargetLanguage targetLanguage;
    private String targetLanguageName;
    private Long fileSize;
    private Integer pageCount;

    // 처리 정보
    private Long processingTimeMs;
    private String ocrMethod;

    // 처리 상태 enum
    public enum ProcessingStatus {
        COMPLETED,      // 모든 단계 완료
        PARTIAL,        // 일부 단계만 완료 (재시도 가능)
        FAILED          // 실패
    }
}
