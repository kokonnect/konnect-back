package com.example.konnect_backend.domain.notification.service;

import com.example.konnect_backend.domain.notification.dto.request.FcmTokenRequest;
import com.example.konnect_backend.domain.notification.dto.response.NotificationListResponse;
import com.example.konnect_backend.domain.notification.dto.response.NotificationResponse;
import com.example.konnect_backend.domain.notification.dto.response.UnreadCountResponse;
import com.example.konnect_backend.domain.notification.entity.FcmToken;
import com.example.konnect_backend.domain.notification.entity.Notification;
import com.example.konnect_backend.domain.notification.entity.NotificationType;
import com.example.konnect_backend.domain.notification.repository.FcmTokenRepository;
import com.example.konnect_backend.domain.notification.repository.NotificationRepository;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    /**
     * FCM 토큰 등록/갱신
     */
    @Transactional
    public void registerFcmToken(Long userId, FcmTokenRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 동일한 토큰이 이미 존재하는지 확인
        if (fcmTokenRepository.existsByToken(request.getToken())) {
            fcmTokenRepository.findByToken(request.getToken()).ifPresent(existingToken -> {
                // 다른 사용자의 토큰이면 이전 사용자에서 삭제
                if (!existingToken.getUser().getId().equals(userId)) {
                    fcmTokenRepository.delete(existingToken);
                    createNewToken(user, request);
                } else {
                    // 같은 사용자면 활성화만
                    existingToken.activate();
                    fcmTokenRepository.save(existingToken);
                }
            });
            return;
        }

        // deviceId가 있으면 기존 토큰 업데이트
        if (request.getDeviceId() != null) {
            fcmTokenRepository.findByUserAndDeviceId(user, request.getDeviceId())
                    .ifPresentOrElse(
                            existingToken -> {
                                existingToken.updateToken(request.getToken());
                                existingToken.activate();
                                fcmTokenRepository.save(existingToken);
                            },
                            () -> createNewToken(user, request)
                    );
        } else {
            createNewToken(user, request);
        }

        log.info("FCM 토큰 등록 완료 - userId: {}", userId);
    }

    private void createNewToken(User user, FcmTokenRequest request) {
        FcmToken token = FcmToken.builder()
                .user(user)
                .token(request.getToken())
                .deviceId(request.getDeviceId())
                .deviceType(request.getDeviceType())
                .isActive(true)
                .build();
        fcmTokenRepository.save(token);
    }

    /**
     * FCM 토큰 삭제 (로그아웃 시)
     */
    @Transactional
    public void removeFcmToken(String token) {
        fcmTokenRepository.deleteByToken(token);
        log.info("FCM 토큰 삭제 완료");
    }

    /**
     * 알림 생성 및 발송
     */
    @Transactional
    public NotificationResponse createAndSendNotification(
            Long userId,
            String title,
            String body,
            NotificationType type,
            Long referenceId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .body(body)
                .type(type)
                .referenceId(referenceId)
                .isRead(false)
                .isSent(false)
                .build();

        notification = notificationRepository.save(notification);

        // FCM 푸시 알림 발송
        boolean sent = fcmService.sendNotification(user, notification);
        if (sent) {
            notification.markAsSent();
            notificationRepository.save(notification);
        }

        return NotificationResponse.from(notification);
    }

    /**
     * 알림 목록 조회
     */
    public NotificationListResponse getNotifications(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage = notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        long unreadCount = notificationRepository.countByUserAndIsReadFalse(user);

        List<NotificationResponse> notifications = notificationPage.getContent().stream()
                .map(NotificationResponse::from)
                .toList();

        return NotificationListResponse.builder()
                .notifications(notifications)
                .unreadCount(unreadCount)
                .page(page)
                .size(size)
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .hasNext(notificationPage.hasNext())
                .build();
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    public UnreadCountResponse getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        long count = notificationRepository.countByUserAndIsReadFalse(user);
        return UnreadCountResponse.builder()
                .unreadCount(count)
                .build();
    }

    /**
     * 단일 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(userId)) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        notificationRepository.markAllAsReadByUser(user);
    }

    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(userId)) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }

        notificationRepository.delete(notification);
    }
}
