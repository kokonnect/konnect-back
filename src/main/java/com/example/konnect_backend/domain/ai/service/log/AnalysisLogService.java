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
@Slf4j
@RequiredArgsConstructor
public class AnalysisLogService {

    private static final Logger jsonLogger = LoggerFactory.getLogger(
        "com.example.konnect_backend.domain.ai.service.log.json");

    private final AnalysisRequestLogRepository requestLogRepository;

    @Transactional
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
        UUID requestId = context.getRequestId();
        int inputTokens = context.getInputTokens().get();
        int outputTokens = context.getOutputTokens().get();

        double processingTimeInSeconds = processingTimeInMillis / 1000.0;

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📊 파이프라인 처리 종료: {}", status);
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("   요청 ID: {}", requestId);
        log.info("   처리 시간: {}ms ({}초)", processingTimeInMillis,
            String.format("%.1f", processingTimeInSeconds));
        log.info("─────────────────────토큰 사용량 요약──────────────────────────");
        log.info("   입력 토큰 (Input):  {}", String.format("%,d", inputTokens));
        log.info("   출력 토큰 (Output): {}", String.format("%,d", outputTokens));
        log.info("   총 토큰 (Total):    {}", String.format("%,d", inputTokens + outputTokens));
        log.info("═══════════════════════════════════════════════════════════════");

        jsonLogger.info("파이프라인 처리 종료", kv("status", status), kv("request id", requestId),
            kv("processing time in millis", processingTimeInMillis),
            kv("input tokens", inputTokens), kv("output tokens", outputTokens),
            kv("total tokens", inputTokens + outputTokens), kv("timestamp", timestamp));
    }
}
