package com.example.konnect_backend.domain.ai.domain.vo;

import com.example.konnect_backend.domain.ai.dto.internal.ClassificationResult;
import com.example.konnect_backend.domain.ai.dto.internal.ExtractionResult;
import com.example.konnect_backend.domain.ai.dto.response.DifficultExpressionDto;
import com.example.konnect_backend.domain.ai.type.DocumentType;
import com.example.konnect_backend.domain.ai.type.FileType;
import com.example.konnect_backend.domain.ai.type.TargetLanguage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineContext {

    private UUID requestId;

    // 모듈별 실행 결과
    private String extractedText;
    private Integer pageCount;
    private String ocrMethod;

    private ClassificationResult classificationResult;
    private DocumentType documentType;

    private ExtractionResult extractionResult;

    private List<DifficultExpressionDto> difficultExpressions;

    private String simplifiedKorean;

    private String translatedText;

    private String summary;

    // 사용자 요청값
    private UploadFile file;
    private TargetLanguage targetLanguage;

    // 요청의 토큰 사용량, 비동기 호출로 분리를 염두하여 AtomicInteger 사용
    @Builder.Default
    private AtomicInteger inputTokens = new AtomicInteger();
    @Builder.Default
    private AtomicInteger outputTokens = new AtomicInteger();

    // 완료된 단계 추적
    @Builder.Default
    private PipelineStage completedStage = PipelineStage.NONE;

    @Builder.Default
    private List<String> processingLogs = new ArrayList<>();

    public void addLog(String log) {
        if (processingLogs == null) {
            processingLogs = new ArrayList<>();
        }
        processingLogs.add(LocalDateTime.now() + ": " + log);
    }

    public void accTokenUsage(TokenUsage tokenUsage) {
        inputTokens.addAndGet(tokenUsage.inputTokens());
        outputTokens.addAndGet(tokenUsage.outputTokens());
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
