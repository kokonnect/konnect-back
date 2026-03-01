package com.example.konnect_backend.domain.ai.service.prompt.module;

import com.example.konnect_backend.domain.ai.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.service.pipeline.PipelineContext;

public interface PromptModule {

    void process(PromptTemplate promptTemplate, PipelineContext context);

    // DB prompt_template 테이블의 module_name과 일치해야 함
    String getModuleName();
}
