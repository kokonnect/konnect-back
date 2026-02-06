package com.example.konnect_backend.domain.ai.service;

import com.example.konnect_backend.domain.ai.config.GeminiConfig;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gemini API 호출 제한 관리 서비스
 *
 * ## 일일 호출 제한 (RPD: Requests Per Day)
 * - gemini-2.0-flash (primary): 200회/일
 * - gemini-2.0-flash-lite (lite): 1,000회/일
 *
 * ## 전략
 * 1. 캐시로 일일 호출 횟수 추적 (자정에 리셋)
 * 2. Primary 모델 제한 도달 시 Lite 모델로 폴백
 * 3. 모든 제한 도달 시 예외 발생
 */
@Service
@Slf4j
public class GeminiRateLimitService {

    private final GeminiConfig config;

    // 일일 호출 카운터 캐시 (24시간 후 만료)
    private final Cache<String, AtomicInteger> dailyCounterCache;

    public GeminiRateLimitService(GeminiConfig config) {
        this.config = config;
        this.dailyCounterCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(24))
                .maximumSize(10)
                .build();
    }

    /**
     * Primary 모델 호출 가능 여부 확인
     */
    public boolean canUsePrimaryModel() {
        int count = getCurrentCount(ModelType.PRIMARY);
        return count < config.getLimit().getPrimary().getRpd();
    }

    /**
     * Lite 모델 호출 가능 여부 확인
     */
    public boolean canUseLiteModel() {
        int count = getCurrentCount(ModelType.LITE);
        return count < config.getLimit().getLite().getRpd();
    }

    /**
     * 사용 가능한 최적의 모델 반환
     * - Primary 사용 가능하면 Primary
     * - Primary 제한 시 Lite로 폴백
     * - 모두 제한 시 null
     */
    public String getAvailableModel(boolean preferPrimary) {
        if (preferPrimary && canUsePrimaryModel()) {
            return config.getModel().getPrimary();
        }
        if (canUseLiteModel()) {
            return config.getModel().getLite();
        }
        if (canUsePrimaryModel()) {
            return config.getModel().getPrimary();
        }
        log.error("모든 Gemini 모델의 일일 호출 제한에 도달했습니다");
        return null;
    }

    /**
     * Vision 모델 반환 (이미지 분석용)
     * Vision은 Primary 모델만 지원
     */
    public String getVisionModel() {
        if (!canUsePrimaryModel()) {
            log.warn("Vision 모델 (Primary) 일일 제한 도달");
            return null;
        }
        return config.getModel().getVision();
    }

    /**
     * 호출 기록 (성공 시 호출)
     */
    public void recordUsage(String modelName) {
        ModelType type = getModelType(modelName);
        incrementCount(type);
        log.debug("Gemini API 호출 기록: model={}, 일일횟수={}/{}",
                modelName,
                getCurrentCount(type),
                type == ModelType.PRIMARY ? config.getLimit().getPrimary().getRpd() : config.getLimit().getLite().getRpd());
    }

    /**
     * 현재 사용량 조회
     */
    public UsageStatus getUsageStatus() {
        int primaryCount = getCurrentCount(ModelType.PRIMARY);
        int liteCount = getCurrentCount(ModelType.LITE);

        return new UsageStatus(
                primaryCount,
                config.getLimit().getPrimary().getRpd(),
                liteCount,
                config.getLimit().getLite().getRpd()
        );
    }

    private int getCurrentCount(ModelType type) {
        String key = getCacheKey(type);
        AtomicInteger counter = dailyCounterCache.getIfPresent(key);
        return counter != null ? counter.get() : 0;
    }

    private void incrementCount(ModelType type) {
        String key = getCacheKey(type);
        AtomicInteger counter = dailyCounterCache.get(key, k -> new AtomicInteger(0));
        counter.incrementAndGet();
    }

    private String getCacheKey(ModelType type) {
        return type.name() + "_" + LocalDate.now().toString();
    }

    private ModelType getModelType(String modelName) {
        if (modelName.contains("lite")) {
            return ModelType.LITE;
        }
        return ModelType.PRIMARY;
    }

    public enum ModelType {
        PRIMARY, LITE
    }

    public record UsageStatus(
            int primaryUsed,
            int primaryLimit,
            int liteUsed,
            int liteLimit
    ) {
        public int primaryRemaining() {
            return primaryLimit - primaryUsed;
        }

        public int liteRemaining() {
            return liteLimit - liteUsed;
        }

        @Override
        public String toString() {
            return String.format("Primary: %d/%d (남은: %d), Lite: %d/%d (남은: %d)",
                    primaryUsed, primaryLimit, primaryRemaining(),
                    liteUsed, liteLimit, liteRemaining());
        }
    }
}
