package com.example.konnect_backend.domain.ai.service.prompt;

import com.example.konnect_backend.domain.ai.service.pipeline.PipelineContext;

public interface PromptModule<I, O> {

    O process(I input, PipelineContext context);

    String getModuleName();
}
