package com.example.konnect_backend.domain.ai.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

@Component
public class LlmHealthTracker {

    public enum StateChange {
        DOWN, UP, NO_CHANGE
    }

    private enum HealthState {
        HEALTHY, DEGRADED
    }

    private final int windowSize;
    private final int recoveryThreshold; // 생성자에 추가
    private final int failureThreshold;

    private final Deque<Boolean> window = new ArrayDeque<>();
    private int failureCount = 0;
    private int consecutiveSuccessCount = 0;

    private HealthState currentState = HealthState.HEALTHY;

    public LlmHealthTracker(@Value("${llmtracker.window-size:5}") int windowSize,
                            @Value("${llmtracker.recovery-threshold:4}") int recoveryThreshold,
                            @Value("${llmtracker.failure-threshold:3}") int failureThreshold) {
        this.windowSize = windowSize;
        this.recoveryThreshold = recoveryThreshold;
        this.failureThreshold = failureThreshold;
    }

    // 병목이 될 가능성이 있다. 성능 저하 시 락 방식 개선 필요.
    public synchronized StateChange recordAndCheck(boolean success) {
        updateWindow(success);

        HealthState nextState = computeNextState();
        StateChange change = resolveStateChange(currentState, nextState);
        currentState = nextState;
        return change;
    }

    private void updateWindow(boolean success) {
        if (window.size() == windowSize) {
            boolean oldest = window.removeFirst();
            if (!oldest) failureCount--;
        }
        window.addLast(success);
        if (!success) {
            failureCount++;
            consecutiveSuccessCount = 0;
        } else {
            consecutiveSuccessCount++;
        }
    }

    private HealthState computeNextState() {
        if (currentState == HealthState.HEALTHY) {
            boolean degraded = window.size() == windowSize && failureCount >= failureThreshold;
            return degraded ? HealthState.DEGRADED : HealthState.HEALTHY;
        } else {
            boolean recovered = consecutiveSuccessCount >= recoveryThreshold;
            return recovered ? HealthState.HEALTHY : HealthState.DEGRADED;
        }
    }

    private StateChange resolveStateChange(HealthState before, HealthState after) {
        if (before == HealthState.HEALTHY && after == HealthState.DEGRADED) return StateChange.DOWN;
        if (before == HealthState.DEGRADED && after == HealthState.HEALTHY) return StateChange.UP;
        return StateChange.NO_CHANGE;
    }
}