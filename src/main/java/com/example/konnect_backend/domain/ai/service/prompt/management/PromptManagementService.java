package com.example.konnect_backend.domain.ai.service.prompt.management;

import com.example.konnect_backend.domain.ai.dto.internal.GeminiCallResult;
import com.example.konnect_backend.domain.ai.dto.internal.PromptSummary;
import com.example.konnect_backend.domain.ai.dto.internal.PromptTemplateWithModelName;
import com.example.konnect_backend.domain.ai.dto.request.RunPromptRequest;
import com.example.konnect_backend.domain.ai.dto.response.*;
import com.example.konnect_backend.domain.ai.domain.entity.AiModel;
import com.example.konnect_backend.domain.ai.domain.entity.PromptSlot;
import com.example.konnect_backend.domain.ai.domain.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.infra.GeminiService;
import com.example.konnect_backend.domain.ai.repository.AiModelRepository;
import com.example.konnect_backend.domain.ai.repository.PromptTemplateRepository;
import com.example.konnect_backend.domain.ai.service.prompt.PromptTemplateResolver;
import com.example.konnect_backend.domain.ai.type.PromptStatus;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptManagementService {

    private final GeminiService geminiService;
    private final PromptLoader promptLoader;
    private final PromptTemplateResolver resolver;

    private final PromptTemplateRepository promptRepository;
    private final AiModelRepository aiModelRepository;

    @Transactional(readOnly = true)
    public PromptSummaryListResponse getPrompts(PromptStatus status, String moduleName) {
        List<PromptSummary> activePromptSummaries = promptRepository.findPrompts(
            status, moduleName);
        List<PromptSummaryResponse> promptResponses = activePromptSummaries.stream()
            .map(PromptSummaryResponse::from).toList();
        return new PromptSummaryListResponse(promptResponses);
    }

    @Transactional(readOnly = true)
    public PromptResponse getPrompt(Long promptId) {
        PromptTemplateWithModelName promptWithModelName = promptRepository.findPromptById(promptId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.PROMPT_NOT_FOUND));

        return PromptResponse.from(promptWithModelName);
    }

    @Transactional(readOnly = true)
    public ModelListResponse getModels() {
        List<AiModel> models = aiModelRepository.findAll();
        List<ModelResponse> responses = models.stream().map(ModelResponse::from).toList();
        return new ModelListResponse(responses);
    }

    // 동시성 문제는 DB unique 인덱스로 방지
    @Transactional
    public void activate(Long promptId) {
        PromptTemplate toActivate = promptRepository.findById(promptId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.PROMPT_NOT_FOUND));

        List<PromptTemplate> activePrompts = promptRepository.findByModuleNameAndStatus(
            toActivate.getModuleName(), PromptStatus.ACTIVE);
        if (activePrompts.size() != 1) {
            throw new IllegalStateException("활성화된 프롬프트가 1개가 아닙니다.");
        }
        PromptTemplate previousActive = activePrompts.get(0);

        toActivate.setStatus(PromptStatus.ACTIVE);
        previousActive.setStatus(PromptStatus.DEPRECATED);
    }

    public RunResultResponse run(RunPromptRequest request) {
        String prompt = resolver.resolve(request.promptTemplate(), request.vars());

        long start = System.currentTimeMillis();
        GeminiCallResult result = geminiService.call(request.modelName(), prompt, 0.2,
            request.maxTokens());
        long timeTakenInMillis = System.currentTimeMillis() - start;

        return new RunResultResponse(result.response(), (int) timeTakenInMillis,
            result.tokenUsage().inputTokens(), result.tokenUsage().outputTokens());
    }

    // 템플릿 내 변수 추가는 파이프라인에서 코드 변화가 필요하기에 지원하지 않습니다.
    @Transactional
    public PromptResponse createNewVersion(String moduleName, String template,
                                           String model, Integer maxTokens) {
        AiModel aiModel = aiModelRepository.findByName(model)
            .orElseThrow(() -> new GeneralException(ErrorStatus.INVALID_AI_MODEL));
        PromptTemplate maxVersion = promptRepository.getMaxVersionOfModule(moduleName);

        // (module_name, version) unique 제약으로 동시성 문제 없음
        PromptTemplate newPrompt = new PromptTemplate(moduleName, maxVersion.getVersion() + 1,
            template, maxTokens,
            aiModel.getId());

        PromptTemplate activePrompt = promptLoader.getActivePromptTemplate(moduleName);
        List<PromptSlot> slots = activePrompt.getSlots();

        Set<String> slotKeysInTemplate = resolver.getSlotKeys(template);
        Set<String> requiredKeys = slots.stream()
            .map(PromptSlot::getSlotKey)
            .collect(Collectors.toSet());
        if (!slotKeysInTemplate.containsAll(requiredKeys)) {
            throw new GeneralException(ErrorStatus.INVALID_PROMPT_TEMPLATE);
        }

        List<PromptSlot> newPromptSlots = slots.stream()
            .map(s -> s.withPromptTemplate(newPrompt)).toList();
        newPrompt.updateSlots(newPromptSlots);

        PromptTemplate saved = promptRepository.save(newPrompt);
        return PromptResponse.from(new PromptTemplateWithModelName(saved, aiModel.getName()));
    }
}
