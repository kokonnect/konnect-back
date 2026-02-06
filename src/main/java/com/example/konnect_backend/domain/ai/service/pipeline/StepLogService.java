package com.example.konnect_backend.domain.ai.service.pipeline;

import com.example.konnect_backend.domain.ai.dto.internal.ClassificationResult;
import com.example.konnect_backend.domain.ai.util.PromptUtils;
import com.example.konnect_backend.domain.document.entity.AnalysisStepLog;
import com.example.konnect_backend.domain.document.entity.DocumentAnalysis;
import com.example.konnect_backend.domain.document.repository.AnalysisStepLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StepLogService {

    private final AnalysisStepLogRepository stepLogRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_INPUT_TEXT_LENGTH = 3000;
    private static final int MAX_RESPONSE_LENGTH = 10000;

    /**
     * 성공한 단계 로그 저장
     */
    @Transactional
    public AnalysisStepLog logSuccess(DocumentAnalysis analysis, StepInfo stepInfo, String rawResponse,
                                       String parsedResult, String outputSummary, long processingTimeMs) {
        AnalysisStepLog stepLog = AnalysisStepLog.successBuilder()
                .documentAnalysis(analysis)
                .stepName(stepInfo.getStepName())
                .stepOrder(stepInfo.getStepOrder())
                .inputText(PromptUtils.truncateText(stepInfo.getInputText(), MAX_INPUT_TEXT_LENGTH))
                .inputLength(PromptUtils.getOriginalLength(stepInfo.getInputText()))
                .promptTemplate(stepInfo.getPromptTemplate())
                .modelUsed(stepInfo.getModelUsed())
                .temperature(stepInfo.getTemperature())
                .maxTokens(stepInfo.getMaxTokens())
                .rawResponse(PromptUtils.truncateText(rawResponse, MAX_RESPONSE_LENGTH))
                .parsedResult(parsedResult)
                .outputSummary(outputSummary)
                .processingTimeMs(processingTimeMs)
                .build();

        return stepLogRepository.save(stepLog);
    }

    /**
     * 분류 단계 성공 로그 저장 (분류 관련 상세 정보 포함)
     */
    @Transactional
    public AnalysisStepLog logClassificationSuccess(DocumentAnalysis analysis, StepInfo stepInfo,
                                                     String rawResponse, ClassificationResult result,
                                                     long processingTimeMs) {
        String parsedResult;
        try {
            parsedResult = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            parsedResult = "{}";
        }

        String keywordsStr = result.getKeywords() != null ? String.join(",", result.getKeywords()) : "";

        AnalysisStepLog stepLog = AnalysisStepLog.successBuilder()
                .documentAnalysis(analysis)
                .stepName(stepInfo.getStepName())
                .stepOrder(stepInfo.getStepOrder())
                .inputText(PromptUtils.truncateText(stepInfo.getInputText(), MAX_INPUT_TEXT_LENGTH))
                .inputLength(PromptUtils.getOriginalLength(stepInfo.getInputText()))
                .promptTemplate(stepInfo.getPromptTemplate())
                .modelUsed(stepInfo.getModelUsed())
                .temperature(stepInfo.getTemperature())
                .maxTokens(stepInfo.getMaxTokens())
                .rawResponse(PromptUtils.truncateText(rawResponse, MAX_RESPONSE_LENGTH))
                .parsedResult(parsedResult)
                .outputSummary(String.format("%s 분류, 신뢰도 %.2f", result.getDocumentType().getDisplayName(), result.getConfidence()))
                .classificationType(result.getDocumentType().name())
                .classificationConfidence(result.getConfidence())
                .classificationKeywords(keywordsStr)
                .classificationReasoning(result.getReasoning())
                .processingTimeMs(processingTimeMs)
                .build();

        return stepLogRepository.save(stepLog);
    }

    /**
     * 실패한 단계 로그 저장
     */
    @Transactional
    public AnalysisStepLog logFailure(DocumentAnalysis analysis, StepInfo stepInfo,
                                       String errorMessage, long processingTimeMs) {
        AnalysisStepLog stepLog = AnalysisStepLog.failedBuilder()
                .documentAnalysis(analysis)
                .stepName(stepInfo.getStepName())
                .stepOrder(stepInfo.getStepOrder())
                .inputText(PromptUtils.truncateText(stepInfo.getInputText(), MAX_INPUT_TEXT_LENGTH))
                .inputLength(PromptUtils.getOriginalLength(stepInfo.getInputText()))
                .promptTemplate(stepInfo.getPromptTemplate())
                .modelUsed(stepInfo.getModelUsed())
                .temperature(stepInfo.getTemperature())
                .maxTokens(stepInfo.getMaxTokens())
                .errorMessage(errorMessage)
                .processingTimeMs(processingTimeMs)
                .build();

        return stepLogRepository.save(stepLog);
    }

    /**
     * 스킵된 단계 로그 저장
     */
    @Transactional
    public AnalysisStepLog logSkipped(DocumentAnalysis analysis, String stepName, int stepOrder, String reason) {
        AnalysisStepLog stepLog = AnalysisStepLog.skippedBuilder()
                .documentAnalysis(analysis)
                .stepName(stepName)
                .stepOrder(stepOrder)
                .outputSummary(reason)
                .processingTimeMs(0L)
                .build();

        return stepLogRepository.save(stepLog);
    }

    /**
     * 단계 정보를 담는 내부 클래스
     */
    @lombok.Data
    @lombok.Builder
    public static class StepInfo {
        private String stepName;
        private int stepOrder;
        private String inputText;
        private String promptTemplate;
        private String modelUsed;
        private Double temperature;
        private Integer maxTokens;
    }
}
