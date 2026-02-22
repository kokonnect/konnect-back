package com.example.konnect_backend.domain.ai.service.pipeline;

import com.example.konnect_backend.domain.ai.dto.internal.ClassificationResult;
import com.example.konnect_backend.domain.ai.dto.internal.ExtractionResult;
import com.example.konnect_backend.domain.ai.dto.response.DifficultExpressionDto;
import com.example.konnect_backend.domain.ai.dto.response.DocumentAnalysisResponse;
import com.example.konnect_backend.domain.ai.infra.GeminiService;
import com.example.konnect_backend.domain.ai.model.vo.UploadFile;
import com.example.konnect_backend.domain.ai.service.prompt.PromptManager;
import com.example.konnect_backend.domain.ai.service.prompt.module.*;
import com.example.konnect_backend.domain.ai.service.textextractor.TextExtractorFacade;
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

    private final PromptManager promptManager;

    private final TextExtractorFacade textExtractorFacade;
    private final DocumentClassifierModule classifierModule; // 사용하지 않는 모듈이나 기존 로그와의 호환성을 위해 유지
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
    public DocumentAnalysisResponse analyze(UploadFile file, Long requesterId) {
        Long analysisId = idGenerator.newId();
        User user = getUser(requesterId);
        TargetLanguage targetLanguage = getTargetLanguage(user);

        PipelineContext context = PipelineContext.builder().targetLanguage(targetLanguage)
            .completedStage(PipelineContext.PipelineStage.NONE).metadata(new HashMap<>())
            .processingLogs(new ArrayList<>()).build();

        context.addMetadata("useSimpleLanguage", true);
        context.addMetadata("analysisId", analysisId);
        context.addMetadata("fileName", file.originalName());
        context.addMetadata("fileType", file.fileType().name());

        return executePipeline(analysisId, file, user, context);
    }

    private DocumentAnalysisResponse executePipeline(Long analysisId, UploadFile file, User user,
                                                     PipelineContext context) {
        long startTime = System.currentTimeMillis();

        final List<String> stages = List.of("INIT", "TEXT_EXTRACTION", "CLASSIFICATION",
            "EXTRACTION", "DIFFICULT_EXPRESSIONS", "SIMPLIFICATION", "TRANSLATION", "SUMMARIZATION",
            "SAVE");
        int currentStage = 0;
        DocumentAnalysis savedAnalysis;

        List<PromptModule> modules = List.of(classifierModule, unifiedExtractorModule,
            difficultExpressionExtractorModule, koreanSimplifierModule, translatorModule,
            summarizerModule);

        // 파이프라인 시작 시 토큰 사용량 초기화
        geminiService.resetSessionTokenUsage();

        try {
            log.info("문서 분석 파이프라인 시작: analysisId={}, 파일={}, 타입={}, 언어={}", analysisId,
                file.originalName(), file.fileType(), context.getTargetLanguage().getDisplayName());

            currentStage++;
            textExtractorFacade.extract(file, context).getText();

            for (PromptModule module : modules) {
                currentStage++;
                // Todo: 임시로 모든 버전 1 사용, 활성화된 버전으로 분기 가능하도록 변경 필요
                String promptTemplate = promptManager.getPromptTemplate(module.getModuleName(), 1);
                module.process(promptTemplate, context);
            }

            currentStage++;
            savedAnalysis = saveAnalysisResult(file, file.fileType(), user, context);

            // 10. 단계별 로그 저장
            if (savedAnalysis != null) {
                saveStepLogs(savedAnalysis, context);
            }

            context.setCompletedStage(PipelineContext.PipelineStage.COMPLETED);
            long processingTime = System.currentTimeMillis() - startTime;

            // 파이프라인 완료 시 총 토큰 사용량 로깅
            logTotalTokenUsage(analysisId, processingTime);

            // 11. 성공 응답 생성
            return buildSuccessResponse(analysisId, file, context);
        } catch (Exception e) {
            log.error("문서 분석 파이프라인 실패: analysisId={}, stage={}", analysisId, stages.get(currentStage), e);
            throw e;
        }
    }

    private User getUser(Long userId) {
        // 미인증 사용자 (테스트용)
        if (userId == null) {
            return null;
        }
        // UserService가 현재 인증 정보를 직접 꺼내써서 userId로 직접 접근하는 게 의미 상 명확
        return userRepository.findById(userId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    private TargetLanguage getTargetLanguage(User user) {
        // 미인증 사용자 (테스트용)
        if (user == null) {
            return TargetLanguage.KOREAN;
        }

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

    private DocumentAnalysis saveAnalysisResult(UploadFile file, FileType fileType, User user,
                                                PipelineContext context) {
        String extractedText = context.getExtractedText();
        ClassificationResult classification = context.getClassificationResult();
        ExtractionResult extraction = context.getExtractionResult();
        String translatedText = context.getTranslatedText();
        String summary = context.getSummary();

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
     * 단계별 로그 저장
     */
    private void saveStepLogs(DocumentAnalysis analysis, PipelineContext context) {
        String extractedText = context.getExtractedText();
        ClassificationResult classification = context.getClassificationResult();
        ExtractionResult extraction = context.getExtractionResult();
        List<DifficultExpressionDto> difficultExpressions = context.getDifficultExpressions();
        String simplifiedKorean = context.getSimplifiedKorean();
        String translatedText = context.getTranslatedText();
        String summary = context.getSummary();

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
                    .promptTemplate("DocumentClassifierModule") // Todo 로그 삭제 예정으로 임시 처리
                    .modelUsed(DocumentClassifierModule.MODEL_NAME)
                    .temperature(DocumentClassifierModule.TEMPERATURE)
                    .maxTokens(DocumentClassifierModule.MAX_TOKENS).build(),
                classifierModule.getLastRawResponse(), classification,
                classifierModule.getLastProcessingTimeMs());

            // 3. EXTRACTION 로그
            String extractionJson = objectMapper.writeValueAsString(extraction);
            stepLogService.logSuccess(analysis,
                StepLogService.StepInfo.builder().stepName("EXTRACTION").stepOrder(stepOrder++)
                    .inputText(extractedText).promptTemplate("UNIFIED_EXTRACTION_PROMPT") // Todo 로그 삭제 예정으로 임시 처리
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
                    .promptTemplate("DifficultExpressionExtractorModule") // Todo 로그 삭제 예정으로 임시 처리
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
                    .promptTemplate("KoreanSimplifierModule") // Todo 로그 삭제 예정으로 임시 처리
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
                    .promptTemplate("TranslatorModule") // Todo 로그 삭제 예정으로 임시 처리
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
                    .promptTemplate("SummarizerModule") // Todo 로그 삭제 예정으로 임시 처리
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

    /**
     * 성공 응답 생성
     */
    private DocumentAnalysisResponse buildSuccessResponse(Long analysisId, UploadFile file,
                                                          PipelineContext context) {
        String extractedText = context.getExtractedText();
        ExtractionResult extraction = context.getExtractionResult();
        List<DifficultExpressionDto> difficultExpressions = context.getDifficultExpressions();
        String translatedText = context.getTranslatedText();
        String summary = context.getSummary();

        return DocumentAnalysisResponse.builder().analysisId(analysisId)
            .extractedText(extractedText).difficultExpressions(difficultExpressions)
            .translatedText(translatedText).summary(summary)
            .extractedSchedules(extraction.getSchedules()).originalFileName(file.originalName())
            .build();
    }
}
