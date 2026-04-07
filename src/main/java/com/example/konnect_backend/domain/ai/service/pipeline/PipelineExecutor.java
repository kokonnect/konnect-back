package com.example.konnect_backend.domain.ai.service.pipeline;

import com.example.konnect_backend.domain.ai.domain.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.domain.vo.PipelineContext;
import com.example.konnect_backend.domain.ai.domain.vo.TokenUsage;
import com.example.konnect_backend.domain.ai.service.module.*;
import com.example.konnect_backend.domain.ai.service.prompt.management.PromptLoader;
import com.example.konnect_backend.domain.ai.service.textextractor.TextExtractorFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@RequiredArgsConstructor
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

        CompletableFuture<Void> classification = run(classifierModule, context);
        CompletableFuture<Void> extraction = run(unifiedExtractorModule, context);
        CompletableFuture<Void> difficultExpression = run(difficultExpressionExtractorModule,
            context);

        CompletableFuture<Void> simplificationFlow =
            run(koreanSimplifierModule, context)
                .thenCompose(v -> CompletableFuture.allOf(
                    run(translatorModule, context),
                    run(summarizerModule, context)
                ));

        CompletableFuture<Void> all = CompletableFuture.allOf(
            classification,
            extraction,
            difficultExpression,
            simplificationFlow
        );

        try {
            all.join();
        } catch (CompletionException e) {
            all.cancel(true);
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
        );
    }
}
