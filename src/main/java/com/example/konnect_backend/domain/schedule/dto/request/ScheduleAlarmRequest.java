package com.example.konnect_backend.domain.schedule.dto.request;

import com.example.konnect_backend.domain.schedule.entity.status.AlarmTimeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일정 알림 설정 요청")
public class ScheduleAlarmRequest {
    
    @NotNull(message = "알림 타입은 필수입니다.")
    @Schema(description = "알림 타입", example = "BEFORE_1H", required = true)
    private AlarmTimeType alarmTimeType;
    
    @Schema(description = "사용자 정의 알림 시간 (CUSTOM일 때 필수)", example = "2024-01-15T09:00:00")
    private LocalDateTime customMinutesBefore;
}