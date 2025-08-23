package com.example.konnect_backend.domain.schedule.dto.response;

import com.example.konnect_backend.domain.schedule.entity.ScheduleAlarm;
import com.example.konnect_backend.domain.schedule.entity.status.AlarmTimeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일정 알림 설정 응답")
public class ScheduleAlarmResponse {
    
    @Schema(description = "알림 설정 ID", example = "1")
    private Long id;
    
    @Schema(description = "일정 ID", example = "1")
    private Long scheduleId;
    
    @Schema(description = "알림 타입", example = "BEFORE_1H")
    private AlarmTimeType alarmTimeType;
    
    @Schema(description = "사용자 정의 알림 시간", example = "2024-01-15T09:00:00")
    private LocalDateTime customMinutesBefore;
    
    public static ScheduleAlarmResponse from(ScheduleAlarm scheduleAlarm) {
        return ScheduleAlarmResponse.builder()
                .id(scheduleAlarm.getId())
                .scheduleId(scheduleAlarm.getSchedule().getScheduleId())
                .alarmTimeType(scheduleAlarm.getAlarmTimeType())
                .customMinutesBefore(scheduleAlarm.getCustomMinutesBefore())
                .build();
    }
}