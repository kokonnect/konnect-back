package com.example.konnect_backend.domain.ai.service.pipeline;

import com.example.konnect_backend.domain.ai.dto.internal.ExtractionResult;
import com.example.konnect_backend.domain.ai.dto.internal.TextExtractionResult;
import com.example.konnect_backend.domain.ai.dto.response.ClassificationResult;
import com.example.konnect_backend.domain.ai.dto.response.DifficultExpressionDto;
import com.example.konnect_backend.domain.ai.dto.response.DocumentAnalysisResponse;
import com.example.konnect_backend.domain.ai.exception.DocumentAnalysisException;
import com.example.konnect_backend.domain.ai.exception.TextExtractionException;
import com.example.konnect_backend.domain.ai.service.GeminiService;
import com.example.konnect_backend.domain.ai.service.extractor.ImageTextExtractor;
import com.example.konnect_backend.domain.ai.service.extractor.PdfTextExtractor;
import com.example.konnect_backend.domain.ai.service.prompt.*;
import com.example.konnect_backend.domain.ai.type.FileType;
import com.example.konnect_backend.domain.ai.type.ProcessingStatus;
import com.example.konnect_backend.domain.ai.type.TargetLanguage;
import com.example.konnect_backend.domain.document.entity.Document;
import com.example.konnect_backend.domain.document.entity.DocumentAnalysis;
import com.example.konnect_backend.domain.document.entity.DocumentFile;
import com.example.konnect_backend.domain.document.entity.DocumentTranslation;
import com.example.konnect_backend.domain.document.repository.DocumentAnalysisRepository;
import com.example.konnect_backend.domain.document.repository.DocumentRepository;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.security.SecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final AnalysisCacheService cacheService;
    private final StepLogService stepLogService;
    private final GeminiService geminiService;

    private static final int TOTAL_STEPS = 7;

    /**
     * 새 문서 분석 시작
     */
    @Transactional
    public DocumentAnalysisResponse analyze(MultipartFile file, FileType fileType) {
        long startTime = System.currentTimeMillis();
        Long analysisId = cacheService.generateAnalysisId();

        User user = getCurrentUser();
        TargetLanguage targetLanguage = getUserTargetLanguage(user);

        PipelineContext context = PipelineContext.builder()
                .targetLanguage(targetLanguage)
                .completedStage(PipelineContext.PipelineStage.NONE)
                .metadata(new HashMap<>())
                .processingLogs(new ArrayList<>())
                .build();

        context.addMetadata("useSimpleLanguage", true);
        context.addMetadata("analysisId", analysisId);
        context.addMetadata("fileName", file.getOriginalFilename());
        context.addMetadata("fileType", fileType.name());

        return executePipeline(analysisId, file, fileType, user, context, startTime);
    }

    /**
     * 실패한 분석 재시도
     */
    @Transactional
    public DocumentAnalysisResponse retry(Long analysisId, MultipartFile file, FileType fileType) {
        long startTime = System.currentTimeMillis();

        PipelineContext context = cacheService.getContext(analysisId);
        if (context == null) {
            throw new DocumentAnalysisException(ErrorStatus.ANALYSIS_NOT_FOUND);
        }

        User user = getCurrentUser();
        log.info("분석 재시도: analysisId={}, 완료단계={}", analysisId, context.getCompletedStage());

        return executePipeline(analysisId, file, fileType, user, context, startTime);
    }

    /**
     * 파이프라인 실행 (신규/재시도 공통)
     */
    private DocumentAnalysisResponse executePipeline(Long analysisId, MultipartFile file, FileType fileType,
                                                     User user, PipelineContext context, long startTime) {
        String currentStage = "INIT";
        int completedStepCount = 0;
        DocumentAnalysis savedAnalysis = null;

        // 파이프라인 시작 시 토큰 사용량 초기화
        geminiService.resetSessionTokenUsage();

        try {
            log.info("문서 분석 파이프라인 시작: analysisId={}, 파일={}, 타입={}, 언어={}",
                    analysisId,
                    file.getOriginalFilename(),
                    fileType,
                    context.getTargetLanguage().getDisplayName());

            // 1. 파일 검증
            validateFile(file, fileType);

            // 2. 텍스트 추출 (OCR) - Step 1
            currentStage = "TEXT_EXTRACTION";
            String extractedText = executeTextExtraction(file, fileType, context);
            completedStepCount++;
            cacheService.saveContext(analysisId, context);

            // 3. 문서 유형 분류 - Step 2
            currentStage = "CLASSIFICATION";
            ClassificationResult classification = executeClassification(extractedText, context);
            completedStepCount++;
            cacheService.saveContext(analysisId, context);

            // 4. 통합 정보 추출 - Step 3
            currentStage = "EXTRACTION";
            ExtractionResult extraction = executeExtraction(extractedText, context);
            completedStepCount++;
            cacheService.saveContext(analysisId, context);

            // 5. 어려운 표현 추출 및 풀이 - Step 4
            currentStage = "DIFFICULT_EXPRESSIONS";
            List<DifficultExpressionDto> difficultExpressions = executeDifficultExpressionExtraction(extractedText, context);
            completedStepCount++;
            cacheService.saveContext(analysisId, context);

            // 6. 쉬운 한국어로 재작성 - Step 5
            currentStage = "SIMPLIFICATION";
            String simplifiedKorean = executeSimplification(extractedText, context);
            completedStepCount++;
            cacheService.saveContext(analysisId, context);

            // 7. 번역 (쉬운 한국어 기반) - Step 6
            currentStage = "TRANSLATION";
            String translatedText = executeTranslation(simplifiedKorean, context);
            completedStepCount++;
            cacheService.saveContext(analysisId, context);

            // 8. 요약 (쉬운 한국어 기반) - Step 7
            currentStage = "SUMMARIZATION";
            String summary = executeSummarization(simplifiedKorean, context);
            completedStepCount++;
            cacheService.saveContext(analysisId, context);

            // 9. DB 저장
            currentStage = "SAVE";
            savedAnalysis = saveAnalysisResult(file, fileType, user, context, classification,
                    extraction, extractedText, translatedText, summary);

            // 10. 단계별 로그 저장
            if (savedAnalysis != null) {
                saveStepLogs(savedAnalysis, context, extractedText, classification,
                        extraction, difficultExpressions, simplifiedKorean, translatedText, summary);
            }

            context.setCompletedStage(PipelineContext.PipelineStage.COMPLETED);
            long processingTime = System.currentTimeMillis() - startTime;

            // 완료 시 캐시 삭제
            cacheService.removeContext(analysisId);

            // 파이프라인 완료 시 총 토큰 사용량 로깅
            logTotalTokenUsage(analysisId, processingTime);

            // 11. 성공 응답 생성
            return buildSuccessResponse(analysisId, file, fileType, context, classification, extraction,
                    extractedText, difficultExpressions, simplifiedKorean, translatedText, summary, processingTime);

        } catch (DocumentAnalysisException | TextExtractionException e) {
            cacheService.saveContext(analysisId, context);
            return buildPartialResponse(analysisId, file, fileType, context, currentStage, e.getMessage(),
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("문서 분석 파이프라인 실패: analysisId={}, stage={}", analysisId, currentStage, e);
            cacheService.saveContext(analysisId, context);
            return buildPartialResponse(analysisId, file, fileType, context, currentStage, e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 단계별 로그 저장
     */
    private void saveStepLogs(DocumentAnalysis analysis, PipelineContext context,
                              String extractedText, ClassificationResult classification,
                              ExtractionResult extraction, List<DifficultExpressionDto> difficultExpressions,
                              String simplifiedKorean, String translatedText, String summary) {
        try {
            int stepOrder = 1;

            // 1. TEXT_EXTRACTION 로그
            stepLogService.logSuccess(analysis,
                    StepLogService.StepInfo.builder()
                            .stepName("TEXT_EXTRACTION")
                            .stepOrder(stepOrder++)
                            .promptTemplate("OCR")
                            .modelUsed(context.getOcrMethod())
                            .build(),
                    null,
                    null,
                    String.format("텍스트 추출 완료: %d자", extractedText.length()),
                    0L);

            // 2. CLASSIFICATION 로그
            stepLogService.logClassificationSuccess(analysis,
                    StepLogService.StepInfo.builder()
                            .stepName("CLASSIFICATION")
                            .stepOrder(stepOrder++)
                            .inputText(extractedText)
                            .promptTemplate(DocumentClassifierModule.PROMPT_TEMPLATE_NAME)
                            .modelUsed(DocumentClassifierModule.MODEL_NAME)
                            .temperature(DocumentClassifierModule.TEMPERATURE)
                            .maxTokens(DocumentClassifierModule.MAX_TOKENS)
                            .build(),
                    classifierModule.getLastRawResponse(),
                    classification,
                    classifierModule.getLastProcessingTimeMs());

            // 3. EXTRACTION 로그
            String extractionJson = objectMapper.writeValueAsString(extraction);
            stepLogService.logSuccess(analysis,
                    StepLogService.StepInfo.builder()
                            .stepName("EXTRACTION")
                            .stepOrder(stepOrder++)
                            .inputText(extractedText)
                            .promptTemplate("UNIFIED_EXTRACTION_PROMPT")
                            .modelUsed(UnifiedExtractorModule.MODEL_NAME)
                            .temperature(UnifiedExtractorModule.TEMPERATURE)
                            .maxTokens(UnifiedExtractorModule.MAX_TOKENS)
                            .build(),
                    unifiedExtractorModule.getLastRawResponse(),
                    extractionJson,
                    String.format("추출 완료: %d개 일정", extraction.getSchedules().size()),
                    unifiedExtractorModule.getLastProcessingTimeMs());

            // 4. DIFFICULT_EXPRESSIONS 로그
            String difficultJson = objectMapper.writeValueAsString(difficultExpressions);
            stepLogService.logSuccess(analysis,
                    StepLogService.StepInfo.builder()
                            .stepName("DIFFICULT_EXPRESSIONS")
                            .stepOrder(stepOrder++)
                            .inputText(extractedText)
                            .promptTemplate(DifficultExpressionExtractorModule.PROMPT_TEMPLATE_NAME)
                            .modelUsed(DifficultExpressionExtractorModule.MODEL_NAME)
                            .temperature(DifficultExpressionExtractorModule.TEMPERATURE)
                            .maxTokens(DifficultExpressionExtractorModule.MAX_TOKENS)
                            .build(),
                    difficultExpressionExtractorModule.getLastRawResponse(),
                    difficultJson,
                    String.format("어려운 표현 %d개 추출", difficultExpressions.size()),
                    difficultExpressionExtractorModule.getLastProcessingTimeMs());

            // 5. SIMPLIFICATION 로그
            stepLogService.logSuccess(analysis,
                    StepLogService.StepInfo.builder()
                            .stepName("SIMPLIFICATION")
                            .stepOrder(stepOrder++)
                            .inputText(extractedText)
                            .promptTemplate(KoreanSimplifierModule.PROMPT_TEMPLATE_NAME)
                            .modelUsed(KoreanSimplifierModule.MODEL_NAME)
                            .temperature(KoreanSimplifierModule.TEMPERATURE)
                            .maxTokens(KoreanSimplifierModule.MAX_TOKENS)
                            .build(),
                    koreanSimplifierModule.getLastRawResponse(),
                    simplifiedKorean,
                    String.format("쉬운 한국어 %d자", simplifiedKorean.length()),
                    koreanSimplifierModule.getLastProcessingTimeMs());

            // 6. TRANSLATION 로그
            stepLogService.logSuccess(analysis,
                    StepLogService.StepInfo.builder()
                            .stepName("TRANSLATION")
                            .stepOrder(stepOrder++)
                            .inputText(simplifiedKorean)
                            .promptTemplate(TranslatorModule.PROMPT_TEMPLATE_NAME)
                            .modelUsed(TranslatorModule.MODEL_NAME)
                            .temperature(TranslatorModule.TEMPERATURE)
                            .maxTokens(TranslatorModule.MAX_TOKENS)
                            .build(),
                    translatorModule.getLastRawResponse(),
                    translatedText,
                    String.format("번역 완료: %d자 -> %s",
                            translatedText.length(),
                            context.getTargetLanguage().getDisplayName()),
                    translatorModule.getLastProcessingTimeMs());

            // 7. SUMMARIZATION 로그
            stepLogService.logSuccess(analysis,
                    StepLogService.StepInfo.builder()
                            .stepName("SUMMARIZATION")
                            .stepOrder(stepOrder)
                            .inputText(simplifiedKorean)
                            .promptTemplate(SummarizerModule.PROMPT_TEMPLATE_NAME)
                            .modelUsed(SummarizerModule.MODEL_NAME)
                            .temperature(SummarizerModule.TEMPERATURE)
                            .maxTokens(SummarizerModule.MAX_TOKENS)
                            .build(),
                    summarizerModule.getLastRawResponse(),
                    summary,
                    String.format("요약 완료: %d자", summary.length()),
                    summarizerModule.getLastProcessingTimeMs());

            // 분석 완료 카운트 업데이트
            analysis.updateStepCounts(TOTAL_STEPS, TOTAL_STEPS);

            log.info("단계별 로그 저장 완료: analysisId={}", analysis.getId());

        } catch (Exception e) {
            log.error("단계별 로그 저장 실패", e);
        }
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
            log.info("   처리 시간: {}ms ({}초)", processingTime, String.format("%.1f", processingSeconds));
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
    private String executeTextExtraction(MultipartFile file, FileType fileType, PipelineContext context) {
        if (context.getCompletedStage().ordinal() >= PipelineContext.PipelineStage.TEXT_EXTRACTED.ordinal()
                && context.getOriginalText() != null) {
            log.debug("텍스트 추출 단계 스킵 (이미 완료)");
            return context.getOriginalText();
        }

        TextExtractionResult extractionResult = extractText(file, fileType, context);
        String extractedText = extractionResult.getText();
        context.setOriginalText(extractedText);
        context.setOcrMethod(extractionResult.getOcrMethod());
        context.setPageCount(extractionResult.getPageCount());
        context.setCompletedStage(PipelineContext.PipelineStage.TEXT_EXTRACTED);
        return extractedText;
    }

    private ClassificationResult executeClassification(String extractedText, PipelineContext context) {
        if (context.getCompletedStage().ordinal() >= PipelineContext.PipelineStage.CLASSIFIED.ordinal()
                && context.getClassificationResult() != null) {
            log.debug("문서 분류 단계 스킵 (이미 완료)");
            return context.getClassificationResult();
        }

        ClassificationResult classification = classifierModule.process(extractedText, context);
        context.setClassificationResult(classification);
        context.setDocumentType(classification.getDocumentType());
        context.setCompletedStage(PipelineContext.PipelineStage.CLASSIFIED);
        return classification;
    }

    private ExtractionResult executeExtraction(String extractedText, PipelineContext context) {
        if (context.getCompletedStage().ordinal() >= PipelineContext.PipelineStage.EXTRACTED.ordinal()
                && context.getExtractionResult() != null) {
            log.debug("정보 추출 단계 스킵 (이미 완료)");
            return context.getExtractionResult();
        }

        // 통합 Extractor 사용 (문서 유형과 무관하게 모든 정보 추출 시도)
        ExtractionResult extraction = unifiedExtractorModule.process(extractedText, context);
        context.setExtractionResult(extraction);
        context.setCompletedStage(PipelineContext.PipelineStage.EXTRACTED);
        return extraction;
    }

    private List<DifficultExpressionDto> executeDifficultExpressionExtraction(String extractedText, PipelineContext context) {
        if (context.getCompletedStage().ordinal() >= PipelineContext.PipelineStage.DIFFICULT_EXPRESSIONS_EXTRACTED.ordinal()
                && context.getDifficultExpressions() != null) {
            log.debug("어려운 표현 추출 단계 스킵 (이미 완료)");
            return context.getDifficultExpressions();
        }

        List<DifficultExpressionDto> expressions = difficultExpressionExtractorModule.process(extractedText, context);
        context.setDifficultExpressions(expressions);
        context.setCompletedStage(PipelineContext.PipelineStage.DIFFICULT_EXPRESSIONS_EXTRACTED);
        return expressions;
    }

    private String executeSimplification(String extractedText, PipelineContext context) {
        if (context.getCompletedStage().ordinal() >= PipelineContext.PipelineStage.SIMPLIFIED.ordinal()
                && context.getSimplifiedKorean() != null) {
            log.debug("쉬운 한국어 재작성 단계 스킵 (이미 완료)");
            return context.getSimplifiedKorean();
        }

        String simplifiedKorean = koreanSimplifierModule.process(extractedText, context);
        context.setSimplifiedKorean(simplifiedKorean);
        context.setCompletedStage(PipelineContext.PipelineStage.SIMPLIFIED);
        return simplifiedKorean;
    }

    private String executeTranslation(String simplifiedKorean, PipelineContext context) {
        if (context.getCompletedStage().ordinal() >= PipelineContext.PipelineStage.TRANSLATED.ordinal()
                && context.getTranslatedText() != null) {
            log.debug("번역 단계 스킵 (이미 완료)");
            return context.getTranslatedText();
        }

        String translatedText = translatorModule.process(simplifiedKorean, context);
        context.setTranslatedText(translatedText);
        context.setCompletedStage(PipelineContext.PipelineStage.TRANSLATED);
        return translatedText;
    }

    private String executeSummarization(String simplifiedKorean, PipelineContext context) {
        if (context.getCompletedStage().ordinal() >= PipelineContext.PipelineStage.SUMMARIZED.ordinal()
                && context.getSummary() != null) {
            log.debug("요약 단계 스킵 (이미 완료)");
            return context.getSummary();
        }

        String summary = summarizerModule.process(simplifiedKorean, context);
        context.setSummary(summary);
        context.setCompletedStage(PipelineContext.PipelineStage.SUMMARIZED);
        return summary;
    }

    private User getCurrentUser() {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    private TargetLanguage getUserTargetLanguage(User user) {
        if (user == null || user.getLanguage() == null) {
            return TargetLanguage.KOREAN;
        }
        return TargetLanguage.fromLanguage(user.getLanguage());
    }

    private void validateFile(MultipartFile file, FileType fileType) {
        if (file.isEmpty()) {
            throw new DocumentAnalysisException(ErrorStatus.FILE_EMPTY);
        }

        if (file.getOriginalFilename() == null) {
            throw new DocumentAnalysisException(ErrorStatus.FILE_NAME_MISSING);
        }

        if (file.getSize() > 20 * 1024 * 1024) {
            throw new DocumentAnalysisException(ErrorStatus.FILE_SIZE_EXCEEDED);
        }

        String contentType = file.getContentType();
        if (fileType == FileType.PDF && !"application/pdf".equals(contentType)) {
            throw new DocumentAnalysisException(ErrorStatus.INVALID_PDF_FILE);
        }

        if (fileType == FileType.IMAGE && (contentType == null || !contentType.startsWith("image/"))) {
            throw new DocumentAnalysisException(ErrorStatus.INVALID_IMAGE_FILE);
        }
    }

    private TextExtractionResult extractText(MultipartFile file, FileType fileType, PipelineContext context) {
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

    private DocumentAnalysis saveAnalysisResult(MultipartFile file, FileType fileType, User user,
                                                 PipelineContext context, ClassificationResult classification,
                                                 ExtractionResult extraction, String extractedText,
                                                 String translatedText, String summary) {
        try {
            if (user == null) {
                log.info("비로그인 사용자 - DB 저장 건너뜀");
                return null;
            }

            Document document = Document.builder()
                    .user(user)
                    .title(file.getOriginalFilename())
                    .description("문서 분석: " + classification.getDocumentType().getDisplayName())
                    .build();

            DocumentFile documentFile = DocumentFile.builder()
                    .fileName(file.getOriginalFilename())
                    .fileType(fileType.name())
                    .fileSize(file.getSize())
                    .extractedText(extractedText)
                    .pageCount(context.getPageCount() != null ? context.getPageCount() : 1)
                    .build();

            DocumentTranslation documentTranslation = DocumentTranslation.builder()
                    .translatedLanguage(context.getTargetLanguage().getLanguageCode())
                    .translatedText(translatedText)
                    .summary(summary)
                    .build();

            document.addDocumentFile(documentFile);
            document.addTranslation(documentTranslation);
            document = documentRepository.save(document);

            String schedulesJson = objectMapper.writeValueAsString(extraction.getSchedules());
            String additionalInfoJson = objectMapper.writeValueAsString(extraction.getAdditionalInfo());
            String keywordsStr = classification.getKeywords() != null
                    ? String.join(",", classification.getKeywords())
                    : "";

            DocumentAnalysis analysis = DocumentAnalysis.builder()
                    .document(document)
                    .documentType(classification.getDocumentType())
                    .classificationConfidence(classification.getConfidence())
                    .classificationKeywords(keywordsStr)
                    .classificationReasoning(classification.getReasoning())
                    .extractedSchedulesJson(schedulesJson)
                    .additionalInfoJson(additionalInfoJson)
                    .processingTimeMs(System.currentTimeMillis())
                    .ocrMethod(context.getOcrMethod())
                    .totalSteps(TOTAL_STEPS)
                    .completedSteps(0)  // 로그 저장 후 업데이트
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
    private DocumentAnalysisResponse buildSuccessResponse(Long analysisId, MultipartFile file, FileType fileType,
                                                          PipelineContext context,
                                                          ClassificationResult classification, ExtractionResult extraction,
                                                          String extractedText, List<DifficultExpressionDto> difficultExpressions,
                                                          String simplifiedKorean, String translatedText, String summary,
                                                          long processingTime) {
        return DocumentAnalysisResponse.builder()
                .analysisId(analysisId)
                .status(
                    com.example.konnect_backend.domain.ai.type.ProcessingStatus.COMPLETED)
                .extractedText(extractedText)
                .simplifiedKorean(simplifiedKorean)
                .difficultExpressions(difficultExpressions)
                .translatedText(translatedText)
                .summary(summary)
                .documentType(classification.getDocumentType())
                .documentTypeName(classification.getDocumentType().getDisplayName())
                .classificationConfidence(classification.getConfidence())
                .classificationKeywords(classification.getKeywords())
                .classificationReasoning(classification.getReasoning())
                .extractedSchedules(extraction.getSchedules())
                .extractedInfo(extraction.getAdditionalInfo())
                .originalFileName(file.getOriginalFilename())
                .fileType(fileType)
                .targetLanguage(context.getTargetLanguage())
                .targetLanguageName(context.getTargetLanguage().getDisplayName())
                .fileSize(file.getSize())
                .pageCount(context.getPageCount())
                .processingTimeMs(processingTime)
                .ocrMethod(context.getOcrMethod())
                .build();
    }

    /**
     * 부분 완료/실패 응답 생성 (재시도 가능)
     */
    private DocumentAnalysisResponse buildPartialResponse(Long analysisId, MultipartFile file, FileType fileType,
                                                          PipelineContext context, String failedStage, String errorMessage,
                                                          long processingTime) {
        DocumentAnalysisResponse.DocumentAnalysisResponseBuilder builder = DocumentAnalysisResponse.builder()
                .analysisId(analysisId)
                .status(ProcessingStatus.PARTIAL)
                .failedStage(failedStage)
                .errorMessage(errorMessage)
                .originalFileName(file.getOriginalFilename())
                .fileType(fileType)
                .targetLanguage(context.getTargetLanguage())
                .targetLanguageName(context.getTargetLanguage().getDisplayName())
                .fileSize(file.getSize())
                .processingTimeMs(processingTime)
                .ocrMethod(context.getOcrMethod());

        // 완료된 단계까지의 데이터 포함
        if (context.getOriginalText() != null) {
            builder.extractedText(context.getOriginalText());
            builder.pageCount(context.getPageCount());
        }

        if (context.getClassificationResult() != null) {
            ClassificationResult cr = context.getClassificationResult();
            builder.documentType(cr.getDocumentType())
                    .documentTypeName(cr.getDocumentType().getDisplayName())
                    .classificationConfidence(cr.getConfidence())
                    .classificationKeywords(cr.getKeywords())
                    .classificationReasoning(cr.getReasoning());
        }

        if (context.getExtractionResult() != null) {
            ExtractionResult er = context.getExtractionResult();
            builder.extractedSchedules(er.getSchedules())
                    .extractedInfo(er.getAdditionalInfo());
        }

        if (context.getDifficultExpressions() != null) {
            builder.difficultExpressions(context.getDifficultExpressions());
        }

        if (context.getSimplifiedKorean() != null) {
            builder.simplifiedKorean(context.getSimplifiedKorean());
        }

        if (context.getTranslatedText() != null) {
            builder.translatedText(context.getTranslatedText());
        }

        if (context.getSummary() != null) {
            builder.summary(context.getSummary());
        }

        return builder.build();
    }
}
