package com.example.konnect_backend.domain.ai.service.pipeline;

import com.example.konnect_backend.domain.ai.dto.internal.ExtractionResult;
import com.example.konnect_backend.domain.ai.dto.internal.ClassificationResult;
import com.example.konnect_backend.domain.ai.dto.response.DifficultExpressionDto;
import com.example.konnect_backend.domain.ai.type.DocumentType;
import com.example.konnect_backend.domain.ai.type.TargetLanguage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineContext {

    private String originalText;

    private String simplifiedKorean;

    private List<DifficultExpressionDto> difficultExpressions;

    private String translatedText;

    private String summary;

    private DocumentType documentType;

    private TargetLanguage targetLanguage;

    private String ocrMethod;

    private Integer pageCount;

    // 중간 결과 저장 (단계별 복구용)
    private ClassificationResult classificationResult;

    private ExtractionResult extractionResult;

    // 완료된 단계 추적
    @Builder.Default
    private PipelineStage completedStage = PipelineStage.NONE;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Builder.Default
    private List<String> processingLogs = new ArrayList<>();

    public void addLog(String log) {
        if (processingLogs == null) {
            processingLogs = new ArrayList<>();
        }
        processingLogs.add(LocalDateTime.now() + ": " + log);
    }

    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    // 파이프라인 단계 enum
    public enum PipelineStage {
        NONE,
        TEXT_EXTRACTED,
        CLASSIFIED,
        EXTRACTED,
        DIFFICULT_EXPRESSIONS_EXTRACTED,
        SIMPLIFIED,
        TRANSLATED,
        SUMMARIZED,
        COMPLETED
    }
}
