package com.example.konnect_backend.domain.ai.service.log;

import com.example.konnect_backend.domain.ai.aop.PromptContext;
import com.example.konnect_backend.domain.ai.domain.entity.log.LlmCallDetail;
import com.example.konnect_backend.domain.ai.domain.entity.log.LlmCallMetadata;
import com.example.konnect_backend.domain.ai.dto.internal.GeminiCallResult;
import com.example.konnect_backend.domain.ai.repository.LlmCallDetailRepository;
import com.example.konnect_backend.domain.ai.repository.LlmCallMetadataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiLogService {

    private final LlmCallMetadataRepository metadataRepository;
    private final LlmCallDetailRepository detailRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(UUID requestId, @Nullable GeminiCallResult result, PromptContext context,
                        int latency) {
        saveLog(requestId, result, context, latency, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(UUID requestId, @Nullable GeminiCallResult result, PromptContext context,
                        int latency, @Nullable String failedModel) {
        LocalDateTime logTime = LocalDateTime.now();

        if (context == null) {
            log.warn("PromptContextHolder 비어있음 — moduleName=UNKNOWN으로 저장. @LlmContext 누락 여부 확인 필요");
        }
        String moduleName = context == null ? "UNKNOWN" : context.moduleName();
        int promptVersion = context == null ? 0 : context.promptVersion();
        Map<String, String> vars = context == null ? Map.of() : context.vars();

        LlmCallMetadata metadata;
        if (result == null) {
            metadata = LlmCallMetadata.fail(requestId, failedModel, latency, moduleName, promptVersion, logTime);
        } else {
            metadata = LlmCallMetadata.succeed(requestId, result.model(), (int) result.maxTokens(),
                result.tokenUsage().inputTokens(), result.tokenUsage().outputTokens(), latency,
                promptVersion, moduleName, result.finishReason(), logTime);
        }

        try {
            metadataRepository.save(metadata);
        } catch (Exception e) {
            log.error("llm_call_metadata 저장 실패: requestId={}, module={}, error={}", requestId,
                moduleName, e.getMessage(), e);
        }

        try {
            String inputVarsJson = objectMapper.writeValueAsString(vars);
            String responseText = result == null ? null : result.response();
            detailRepository.save(
                LlmCallDetail.of(requestId, moduleName, inputVarsJson, responseText, logTime));
        } catch (Exception e) {
            log.error("llm_call_detail 저장 실패: requestId={}, module={}, error={}", requestId,
                moduleName, e.getMessage(), e);
        }

        log.info("Gemini API 호출",
            kv("event", "LLM_CALL_COMPLETE"),
            kv("request_id", requestId),
            kv("module", moduleName),
            kv("prompt_version", promptVersion),
            kv("status", result == null ? "FAIL" : "SUCCESS"),
            kv("latency_ms", latency),
            kv("model", result == null ? failedModel : result.model()),
            kv("input_tokens", result == null ? null : result.tokenUsage().inputTokens()),
            kv("output_tokens", result == null ? null : result.tokenUsage().outputTokens()),
            kv("timestamp", logTime));
    }
}
