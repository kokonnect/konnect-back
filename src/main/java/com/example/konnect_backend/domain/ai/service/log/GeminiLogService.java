package com.example.konnect_backend.domain.ai.service.log;

import com.example.konnect_backend.domain.ai.aop.PromptContext;
import com.example.konnect_backend.domain.ai.dto.internal.GeminiCallResult;
import com.example.konnect_backend.domain.ai.domain.entity.log.LlmCallMetadata;
import com.example.konnect_backend.domain.ai.repository.LlmCallMetadataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(UUID requestId, @Nullable GeminiCallResult result, PromptContext context,
                        int latency) throws JsonProcessingException {
        LocalDateTime logTime = LocalDateTime.now();

        // 프롬프트 모듈 외부에서 호출되는 경우에 대한 방어 코드
        String moduleName = context == null ? "UNKNOWN" : context.moduleName();
        int promptVersion = context == null ? 0 : context.promptVersion();
        Map<String, String> vars = context == null ? Map.of() : context.vars();

        // 메타데이터 DB 로깅
        LlmCallMetadata metadata;
        if (result == null) {
            metadata = LlmCallMetadata.fail(requestId, latency, moduleName, promptVersion,
                logTime);
        } else {
            metadata = LlmCallMetadata.succeed(requestId, result.model(), (int) result.maxTokens(),
                result.tokenUsage().inputTokens(), result.tokenUsage().outputTokens(), latency,
                promptVersion,
                moduleName, result.finishReason(), logTime);
        }

        LlmCallMetadata saved = metadataRepository.save(metadata);

        // 프롬프트 템플릿 정보 및 입력 변수와 모델 응답 로깅
        log.info("Gemini API 호출 완료",
            kv("metadata id", saved.getId()),
            kv("model response", result == null ? null : result.response()),
            kv("module name", moduleName),
            kv("prompt version", promptVersion),
            kv("input vars", objectMapper.writeValueAsString(vars)),
            kv("timestamp", logTime)
        );
    }
}
