package com.example.konnect_backend.domain.ai.service.prompt.management;

import com.example.konnect_backend.domain.ai.domain.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.repository.PromptTemplateRepository;
import com.example.konnect_backend.domain.ai.type.PromptStatus;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromptLoader {

    private final PromptTemplateRepository repository;

    public PromptTemplate getActivePromptTemplate(String moduleName) {
        List<PromptTemplate> promptTemplates = repository.findByModuleNameAndStatus(
            moduleName, PromptStatus.ACTIVE);

        if(promptTemplates.isEmpty()) throw new GeneralException(ErrorStatus.PROMPT_NOT_FOUND);

        if(promptTemplates.size() > 1) throw new IllegalStateException("활성화된 프롬프트 1개 초과");

        return promptTemplates.get(0);
    }
}
