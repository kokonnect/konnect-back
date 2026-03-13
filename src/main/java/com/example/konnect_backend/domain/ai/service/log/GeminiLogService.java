package com.example.konnect_backend.domain.ai.service.log;

import com.example.konnect_backend.domain.ai.aop.PromptContext;
import com.example.konnect_backend.domain.ai.dto.internal.GeminiCallResult;
import com.example.konnect_backend.domain.ai.entity.log.LlmCallMetadata;
import com.example.konnect_backend.domain.ai.repository.LlmCallMetadataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiLogService {

    private final LlmCallMetadataRepository metadataRepository;
    private final ObjectMapper objectMapper;

    @Async("loggingExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(UUID requestId, @Nullable GeminiCallResult result, PromptContext context,
                        int latency) throws JsonProcessingException {
        LocalDateTime logTime = LocalDateTime.now();

        // 메타데이터 DB 로깅
        LlmCallMetadata metadata;
        if (result == null) {
            metadata = LlmCallMetadata.fail(requestId, latency, context.moduleName(), context.promptVersion(),
                logTime);
        } else {
            metadata = LlmCallMetadata.succeed(requestId, result.model(), (int) result.maxTokens(),
                result.tokenUsage().inputTokens(), result.tokenUsage().outputTokens(), latency, context.promptVersion(),
                context.moduleName(), result.finishReason(), logTime);
        }

        LlmCallMetadata saved = metadataRepository.save(metadata);

        // 프롬프트 템플릿 정보 및 입력 변수와 모델 응답을 콘솔(파일)에 로깅한다.
        log.info("메타데이터 ID: {}", saved.getId());
        log.info("모델 응답: {}", result == null ? null : result.response());
        log.info("모듈명: {}", context.moduleName());
        log.info("프롬프트 버전: {}", context.promptVersion());
        log.info("입력 변수: {}", objectMapper.writeValueAsString(context.vars()));
    }
}
