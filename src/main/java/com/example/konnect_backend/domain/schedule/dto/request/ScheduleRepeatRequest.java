package com.example.konnect_backend.domain.schedule.dto.request;

import com.example.konnect_backend.domain.schedule.entity.status.RepeatEndType;
import com.example.konnect_backend.domain.schedule.entity.status.RepeatType;
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
@Schema(description = "일정 반복 설정 요청")
public class ScheduleRepeatRequest {
    
    @NotNull(message = "반복 타입은 필수입니다.")
    @Schema(description = "반복 타입", example = "WEEKLY", required = true)
    private RepeatType repeatType;
    
    @NotNull(message = "반복 종료 타입은 필수입니다.")
    @Schema(description = "반복 종료 타입", example = "UNTIL_DATE", required = true)
    private RepeatEndType repeatEndType;
    
    @Schema(description = "반복 종료 날짜 (UNTIL_DATE일 때 필수)", example = "2024-12-31T23:59:59")
    private LocalDateTime repeatEndDate;
    
    @Schema(description = "반복 횟수 (COUNT일 때 필수)", example = "10")
    private Long repeatCount;
}