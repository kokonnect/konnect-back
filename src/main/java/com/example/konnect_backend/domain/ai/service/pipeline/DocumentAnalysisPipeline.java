package com.example.konnect_backend.domain.ai.service.pipeline;

import com.example.konnect_backend.domain.ai.dto.internal.ExtractionResult;
import com.example.konnect_backend.domain.ai.dto.response.DifficultExpressionDto;
import com.example.konnect_backend.domain.ai.dto.response.DocumentAnalysisResponse;
import com.example.konnect_backend.domain.ai.domain.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.domain.entity.log.AnalysisRequestLog;
import com.example.konnect_backend.domain.ai.domain.vo.ExtractedText;
import com.example.konnect_backend.domain.ai.domain.vo.PipelineContext;
import com.example.konnect_backend.domain.ai.domain.vo.TokenUsage;
import com.example.konnect_backend.domain.ai.domain.vo.UploadFile;
import com.example.konnect_backend.domain.ai.repository.AnalysisRequestLogRepository;
import com.example.konnect_backend.domain.ai.service.history.AnalysisHistoryService;
import com.example.konnect_backend.domain.ai.service.module.*;
import com.example.konnect_backend.domain.ai.service.prompt.management.PromptLoader;
import com.example.konnect_backend.domain.ai.service.textextractor.TextExtractorFacade;
import com.example.konnect_backend.domain.ai.type.TargetLanguage;
import com.example.konnect_backend.domain.user.entity.Device;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.entity.status.UsageType;
import com.example.konnect_backend.domain.user.repository.DeviceRepository;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.domain.user.service.UsageFacade;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.example.konnect_backend.domain.ai.interceptor.AnalysisInterceptor.REQUEST_ID_KEY;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAnalysisPipeline {

    private static final Logger jsonLogger = LoggerFactory.getLogger(
        "com.example.konnect_backend.domain.ai.service.pipeline.DocumentAnalysisPipeline.json");

    private final PromptLoader promptLoader;
    private final AnalysisHistoryService analysisHistoryService;
    private final UsageFacade usageFacade;

    private final TextExtractorFacade textExtractorFacade;
    private final DocumentClassifierModule classifierModule;
    private final UnifiedExtractorModule unifiedExtractorModule;
    private final DifficultExpressionExtractorModule difficultExpressionExtractorModule;
    private final KoreanSimplifierModule koreanSimplifierModule;
    private final TranslatorModule translatorModule;
    private final SummarizerModule summarizerModule;
    private final DeviceRepository deviceRepository;

    private final UserRepository userRepository;
    private final AnalysisRequestLogRepository requestLogRepository;

    private final ThreadPoolTaskExecutor promptExecutor;

    @Transactional
    public DocumentAnalysisResponse analyze(UploadFile file, Long requesterId, String deviceUuid) {

        // 사용량 증가
        usageFacade.validateAndIncrease(UsageType.DOCUMENT, deviceUuid);

        UUID requestId = UUID.fromString(MDC.get(REQUEST_ID_KEY));
        log.debug("[analyze] requestId: {}", requestId);
        User user = getUser(requesterId);
        TargetLanguage targetLanguage = getTargetLanguage(user, deviceUuid);

        PipelineContext context = PipelineContext.builder()
                .requestId(requestId)
                .targetLanguage(targetLanguage)
                .completedStage(PipelineContext.PipelineStage.NONE)
                .filename(file.originalName())
                .fileType(file.fileType())
                .processingLogs(new ArrayList<>())
                .build();

        return executePipeline(requestId, file, user, deviceUuid, context);
    }

    private DocumentAnalysisResponse executePipeline(UUID requestId, UploadFile file, User user,
                                                     String deviceUuid, PipelineContext context) {
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

//            for (PromptModule module : modules) {
//                PromptTemplate promptTemplate = promptLoader.getActivePromptTemplate(
//                    module.getModuleName());
//                TokenUsage tokenUsage = module.process(promptTemplate, context);
//                context.accTokenUsage(tokenUsage);
//            }

            CompletableFuture<Void> classification = CompletableFuture.runAsync(() -> {
                executeModuleAndAccTokenUsage(classifierModule, context);
            }, promptExecutor);

            CompletableFuture<Void> extraction = CompletableFuture.runAsync(() -> {
                executeModuleAndAccTokenUsage(unifiedExtractorModule, context);
            }, promptExecutor);

            CompletableFuture<Void> difficultExpression = CompletableFuture.runAsync(() -> {
                executeModuleAndAccTokenUsage(difficultExpressionExtractorModule, context);
            }, promptExecutor);

            CompletableFuture<Void> simplification = CompletableFuture.runAsync(() -> {
                executeModuleAndAccTokenUsage(koreanSimplifierModule, context);
            }, promptExecutor).thenCompose(_ignored -> CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> {
                    executeModuleAndAccTokenUsage(translatorModule, context);
                }, promptExecutor),
                CompletableFuture.runAsync(() -> {
                    executeModuleAndAccTokenUsage(summarizerModule, context);
                }, promptExecutor)
            ));

            List<CompletableFuture<Void>> futures = new ArrayList<>(
                List.of(classification, extraction, difficultExpression, simplification));

            CompletableFuture<Void> all = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            try {
                CompletableFuture.allOf(all).join();
            } catch (CompletionException e) {
                futures.forEach(f -> f.cancel(true));
                throw e; // 또는 원인 예외 unwrap
            }

            context.setCompletedStage(PipelineContext.PipelineStage.COMPLETED);

            // 전체 요청 수준에서의 기록
            // 콘솔 로그, AnalysisRequestLog, 번역 기록의 timestamp 는 일치해야 한다.
            LocalDateTime now = LocalDateTime.now();
            long processingTime = System.currentTimeMillis() - startTime;

            // 1. 총 토큰 사용량 및 처리시간 로그
            logRequestProcessingResult("SUCCESS", context, processingTime, now);

            // 2. 요청 처리 기록
            AnalysisRequestLog succeededRequest = AnalysisRequestLog.succeed(requestId,
                user == null ? null : user.getId(), (int) processingTime, now);
            AnalysisRequestLog savedRequestLog = requestLogRepository.save(succeededRequest);

            // 3. 분석 내역 조회용
            Long analysisId = analysisHistoryService.saveHistory(
                    user == null ? null : user.getId(),
                    deviceUuid,
                    file,
                    context.getTargetLanguage(),
                    savedRequestLog.getId(),
                    new ExtractedText(context.getExtractedText()),
                    context.getTranslatedText(),
                    context.getSummary(),
                    now
            );

            return buildSuccessResponse(analysisId, file, context);
        } catch (Exception e) {
            log.error("문서 분석 파이프라인 실패: requestId={}", requestId, e);

            LocalDateTime now = LocalDateTime.now();

            // 실패 요청 기록 저장
            long processingTime = System.currentTimeMillis() - startTime;

            logRequestProcessingResult("FAIL", context, processingTime, now);

            AnalysisRequestLog failedRequest = AnalysisRequestLog.fail(requestId,
                user == null ? null : user.getId(), // 원래 null 이 있으면 안 됨
                (int) processingTime, now);
            requestLogRepository.save(failedRequest);

            throw e;
        }
    }

    private void executeModuleAndAccTokenUsage(PromptModule module, PipelineContext context) {
        PromptTemplate promptTemplate = promptLoader.getActivePromptTemplate(module.getModuleName());
        TokenUsage tokenUsage = module.process(promptTemplate, context);
        context.accTokenUsage(tokenUsage);
    }

    private User getUser(Long userId) {
        // 미인증 사용자 (게스트용)
        if (userId == null) {
            return null;
        }
        // UserService가 현재 인증 정보를 직접 꺼내써서 userId로 직접 접근하는 게 의미 상 명확
        return userRepository.findById(userId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    private TargetLanguage getTargetLanguage(User user, String deviceUuid) {
        // 로그인 사용자 우선
        if (user != null && user.getLanguage() != null) {
            return TargetLanguage.fromLanguage(user.getLanguage());
        }

        // 게스트 → device language
        if (deviceUuid != null) {
            return deviceRepository.findById(deviceUuid)
                    .map(Device::getLanguage)
                    .filter(lang -> lang != null)
                    .map(TargetLanguage::fromLanguage)
                    .orElse(TargetLanguage.KOREAN);
        }

        // fallback
        return TargetLanguage.KOREAN;
    }

    private void logRequestProcessingResult(String status, PipelineContext context,
                                            long processingTimeInMillis, LocalDateTime timestamp) {
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


        jsonLogger.info("파이프라인 처리 종료", kv("status", status), kv("request id", requestId),
            kv("processing time in millis", processingTimeInMillis),
            kv("input tokens", inputTokens), kv("output tokens", outputTokens),
            kv("total tokens", inputTokens + outputTokens), kv("timestamp", timestamp));
    }
    
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
