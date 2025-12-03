package com.example.konnect_backend.domain.notification.controller;

import com.example.konnect_backend.domain.notification.dto.request.FcmTokenRequest;
import com.example.konnect_backend.domain.notification.dto.response.NotificationListResponse;
import com.example.konnect_backend.domain.notification.dto.response.UnreadCountResponse;
import com.example.konnect_backend.domain.notification.service.NotificationService;
import com.example.konnect_backend.global.ApiResponse;
import com.example.konnect_backend.global.security.SecurityUtil;
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

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/fcm-token")
    @Operation(summary = "FCM 토큰 등록", description = "푸시 알림을 위한 FCM 토큰을 등록합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<String> registerFcmToken(@Valid @RequestBody FcmTokenRequest fcmTokenRequest) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        notificationService.registerFcmToken(userId, fcmTokenRequest);
        return ApiResponse.onSuccess("FCM 토큰이 등록되었습니다.");
    }

    @DeleteMapping("/fcm-token")
    @Operation(summary = "FCM 토큰 삭제", description = "로그아웃 시 FCM 토큰을 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<String> removeFcmToken(
            @Parameter(description = "삭제할 FCM 토큰", required = true)
            @RequestParam String token) {
        notificationService.removeFcmToken(token);
        return ApiResponse.onSuccess("FCM 토큰이 삭제되었습니다.");
    }

    @GetMapping
    @Operation(summary = "알림 목록 조회", description = "사용자의 알림 목록을 페이징하여 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<NotificationListResponse> getNotifications(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        return ApiResponse.onSuccess(notificationService.getNotifications(userId, page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "읽지 않은 알림 개수 조회", description = "읽지 않은 알림의 개수를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<UnreadCountResponse> getUnreadCount() {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        return ApiResponse.onSuccess(notificationService.getUnreadCount(userId));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<String> markAsRead(
            @Parameter(description = "알림 ID", required = true, example = "1")
            @PathVariable Long notificationId) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        notificationService.markAsRead(userId, notificationId);
        return ApiResponse.onSuccess("알림이 읽음 처리되었습니다.");
    }

    @PatchMapping("/read-all")
    @Operation(summary = "모든 알림 읽음 처리", description = "사용자의 모든 알림을 읽음 처리합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<String> markAllAsRead() {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        notificationService.markAllAsRead(userId);
        return ApiResponse.onSuccess("모든 알림이 읽음 처리되었습니다.");
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<String> deleteNotification(
            @Parameter(description = "알림 ID", required = true, example = "1")
            @PathVariable Long notificationId) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        notificationService.deleteNotification(userId, notificationId);
        return ApiResponse.onSuccess("알림이 삭제되었습니다.");
    }
}
