package com.example.konnect_backend.domain.ai.service.module;

import com.example.konnect_backend.domain.ai.domain.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.domain.vo.TokenUsage;
import com.example.konnect_backend.domain.ai.domain.vo.PipelineContext;

import java.util.Map;

public interface PromptModule {

    TokenUsage process(PromptTemplate promptTemplate, PipelineContext context);

    // DB prompt_template 테이블의 module_name과 일치해야 함
    String getModuleName();

    Map<String, String> getVars(PipelineContext context);
}
