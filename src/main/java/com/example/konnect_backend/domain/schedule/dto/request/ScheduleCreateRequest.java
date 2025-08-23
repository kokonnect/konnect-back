package com.example.konnect_backend.domain.schedule.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일정 생성 요청")
public class ScheduleCreateRequest {
    
    @Schema(description = "자녀 ID (선택)", example = "1")
    private Long childId;
    
    @NotBlank(message = "제목은 필수입니다.")
    @Schema(description = "일정 제목", example = "학부모 상담", required = true)
    private String title;
    
    @Schema(description = "메모", example = "담임 선생님과 상담")
    private String memo;
    
    @NotNull(message = "시작 날짜는 필수입니다.")
    @Schema(description = "시작 날짜/시간", example = "2024-01-15T10:00:00", required = true)
    private LocalDateTime startDate;
    
    @NotNull(message = "종료 날짜는 필수입니다.")
    @Schema(description = "종료 날짜/시간", example = "2024-01-15T11:00:00", required = true)
    private LocalDateTime endDate;
    
    @Schema(description = "종일 일정 여부", example = "false")
    @Builder.Default
    private Boolean isAllDay = false;
    
    @Schema(description = "공지사항으로부터 생성 여부", example = "false")
    @Builder.Default
    private Boolean createdFromNotice = false;
    
    @Valid
    @Schema(description = "반복 설정 (선택)")
    private ScheduleRepeatRequest repeat;
    
    @Valid
    @Schema(description = "알림 설정 목록 (선택)")
    private List<ScheduleAlarmRequest> alarms;
}