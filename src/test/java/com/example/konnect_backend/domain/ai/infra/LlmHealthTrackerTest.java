package com.example.konnect_backend.domain.ai.infra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.example.konnect_backend.domain.ai.infra.LlmHealthTracker.StateChange.*;
import static org.assertj.core.api.Assertions.assertThat;

class LlmHealthTrackerTest {

    LlmHealthTracker tracker = new LlmHealthTracker(5, 3, 3);

    @Test
    @DisplayName("실패가 임계치에 도달하면 DOWN으로 전환된다")
    void Should_ReturnDown_When_FailureThresholdReached() {
        record(true, true);
        assertThat(record(false, false, false)).containsExactly(
            NO_CHANGE, NO_CHANGE, DOWN
        );
    }

    @Test
    @DisplayName("DOWN 상태에서 연속 성공이 recoveryThreshold 미만이면 UP이 아니다")
    void Should_NotReturnUp_When_ConsecutiveSuccessBelowRecoveryThreshold() {
        degradeToDown();
        assertThat(record(true, true)).allMatch(c -> c == NO_CHANGE);
    }

    @Test
    @DisplayName("DOWN 상태에서 연속 성공이 recoveryThreshold 이상이면 UP으로 전환된다")
    void Should_ReturnUp_When_ConsecutiveSuccessReachesRecoveryThreshold() {
        degradeToDown();
        record(true, true);
        assertThat(tracker.recordAndCheck(true)).isEqualTo(UP);
    }

    @Test
    @DisplayName("UP 복구 직후 단일 실패로 DOWN이 되지 않는다")
    void Should_NotReturnDown_When_SingleFailureAfterRecovery() {
        degradeToDown();
        record(true, true, true);
        assertThat(tracker.recordAndCheck(false)).isEqualTo(NO_CHANGE);
    }

    private void degradeToDown() {
        record(true, true, false, false, false);
    }

    private List<LlmHealthTracker.StateChange> record(Boolean... results) {
        return Arrays.stream(results)
            .map(tracker::recordAndCheck)
            .toList();
    }
}