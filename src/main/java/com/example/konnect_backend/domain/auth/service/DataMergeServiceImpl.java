package com.example.konnect_backend.domain.auth.service;

import com.example.konnect_backend.domain.ai.repository.AnalysisHistoryRepository;
import com.example.konnect_backend.domain.message.repository.UserGeneratedMessageRepository;
import com.example.konnect_backend.domain.user.entity.Device;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.repository.DeviceRepository;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DataMergeServiceImpl implements DataMergeService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final UserGeneratedMessageRepository messageRepository;
    private final AnalysisHistoryRepository analysisHistoryRepository;

    @Override
    public void mergeGuestToUser(String deviceUuid, Long userId) {
        if (deviceUuid == null || deviceUuid.isBlank()) {
            return; // or throw GeneralException
        }

        Device device = deviceRepository.findById(deviceUuid)
                .orElseGet(() ->
                        deviceRepository.save(
                                Device.builder()
                                        .deviceUuid(deviceUuid)
                                        .build()
                        )
                );

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 게스트에서 선택한 언어를 최초 1회만 회원에게 승계
        if (targetUser.getLanguage() == null && device.getLanguage() != null) {
            targetUser.updateLanguage(device.getLanguage());
        }

        // 현재 디바이스는 최신 로그인 유저에 연결
        device.updateUser(targetUser);

        // 데이터 이전
        messageRepository.migrateGuestToUser(targetUser, deviceUuid);
        analysisHistoryRepository.migrateGuestToUser(targetUser.getId(), deviceUuid);
    }
}