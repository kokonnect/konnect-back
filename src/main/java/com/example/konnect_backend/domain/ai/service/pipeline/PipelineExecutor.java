package com.example.konnect_backend.domain.ai.service.pipeline;

import com.example.konnect_backend.domain.ai.domain.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.domain.vo.PipelineContext;
import com.example.konnect_backend.domain.ai.domain.vo.TokenUsage;
import com.example.konnect_backend.domain.ai.exception.DocumentAnalysisException;
import com.example.konnect_backend.domain.ai.service.module.*;
import com.example.konnect_backend.domain.ai.service.prompt.management.PromptLoader;
import com.example.konnect_backend.domain.ai.service.textextractor.TextExtractorFacade;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineExecutor {

    private final TextExtractorFacade textExtractorFacade;
    private final DocumentClassifierModule classifierModule;
    private final UnifiedExtractorModule unifiedExtractorModule;
    private final DifficultExpressionExtractorModule difficultExpressionExtractorModule;
    private final KoreanSimplifierModule koreanSimplifierModule;
    private final TranslatorModule translatorModule;
    private final SummarizerModule summarizerModule;

    private final PromptLoader promptLoader;
    private final ThreadPoolTaskExecutor promptExecutor;

    @Transactional
    public void execute(PipelineContext context) {
        textExtractorFacade.extract(context);

        AtomicBoolean pipelineFailed = new AtomicBoolean(false);

        CompletableFuture<Void> classification = run(classifierModule, context)
            .whenComplete((r, e) -> { if (e != null) pipelineFailed.set(true); });
        CompletableFuture<Void> extraction = run(unifiedExtractorModule, context)
            .whenComplete((r, e) -> { if (e != null) pipelineFailed.set(true); });
        CompletableFuture<Void> difficultExpression = run(difficultExpressionExtractorModule, context)
            .whenComplete((r, e) -> { if (e != null) pipelineFailed.set(true); });
        CompletableFuture<Void> koreanSimplified = run(koreanSimplifierModule, context)
            .whenComplete((r, e) -> { if (e != null) pipelineFailed.set(true); });

        // koreanSimplified 성공 시에도 다른 병렬 모듈이 이미 실패했으면 미실행
        CompletableFuture<Void> translator = koreanSimplified.thenCompose(v -> {
            if (pipelineFailed.get()) return CompletableFuture.failedFuture(
                new DocumentAnalysisException(ErrorStatus.DOCUMENT_ANALYSIS_FAILED));
            return run(translatorModule, context);
        });
        CompletableFuture<Void> summarizer = koreanSimplified.thenCompose(v -> {
            if (pipelineFailed.get()) return CompletableFuture.failedFuture(
                new DocumentAnalysisException(ErrorStatus.DOCUMENT_ANALYSIS_FAILED));
            return run(summarizerModule, context);
        });

        CompletableFuture<Void> all = CompletableFuture.allOf(
            classification, extraction, difficultExpression, koreanSimplified, translator, summarizer
        );

        // 실제로 시작된 모든 호출이 완료될 때까지 대기 → 토큰 사용량 전부 집계
        CompletableFuture.allOf(
            classification.exceptionally(e -> null),
            extraction.exceptionally(e -> null),
            difficultExpression.exceptionally(e -> null),
            koreanSimplified.exceptionally(e -> null),
            translator.exceptionally(e -> null),
            summarizer.exceptionally(e -> null)
        ).join();

        // CompletionException 언래핑 → 호출자에게 원래 예외 전달
        try {
            all.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw e;
        }

        context.setCompletedStage(PipelineContext.PipelineStage.COMPLETED);
    }

    private void executeModuleAndAccTokenUsage(PromptModule module, PipelineContext context) {
        PromptTemplate promptTemplate = promptLoader.getActivePromptTemplate(
            module.getModuleName());
        TokenUsage tokenUsage = module.process(promptTemplate, context);
        context.accTokenUsage(tokenUsage);
    }

    private CompletableFuture<Void> run(PromptModule module, PipelineContext context) {
        return CompletableFuture.runAsync(
            () -> executeModuleAndAccTokenUsage(module, context),
            promptExecutor
        ).whenComplete((result, throwable) -> {
            if (throwable != null) {
                Throwable cause = throwable instanceof CompletionException ce ? ce.getCause() : throwable;
                log.error("모듈 실패",
                    kv("event", "MODULE_FAILED"),
                    kv("module", module.getModuleName()),
                    kv("error", cause != null ? cause.getMessage() : throwable.getMessage()));
            }
        });
    }
}
