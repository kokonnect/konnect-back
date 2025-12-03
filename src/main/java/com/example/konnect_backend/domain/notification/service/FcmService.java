package com.example.konnect_backend.domain.notification.service;

import com.example.konnect_backend.domain.notification.entity.FcmToken;
import com.example.konnect_backend.domain.notification.entity.Notification;
import com.example.konnect_backend.domain.notification.repository.FcmTokenRepository;
import com.example.konnect_backend.domain.user.entity.User;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FirebaseMessaging firebaseMessaging;
    private final FcmTokenRepository fcmTokenRepository;

    /**
     * 단일 사용자에게 푸시 알림 발송
     */
    public boolean sendNotification(User user, Notification notification) {
        if (firebaseMessaging == null) {
            log.warn("FirebaseMessaging이 초기화되지 않아 알림을 발송할 수 없습니다.");
            return false;
        }

        List<FcmToken> tokens = fcmTokenRepository.findByUserAndIsActiveTrue(user);
        if (tokens.isEmpty()) {
            log.debug("사용자 {}에게 활성화된 FCM 토큰이 없습니다.", user.getId());
            return false;
        }

        List<String> tokenStrings = tokens.stream()
                .map(FcmToken::getToken)
                .toList();

        return sendMulticast(tokenStrings, notification);
    }

    /**
     * 단일 토큰으로 알림 발송
     */
    public boolean sendToToken(String token, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.warn("FirebaseMessaging이 초기화되지 않아 알림을 발송할 수 없습니다.");
            return false;
        }

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data != null ? data : new HashMap<>())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .setBadge(1)
                                    .build())
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .setPriority(AndroidNotification.Priority.HIGH)
                                    .build())
                            .build())
                    .build();

            String response = firebaseMessaging.send(message);
            log.debug("FCM 알림 발송 성공: {}", response);
            return true;

        } catch (FirebaseMessagingException e) {
            handleFcmException(token, e);
            return false;
        }
    }

    /**
     * 여러 토큰에 멀티캐스트 발송
     */
    private boolean sendMulticast(List<String> tokens, Notification notification) {
        if (tokens.isEmpty()) {
            return false;
        }

        Map<String, String> data = new HashMap<>();
        data.put("type", notification.getType().name());
        if (notification.getReferenceId() != null) {
            data.put("referenceId", notification.getReferenceId().toString());
        }
        data.put("notificationId", notification.getId().toString());

        try {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(notification.getTitle())
                            .setBody(notification.getBody())
                            .build())
                    .putAllData(data)
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .setBadge(1)
                                    .build())
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .setPriority(AndroidNotification.Priority.HIGH)
                                    .build())
                            .build())
                    .build();

            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("FCM 멀티캐스트 발송 결과: 성공 {}, 실패 {}",
                    response.getSuccessCount(), response.getFailureCount());

            // 실패한 토큰 처리
            handleFailedTokens(tokens, response);

            return response.getSuccessCount() > 0;

        } catch (FirebaseMessagingException e) {
            log.error("FCM 멀티캐스트 발송 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 실패한 토큰 처리 (비활성화 또는 삭제)
     */
    private void handleFailedTokens(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        List<String> tokensToDeactivate = new ArrayList<>();

        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {
                FirebaseMessagingException exception = responses.get(i).getException();
                if (exception != null) {
                    MessagingErrorCode errorCode = exception.getMessagingErrorCode();
                    if (errorCode == MessagingErrorCode.UNREGISTERED ||
                            errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                        tokensToDeactivate.add(tokens.get(i));
                    }
                }
            }
        }

        // 유효하지 않은 토큰 비활성화
        for (String token : tokensToDeactivate) {
            fcmTokenRepository.findByToken(token).ifPresent(fcmToken -> {
                fcmToken.deactivate();
                fcmTokenRepository.save(fcmToken);
                log.info("유효하지 않은 FCM 토큰 비활성화: {}", token.substring(0, 20) + "...");
            });
        }
    }

    /**
     * FCM 예외 처리
     */
    private void handleFcmException(String token, FirebaseMessagingException e) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();
        log.error("FCM 발송 실패 - ErrorCode: {}, Message: {}", errorCode, e.getMessage());

        if (errorCode == MessagingErrorCode.UNREGISTERED ||
                errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            fcmTokenRepository.findByToken(token).ifPresent(fcmToken -> {
                fcmToken.deactivate();
                fcmTokenRepository.save(fcmToken);
                log.info("유효하지 않은 FCM 토큰 비활성화: {}", token.substring(0, 20) + "...");
            });
        }
    }
}
