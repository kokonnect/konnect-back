package com.example.konnect_backend.domain.ai.service.log;

import com.example.konnect_backend.domain.ai.domain.entity.log.AnalysisRequestLog;
import com.example.konnect_backend.domain.ai.domain.vo.PipelineContext;
import com.example.konnect_backend.domain.ai.repository.AnalysisRequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisLogService {

    private final AnalysisRequestLogRepository requestLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long succeed(PipelineContext context, long processingTimeInMillis, LocalDateTime now,
                        Long userId) {
        logRequestProcessingResult("SUCCESS", context, processingTimeInMillis, now);

        AnalysisRequestLog succeededRequest = AnalysisRequestLog.succeed(context.getRequestId(),
            userId, (int) processingTimeInMillis, now);
        AnalysisRequestLog savedRequestLog = requestLogRepository.save(succeededRequest);
        return savedRequestLog.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(PipelineContext context, long processingTimeInMillis, LocalDateTime now, Long userId) {
        logRequestProcessingResult("FAIL", context, processingTimeInMillis, now);

        AnalysisRequestLog failedRequest = AnalysisRequestLog.fail(context.getRequestId(),
            userId,
            (int) processingTimeInMillis, now);
        requestLogRepository.save(failedRequest);
    }

    private void logRequestProcessingResult(String status, PipelineContext context,
                                            long processingTimeInMillis, LocalDateTime timestamp) {
        int inputTokens = context.getInputTokens().get();
        int outputTokens = context.getOutputTokens().get();

        log.info("파이프라인 처리 종료",
            kv("event", "PIPELINE_COMPLETE"),
            kv("status", status),
            kv("latency_ms", processingTimeInMillis),
            kv("input_tokens", inputTokens),
            kv("output_tokens", outputTokens),
            kv("total_tokens", inputTokens + outputTokens),
            kv("timestamp", timestamp));
    }
}
