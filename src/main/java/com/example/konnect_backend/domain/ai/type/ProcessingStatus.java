package com.example.konnect_backend.domain.ai.type;

public enum ProcessingStatus {
    COMPLETED,      // 모든 단계 완료
    PARTIAL,        // 일부 단계만 완료 (재시도 가능)
    FAILED          // 실패
}