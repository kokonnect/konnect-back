package com.example.konnect_backend.domain.ai.service.pipeline;

import com.example.konnect_backend.domain.ai.dto.internal.ClassificationResult;
import com.example.konnect_backend.domain.ai.dto.internal.ExtractionResult;
import com.example.konnect_backend.domain.ai.dto.internal.TextExtractionResult;
import com.example.konnect_backend.domain.ai.dto.response.DifficultExpressionDto;
import com.example.konnect_backend.domain.ai.dto.response.DocumentAnalysisResponse;
import com.example.konnect_backend.domain.ai.exception.DocumentAnalysisException;
import com.example.konnect_backend.domain.ai.exception.TextExtractionException;
import com.example.konnect_backend.domain.ai.service.GeminiService;
import com.example.konnect_backend.domain.ai.service.extractor.ImageTextExtractor;
import com.example.konnect_backend.domain.ai.service.extractor.PdfTextExtractor;
import com.example.konnect_backend.domain.ai.service.model.UploadFile;
import com.example.konnect_backend.domain.ai.service.prompt.*;
import com.example.konnect_backend.domain.ai.type.FileType;
import com.example.konnect_backend.domain.ai.type.TargetLanguage;
import com.example.konnect_backend.domain.document.entity.Document;
import com.example.konnect_backend.domain.document.entity.DocumentAnalysis;
import com.example.konnect_backend.domain.document.entity.DocumentFile;
import com.example.konnect_backend.domain.document.entity.DocumentTranslation;
import com.example.konnect_backend.domain.document.repository.DocumentAnalysisRepository;
import com.example.konnect_backend.domain.document.repository.DocumentRepository;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.entity.status.Language;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAnalysisPipeline {

    private final ImageTextExtractor imageTextExtractor;
    private final PdfTextExtractor pdfTextExtractor;
    private final DocumentClassifierModule classifierModule;
    private final UnifiedExtractorModule unifiedExtractorModule;
    private final DifficultExpressionExtractorModule difficultExpressionExtractorModule;
    private final KoreanSimplifierModule koreanSimplifierModule;
    private final TranslatorModule translatorModule;
    private final SummarizerModule summarizerModule;

    private final DocumentRepository documentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final StepLogService stepLogService;
    private final GeminiService geminiService;

    private final IdGenerator idGenerator;

    private static final int TOTAL_STEPS = 7;

    @Transactional
    public DocumentAnalysisResponse analyze(UploadFile file, FileType fileType, Long requesterId) {
        Long analysisId = idGenerator.newId();
        User user = getUser(requesterId);
        TargetLanguage targetLanguage = getTargetLanguage(user);

        PipelineContext context = PipelineContext.builder().targetLanguage(targetLanguage)
            .completedStage(PipelineContext.PipelineStage.NONE).metadata(new HashMap<>())
            .processingLogs(new ArrayList<>()).build();

        context.addMetadata("useSimpleLanguage", true);
        context.addMetadata("analysisId", analysisId);
        context.addMetadata("fileName", file.originalName());
        context.addMetadata("fileType", fileType.name());

        return executePipeline(analysisId, file, fileType, user, context);
    }

    private DocumentAnalysisResponse executePipeline(Long analysisId, UploadFile file,
                                                     FileType fileType, User user,
                                                     PipelineContext context) {
        long startTime = System.currentTimeMillis();
        String currentStage = "INIT";
        DocumentAnalysis savedAnalysis;

        // 파이프라인 시작 시 토큰 사용량 초기화
        geminiService.resetSessionTokenUsage();

        try {
            log.info("문서 분석 파이프라인 시작: analysisId={}, 파일={}, 타입={}, 언어={}", analysisId,
                file.originalName(), fileType, context.getTargetLanguage().getDisplayName());

            // 2. 텍스트 추출 (OCR) - Step 1
            currentStage = "TEXT_EXTRACTION";
            String extractedText = executeTextExtraction(file, fileType, context);

            // 3. 문서 유형 분류 - Step 2
            currentStage = "CLASSIFICATION";
            ClassificationResult classification = executeClassification(extractedText, context);

            // 4. 통합 정보 추출 - Step 3
            currentStage = "EXTRACTION";
            ExtractionResult extraction = executeExtraction(extractedText, context);

            // 5. 어려운 표현 추출 및 풀이 - Step 4
            currentStage = "DIFFICULT_EXPRESSIONS";
            List<DifficultExpressionDto> difficultExpressions = executeDifficultExpressionExtraction(
                extractedText, context);

            // 6. 쉬운 한국어로 재작성 - Step 5
            currentStage = "SIMPLIFICATION";
            String simplifiedKorean = executeSimplification(extractedText, context);

            // 7. 번역 (쉬운 한국어 기반) - Step 6
            currentStage = "TRANSLATION";
            String translatedText = executeTranslation(simplifiedKorean, context);

            // 8. 요약 (쉬운 한국어 기반) - Step 7
            currentStage = "SUMMARIZATION";
            String summary = executeSummarization(simplifiedKorean, context);

            // 9. DB 저장
            currentStage = "SAVE";
            savedAnalysis = saveAnalysisResult(file, fileType, user, context, classification,
                extraction, extractedText, translatedText, summary);

            // 10. 단계별 로그 저장
            if (savedAnalysis != null) {
                saveStepLogs(savedAnalysis, context, extractedText, classification, extraction,
                    difficultExpressions, simplifiedKorean, translatedText, summary);
            }

            context.setCompletedStage(PipelineContext.PipelineStage.COMPLETED);
            long processingTime = System.currentTimeMillis() - startTime;

            // 파이프라인 완료 시 총 토큰 사용량 로깅
            logTotalTokenUsage(analysisId, processingTime);

            // 11. 성공 응답 생성
            return buildSuccessResponse(analysisId, file, extraction, extractedText,
                difficultExpressions, translatedText, summary);
        } catch (Exception e) {
            log.error("문서 분석 파이프라인 실패: analysisId={}, stage={}", analysisId, currentStage, e);
            throw e;
        }
    }

    /**
     * 단계별 로그 저장
     */
    private void saveStepLogs(DocumentAnalysis analysis, PipelineContext context,
                              String extractedText, ClassificationResult classification,
                              ExtractionResult extraction,
                              List<DifficultExpressionDto> difficultExpressions,
                              String simplifiedKorean, String translatedText, String summary) {
        try {
            int stepOrder = 1;

            // 1. TEXT_EXTRACTION 로그
            stepLogService.logSuccess(analysis,
                StepLogService.StepInfo.builder().stepName("TEXT_EXTRACTION").stepOrder(stepOrder++)
                    .promptTemplate("OCR").modelUsed(context.getOcrMethod()).build(), null, null,
                String.format("텍스트 추출 완료: %d자", extractedText.length()), 0L);

            // 2. CLASSIFICATION 로그
            stepLogService.logClassificationSuccess(analysis,
                StepLogService.StepInfo.builder().stepName("CLASSIFICATION").stepOrder(stepOrder++)
                    .inputText(extractedText)
                    .promptTemplate(DocumentClassifierModule.PROMPT_TEMPLATE_NAME)
                    .modelUsed(DocumentClassifierModule.MODEL_NAME)
                    .temperature(DocumentClassifierModule.TEMPERATURE)
                    .maxTokens(DocumentClassifierModule.MAX_TOKENS).build(),
                classifierModule.getLastRawResponse(), classification,
                classifierModule.getLastProcessingTimeMs());

            // 3. EXTRACTION 로그
            String extractionJson = objectMapper.writeValueAsString(extraction);
            stepLogService.logSuccess(analysis,
                StepLogService.StepInfo.builder().stepName("EXTRACTION").stepOrder(stepOrder++)
                    .inputText(extractedText).promptTemplate("UNIFIED_EXTRACTION_PROMPT")
                    .modelUsed(UnifiedExtractorModule.MODEL_NAME)
                    .temperature(UnifiedExtractorModule.TEMPERATURE)
                    .maxTokens(UnifiedExtractorModule.MAX_TOKENS).build(),
                unifiedExtractorModule.getLastRawResponse(), extractionJson,
                String.format("추출 완료: %d개 일정", extraction.getSchedules().size()),
                unifiedExtractorModule.getLastProcessingTimeMs());

            // 4. DIFFICULT_EXPRESSIONS 로그
            String difficultJson = objectMapper.writeValueAsString(difficultExpressions);
            stepLogService.logSuccess(analysis,
                StepLogService.StepInfo.builder().stepName("DIFFICULT_EXPRESSIONS")
                    .stepOrder(stepOrder++).inputText(extractedText)
                    .promptTemplate(DifficultExpressionExtractorModule.PROMPT_TEMPLATE_NAME)
                    .modelUsed(DifficultExpressionExtractorModule.MODEL_NAME)
                    .temperature(DifficultExpressionExtractorModule.TEMPERATURE)
                    .maxTokens(DifficultExpressionExtractorModule.MAX_TOKENS).build(),
                difficultExpressionExtractorModule.getLastRawResponse(), difficultJson,
                String.format("어려운 표현 %d개 추출", difficultExpressions.size()),
                difficultExpressionExtractorModule.getLastProcessingTimeMs());

            // 5. SIMPLIFICATION 로그
            stepLogService.logSuccess(analysis,
                StepLogService.StepInfo.builder().stepName("SIMPLIFICATION").stepOrder(stepOrder++)
                    .inputText(extractedText)
                    .promptTemplate(KoreanSimplifierModule.PROMPT_TEMPLATE_NAME)
                    .modelUsed(KoreanSimplifierModule.MODEL_NAME)
                    .temperature(KoreanSimplifierModule.TEMPERATURE)
                    .maxTokens(KoreanSimplifierModule.MAX_TOKENS).build(),
                koreanSimplifierModule.getLastRawResponse(), simplifiedKorean,
                String.format("쉬운 한국어 %d자", simplifiedKorean.length()),
                koreanSimplifierModule.getLastProcessingTimeMs());

            // 6. TRANSLATION 로그
            stepLogService.logSuccess(analysis,
                StepLogService.StepInfo.builder().stepName("TRANSLATION").stepOrder(stepOrder++)
                    .inputText(simplifiedKorean)
                    .promptTemplate(TranslatorModule.PROMPT_TEMPLATE_NAME)
                    .modelUsed(TranslatorModule.MODEL_NAME)
                    .temperature(TranslatorModule.TEMPERATURE)
                    .maxTokens(TranslatorModule.MAX_TOKENS).build(),
                translatorModule.getLastRawResponse(), translatedText,
                String.format("번역 완료: %d자 -> %s", translatedText.length(),
                    context.getTargetLanguage().getDisplayName()),
                translatorModule.getLastProcessingTimeMs());

            // 7. SUMMARIZATION 로그
            stepLogService.logSuccess(analysis,
                StepLogService.StepInfo.builder().stepName("SUMMARIZATION").stepOrder(stepOrder)
                    .inputText(simplifiedKorean)
                    .promptTemplate(SummarizerModule.PROMPT_TEMPLATE_NAME)
                    .modelUsed(SummarizerModule.MODEL_NAME)
                    .temperature(SummarizerModule.TEMPERATURE)
                    .maxTokens(SummarizerModule.MAX_TOKENS).build(),
                summarizerModule.getLastRawResponse(), summary,
                String.format("요약 완료: %d자", summary.length()),
                summarizerModule.getLastProcessingTimeMs());

            // 분석 완료 카운트 업데이트
            analysis.updateStepCounts(TOTAL_STEPS, TOTAL_STEPS);

            log.info("단계별 로그 저장 완료: analysisId={}", analysis.getId());

        } catch (Exception e) {
            log.error("단계별 로그 저장 실패", e);
        }
    }

    private User getUser(Long userId) {
        User user;
        // 미인증 사용자 (테스트용)
        if (userId == null) {
            user = null;
        }
        // UserService가 현재 인증 정보를 직접 꺼내써서 userId로 직접 접근하는 게 의미 상 명확
        user = userRepository.findById(userId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        return user;
    }

    private TargetLanguage getTargetLanguage(User user) {
        Language targetLanguage = user.getLanguage();
        if (targetLanguage == null) {
            return TargetLanguage.KOREAN;
        }

        return TargetLanguage.fromLanguage(user.getLanguage());
    }

    /**
     * 파이프라인 완료 시 총 토큰 사용량 로깅
     */
    private void logTotalTokenUsage(Long analysisId, long processingTime) {
        try {
            GeminiService.SessionTokenUsage tokenUsage = geminiService.getSessionTokenUsage();
            double processingSeconds = processingTime / 1000.0;

            log.info("═══════════════════════════════════════════════════════════════");
            log.info("📊 파이프라인 완료 - 토큰 사용량 요약");
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("   분석 ID: {}", analysisId);
            log.info("   처리 시간: {}ms ({}초)", processingTime,
                String.format("%.1f", processingSeconds));
            log.info("───────────────────────────────────────────────────────────────");
            log.info("   입력 토큰 (Input):  {}", String.format("%,d", tokenUsage.inputTokens()));
            log.info("   출력 토큰 (Output): {}", String.format("%,d", tokenUsage.outputTokens()));
            log.info("   총 토큰 (Total):    {}", String.format("%,d", tokenUsage.totalTokens()));
            log.info("═══════════════════════════════════════════════════════════════");
        } catch (Exception e) {
            log.debug("토큰 사용량 로깅 실패 (무시): {}", e.getMessage());
        }
    }

    // 단계별 실행 메서드 (기존과 동일하지만 통합 Extractor 사용)
    private String executeTextExtraction(UploadFile file, FileType fileType,
                                         PipelineContext context) {
        TextExtractionResult extractionResult = extractText(file, fileType);
        String extractedText = extractionResult.getText();
        context.setOriginalText(extractedText);
        context.setOcrMethod(extractionResult.getOcrMethod());
        context.setPageCount(extractionResult.getPageCount());
        context.setCompletedStage(PipelineContext.PipelineStage.TEXT_EXTRACTED);
        return extractedText;
    }

    private ClassificationResult executeClassification(String extractedText,
                                                       PipelineContext context) {
        ClassificationResult classification = classifierModule.process(extractedText, context);
        context.setClassificationResult(classification);
        context.setDocumentType(classification.getDocumentType());
        context.setCompletedStage(PipelineContext.PipelineStage.CLASSIFIED);
        return classification;
    }

    private ExtractionResult executeExtraction(String extractedText, PipelineContext context) {
        // 통합 Extractor 사용 (문서 유형과 무관하게 모든 정보 추출 시도)
        ExtractionResult extraction = unifiedExtractorModule.process(extractedText, context);
        context.setExtractionResult(extraction);
        context.setCompletedStage(PipelineContext.PipelineStage.EXTRACTED);
        return extraction;
    }

    private List<DifficultExpressionDto> executeDifficultExpressionExtraction(String extractedText,
                                                                              PipelineContext context) {
        List<DifficultExpressionDto> expressions = difficultExpressionExtractorModule.process(
            extractedText, context);
        context.setDifficultExpressions(expressions);
        context.setCompletedStage(PipelineContext.PipelineStage.DIFFICULT_EXPRESSIONS_EXTRACTED);
        return expressions;
    }

    private String executeSimplification(String extractedText, PipelineContext context) {
        String simplifiedKorean = koreanSimplifierModule.process(extractedText, context);
        context.setSimplifiedKorean(simplifiedKorean);
        context.setCompletedStage(PipelineContext.PipelineStage.SIMPLIFIED);
        return simplifiedKorean;
    }

    private String executeTranslation(String simplifiedKorean, PipelineContext context) {
        String translatedText = translatorModule.process(simplifiedKorean, context);
        context.setTranslatedText(translatedText);
        context.setCompletedStage(PipelineContext.PipelineStage.TRANSLATED);
        return translatedText;
    }

    private String executeSummarization(String simplifiedKorean, PipelineContext context) {
        String summary = summarizerModule.process(simplifiedKorean, context);
        context.setSummary(summary);
        context.setCompletedStage(PipelineContext.PipelineStage.SUMMARIZED);
        return summary;
    }

    private TextExtractionResult extractText(UploadFile file, FileType fileType) {
        log.debug("텍스트 추출 시작: {}", fileType);

        TextExtractionResult result;
        if (fileType == FileType.IMAGE) {
            result = imageTextExtractor.extract(file);
        } else if (fileType == FileType.PDF) {
            result = pdfTextExtractor.extract(file);
        } else {
            throw new DocumentAnalysisException(ErrorStatus.UNSUPPORTED_FILE_TYPE);
        }

        if (!result.isSuccess() || result.getText() == null || result.getText().trim().isEmpty()) {
            throw new TextExtractionException(ErrorStatus.TEXT_EXTRACTION_FAILED);
        }

        log.debug("텍스트 추출 완료: {}자, 방식: {}", result.getText().length(), result.getOcrMethod());
        return result;
    }

    private DocumentAnalysis saveAnalysisResult(UploadFile file, FileType fileType, User user,
                                                PipelineContext context,
                                                ClassificationResult classification,
                                                ExtractionResult extraction, String extractedText,
                                                String translatedText, String summary) {
        try {
            if (user == null) {
                log.info("비로그인 사용자 - DB 저장 건너뜀");
                return null;
            }

            Document document = Document.builder().user(user).title(file.originalName())
                .description("문서 분석: " + classification.getDocumentType().getDisplayName()).build();

            DocumentFile documentFile = DocumentFile.builder().fileName(file.originalName())
                .fileType(fileType.name()).fileSize(file.size()).extractedText(extractedText)
                .pageCount(context.getPageCount() != null ? context.getPageCount() : 1).build();

            DocumentTranslation documentTranslation = DocumentTranslation.builder()
                .translatedLanguage(context.getTargetLanguage().getLanguageCode())
                .translatedText(translatedText).summary(summary).build();

            document.addDocumentFile(documentFile);
            document.addTranslation(documentTranslation);
            document = documentRepository.save(document);

            String schedulesJson = objectMapper.writeValueAsString(extraction.getSchedules());
            String additionalInfoJson = objectMapper.writeValueAsString(
                extraction.getAdditionalInfo());
            String keywordsStr = classification.getKeywords() != null ? String.join(",",
                classification.getKeywords()) : "";

            DocumentAnalysis analysis = DocumentAnalysis.builder().document(document)
                .documentType(classification.getDocumentType())
                .classificationConfidence(classification.getConfidence())
                .classificationKeywords(keywordsStr)
                .classificationReasoning(classification.getReasoning())
                .extractedSchedulesJson(schedulesJson).additionalInfoJson(additionalInfoJson)
                .processingTimeMs(System.currentTimeMillis()).ocrMethod(context.getOcrMethod())
                .totalSteps(TOTAL_STEPS).completedSteps(0)  // 로그 저장 후 업데이트
                .build();

            analysis = documentAnalysisRepository.save(analysis);
            log.info("DB 저장 완료: documentId={}, analysisId={}", document.getId(), analysis.getId());

            return analysis;

        } catch (Exception e) {
            log.error("분석 결과 저장 실패", e);
            return null;
        }
    }

    /**
     * 성공 응답 생성
     */
    private DocumentAnalysisResponse buildSuccessResponse(Long analysisId, UploadFile file,
                                                          ExtractionResult extraction,
                                                          String extractedText,
                                                          List<DifficultExpressionDto> difficultExpressions,
                                                          String translatedText, String summary) {
        return DocumentAnalysisResponse.builder().analysisId(analysisId)
            .extractedText(extractedText).difficultExpressions(difficultExpressions)
            .translatedText(translatedText).summary(summary)
            .extractedSchedules(extraction.getSchedules()).originalFileName(file.originalName())
            .build();
    }
}
