package com.example.konnect_backend.domain.ai.service.pipeline;

import com.example.konnect_backend.domain.ai.dto.internal.ClassificationResult;
import com.example.konnect_backend.domain.ai.dto.internal.ExtractionResult;
import com.example.konnect_backend.domain.ai.dto.response.DifficultExpressionDto;
import com.example.konnect_backend.domain.ai.dto.response.DocumentAnalysisResponse;
import com.example.konnect_backend.domain.ai.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.entity.log.AnalysisRequestLog;
import com.example.konnect_backend.domain.ai.model.vo.TokenUsage;
import com.example.konnect_backend.domain.ai.model.vo.UploadFile;
import com.example.konnect_backend.domain.ai.repository.AnalysisRequestLogRepository;
import com.example.konnect_backend.domain.ai.service.prompt.management.PromptLoader;
import com.example.konnect_backend.domain.ai.service.prompt.module.*;
import com.example.konnect_backend.domain.ai.service.textextractor.TextExtractorFacade;
import com.example.konnect_backend.domain.ai.type.FileType;
import com.example.konnect_backend.domain.ai.type.TargetLanguage;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.entity.status.Language;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.konnect_backend.domain.ai.interceptor.AnalysisInterceptor.REQUEST_ID_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAnalysisPipeline {

    private final PromptLoader promptLoader;

    private final TextExtractorFacade textExtractorFacade;
    private final DocumentClassifierModule classifierModule; // 사용하지 않는 모듈이나 기존 로그와의 호환성을 위해 유지
    private final UnifiedExtractorModule unifiedExtractorModule;
    private final DifficultExpressionExtractorModule difficultExpressionExtractorModule;
    private final KoreanSimplifierModule koreanSimplifierModule;
    private final TranslatorModule translatorModule;
    private final SummarizerModule summarizerModule;

    private final UserRepository userRepository;
    private final AnalysisRequestLogRepository requestLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public DocumentAnalysisResponse analyze(UploadFile file, Long requesterId) {
        UUID requestId = UUID.fromString(MDC.get(REQUEST_ID_KEY));
        log.debug("[analyze] requestId: {}", requestId);
        User user = getUser(requesterId);
        TargetLanguage targetLanguage = getTargetLanguage(user);

        PipelineContext context = PipelineContext.builder().requestId(requestId)
            .targetLanguage(targetLanguage)
            .completedStage(PipelineContext.PipelineStage.NONE)
            .filename(file.originalName())
            .fileType(file.fileType())
            .processingLogs(new ArrayList<>()).build();

        return executePipeline(requestId, file, user, context);
    }

    private DocumentAnalysisResponse executePipeline(UUID requestId, UploadFile file, User user,
                                                     PipelineContext context) {
        long startTime = System.currentTimeMillis();

        final List<String> stages = List.of("INIT", "TEXT_EXTRACTION", "CLASSIFICATION",
            "EXTRACTION", "DIFFICULT_EXPRESSIONS", "SIMPLIFICATION", "TRANSLATION", "SUMMARIZATION",
            "SAVE");
        List<PromptModule> modules = List.of(classifierModule, unifiedExtractorModule,
            difficultExpressionExtractorModule, koreanSimplifierModule, translatorModule,
            summarizerModule);

        try {
            log.debug("문서 분석 파이프라인 시작: requestId={}, 파일={}, 타입={}, 언어={}", requestId,
                file.originalName(), file.fileType(), context.getTargetLanguage().getDisplayName());

            textExtractorFacade.extract(file, context);

            for (PromptModule module : modules) {
                PromptTemplate promptTemplate = promptLoader.getActivePromptTemplate(
                    module.getModuleName());
                TokenUsage tokenUsage = module.process(promptTemplate, context);
                context.accTokenUsage(tokenUsage);
            }

            context.setCompletedStage(PipelineContext.PipelineStage.COMPLETED);

            // 전체 요청 수준에서의 기록
            long processingTime = System.currentTimeMillis() - startTime;

            // 1. 총 토큰 사용량 및 처리시간 로그 (Todo 콘솔 → 파일)
            logRequestProcessingResult("SUCCESS", context, processingTime);

            // 2. 요청 처리 기록
            AnalysisRequestLog succeededRequest = AnalysisRequestLog.succeed(requestId,
                user.getId(),
                (int) processingTime);
            requestLogRepository.save(succeededRequest);

            // 3. 번역 기록 조회용 정보
            // Todo 번역 기록 조회를 위한 모든 처리 정보 저장
            Long analysisId = saveAnalysisResult(file, file.fileType(), user, context);


            // 성공 응답 생성
            return buildSuccessResponse(analysisId, file, context);
        } catch (Exception e) {
            log.error("문서 분석 파이프라인 실패: requestId={}", requestId, e);

            // 실패 요청 기록 저장
            long processingTime = System.currentTimeMillis() - startTime;

            logRequestProcessingResult("FAIL", context, processingTime);
            AnalysisRequestLog failedRequest = AnalysisRequestLog.fail(requestId, user.getId(),
                (int) processingTime);
            requestLogRepository.save(failedRequest);

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

    // Todo JSON으로 변환
    private void logRequestProcessingResult(String status, PipelineContext context,
                                            long processingTimeInMillis) {
        UUID requestId = context.getRequestId();
        int inputTokens = context.getInputTokens().get();
        int outputTokens = context.getOutputTokens().get();

        double processingTimeInSeconds = processingTimeInMillis / 1000.0;

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📊 파이프라인 처리 종료: {}", status);
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("   요청 ID: {}", requestId);
        log.info("   처리 시간: {}ms ({}초)", processingTimeInMillis,
            String.format("%.1f", processingTimeInSeconds));
        log.info("─────────────────────토큰 사용량 요약──────────────────────────");
        log.info("   입력 토큰 (Input):  {}", String.format("%,d", inputTokens));
        log.info("   출력 토큰 (Output): {}", String.format("%,d", outputTokens));
        log.info("   총 토큰 (Total):    {}", String.format("%,d", inputTokens + outputTokens));
        log.info("═══════════════════════════════════════════════════════════════");
    }

    private Long saveAnalysisResult(UploadFile file, FileType fileType, User user,
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

            // Todo
            return null;

        } catch (Exception e) {
            log.error("분석 결과 저장 실패", e);
            return null;
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
