package com.example.konnect_backend.domain.schedule.dto.response;

import com.example.konnect_backend.domain.schedule.entity.ScheduleRepeat;
import com.example.konnect_backend.domain.schedule.entity.status.RepeatEndType;
import com.example.konnect_backend.domain.schedule.entity.status.RepeatType;
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
@Schema(description = "일정 반복 설정 응답")
public class ScheduleRepeatResponse {
    
    @Schema(description = "반복 설정 ID", example = "1")
    private Long id;
    
    @Schema(description = "일정 ID", example = "1")
    private Long scheduleId;
    
    @Schema(description = "반복 타입", example = "WEEKLY")
    private RepeatType repeatType;
    
    @Schema(description = "반복 종료 타입", example = "UNTIL_DATE")
    private RepeatEndType repeatEndType;
    
    @Schema(description = "반복 종료 날짜", example = "2024-12-31T23:59:59")
    private LocalDateTime repeatEndDate;
    
    @Schema(description = "반복 횟수", example = "10")
    private Long repeatCount;
    
    public static ScheduleRepeatResponse from(ScheduleRepeat scheduleRepeat) {
        return ScheduleRepeatResponse.builder()
                .id(scheduleRepeat.getId())
                .scheduleId(scheduleRepeat.getSchedule().getScheduleId())
                .repeatType(scheduleRepeat.getRepeatType())
                .repeatEndType(scheduleRepeat.getRepeatEndType())
                .repeatEndDate(scheduleRepeat.getRepeatEndDate())
                .repeatCount(scheduleRepeat.getRepeatCount())
                .build();
    }
}