package com.example.konnect_backend.domain.ai.service.prompt;

import com.example.konnect_backend.domain.ai.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.repository.PromptTemplateRepository;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PromptManager {

    private final PromptTemplateRepository repository;

    @Transactional(readOnly = true)
    public String getPromptTemplate(String moduleName, Integer version) {
        PromptTemplate template = repository.findByModuleNameAndVersion(moduleName, version)
            .orElseThrow(() -> new GeneralException(ErrorStatus.PromptNotFound));

        return template.getTemplate();
    }


}
