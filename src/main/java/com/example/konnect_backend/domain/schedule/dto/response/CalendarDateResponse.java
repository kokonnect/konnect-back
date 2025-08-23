package com.example.konnect_backend.domain.schedule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "달력 날짜별 일정 존재 여부 응답")
public class CalendarDateResponse {
    
    @Schema(description = "날짜", example = "2024-01-15")
    private LocalDate date;
    
    @Schema(description = "해당 날짜에 일정 존재 여부", example = "true")
    private Boolean hasSchedule;
    
    @Schema(description = "해당 날짜 일정 개수", example = "3")
    private Integer scheduleCount;
    
    @Schema(description = "오늘 날짜 여부", example = "true")
    private Boolean isToday;
    
    public static CalendarDateResponse of(LocalDate date, Integer scheduleCount) {
        LocalDate today = LocalDate.now();
        return CalendarDateResponse.builder()
                .date(date)
                .hasSchedule(scheduleCount > 0)
                .scheduleCount(scheduleCount)
                .isToday(date.equals(today))
                .build();
    }
}