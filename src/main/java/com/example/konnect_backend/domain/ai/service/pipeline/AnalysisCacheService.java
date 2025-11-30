package com.example.konnect_backend.domain.ai.service.pipeline;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class AnalysisCacheService {

    private final AtomicLong idGenerator = new AtomicLong(0);

    // 중간 결과 캐시 (30분 유지)
    private final Cache<Long, PipelineContext> contextCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    /**
     * 새로운 분석 ID 생성
     */
    public Long generateAnalysisId() {
        return idGenerator.incrementAndGet();
    }

    /**
     * 파이프라인 컨텍스트 저장
     */
    public void saveContext(Long analysisId, PipelineContext context) {
        contextCache.put(analysisId, context);
        log.debug("컨텍스트 저장: analysisId={}, stage={}", analysisId, context.getCompletedStage());
    }

    /**
     * 파이프라인 컨텍스트 조회
     */
    public PipelineContext getContext(Long analysisId) {
        return contextCache.getIfPresent(analysisId);
    }

    /**
     * 파이프라인 컨텍스트 삭제
     */
    public void removeContext(Long analysisId) {
        contextCache.invalidate(analysisId);
        log.debug("컨텍스트 삭제: analysisId={}", analysisId);
    }

    /**
     * 컨텍스트 존재 여부 확인
     */
    public boolean hasContext(Long analysisId) {
        return contextCache.getIfPresent(analysisId) != null;
    }
}
