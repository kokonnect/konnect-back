package com.example.konnect_backend.domain.schedule.controller;

import com.example.konnect_backend.domain.schedule.dto.request.ScheduleCreateRequest;
import com.example.konnect_backend.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.example.konnect_backend.domain.schedule.dto.response.ScheduleResponse;
import com.example.konnect_backend.domain.schedule.service.ScheduleService;
import com.example.konnect_backend.global.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedule", description = "일정 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class ScheduleController {
    
    private final ScheduleService scheduleService;
    
    @PostMapping
    @Operation(summary = "일정 등록", description = "새로운 일정을 등록합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "일정 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<ScheduleResponse> createSchedule(@Valid @RequestBody ScheduleCreateRequest request) {
        return ApiResponse.onSuccess(scheduleService.createSchedule(request));
    }
    
    @PutMapping("/{scheduleId}")
    @Operation(summary = "일정 수정", description = "기존 일정을 수정합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "일정 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<ScheduleResponse> updateSchedule(
            @Parameter(description = "일정 ID", required = true, example = "1")
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleUpdateRequest request) {
        return ApiResponse.onSuccess(scheduleService.updateSchedule(scheduleId, request));
    }
    
    @DeleteMapping("/{scheduleId}")
    @Operation(summary = "일정 삭제", description = "일정을 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "일정 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<String> deleteSchedule(
            @Parameter(description = "일정 ID", required = true, example = "1")
            @PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ApiResponse.onSuccess("일정이 삭제되었습니다.");
    }
    
    @GetMapping("/monthly")
    @Operation(summary = "월별 일정 조회", description = "특정 연월의 일정을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "일정 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<List<ScheduleResponse>> getMonthlySchedules(
            @Parameter(description = "연도", required = true, example = "2024")
            @RequestParam int year,
            @Parameter(description = "월", required = true, example = "1")
            @RequestParam int month) {
        return ApiResponse.onSuccess(scheduleService.getMonthlySchedules(year, month));
    }
    
    @GetMapping("/recent")
    @Operation(summary = "최근 일정 조회", description = "메인 화면에 표시할 최근 일정을 조회합니다. 기본값은 5개입니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "일정 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<List<ScheduleResponse>> getRecentSchedules(
            @Parameter(description = "조회할 일정 개수", example = "5")
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.onSuccess(scheduleService.getRecentSchedules(limit));
    }
}