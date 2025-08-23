package com.example.konnect_backend.domain.schedule.dto.response;

import com.example.konnect_backend.domain.schedule.entity.Schedule;
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
@Schema(description = "일정 응답")
public class ScheduleResponse {
    
    @Schema(description = "일정 ID", example = "1")
    private Long scheduleId;
    
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    @Schema(description = "자녀 ID", example = "1")
    private Long childId;
    
    @Schema(description = "자녀 이름", example = "김철수")
    private String childName;
    
    @Schema(description = "일정 제목", example = "학부모 상담")
    private String title;
    
    @Schema(description = "메모", example = "담임 선생님과 상담")
    private String memo;
    
    @Schema(description = "시작 날짜/시간", example = "2024-01-15T10:00:00")
    private LocalDateTime startDate;
    
    @Schema(description = "종료 날짜/시간", example = "2024-01-15T11:00:00")
    private LocalDateTime endDate;
    
    @Schema(description = "종일 일정 여부", example = "false")
    private Boolean isAllDay;
    
    @Schema(description = "공지사항으로부터 생성 여부", example = "false")
    private Boolean createdFromNotice;
    
    @Schema(description = "생성일시", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정일시", example = "2024-01-01T00:00:00")
    private LocalDateTime updatedAt;
    
    public static ScheduleResponse from(Schedule schedule) {
        return ScheduleResponse.builder()
                .scheduleId(schedule.getScheduleId())
                .userId(schedule.getUser().getId())
                .childId(schedule.getChild() != null ? schedule.getChild().getId() : null)
                .childName(schedule.getChild() != null ? schedule.getChild().getName() : null)
                .title(schedule.getTitle())
                .memo(schedule.getMemo())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .isAllDay(schedule.getIsAllDay())
                .createdFromNotice(schedule.getCreatedFromNotice())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}