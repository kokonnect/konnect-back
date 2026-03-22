package com.example.konnect_backend.domain.auth.service;

import com.example.konnect_backend.domain.ai.repository.AnalysisHistoryRepository;
import com.example.konnect_backend.domain.message.entity.UserGeneratedMessage;
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

        Device device = deviceRepository.findById(deviceUuid)
                .orElseGet(() ->
                        deviceRepository.save(
                                Device.builder()
                                        .deviceUuid(deviceUuid)
                                        .build()
                        )
                );

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("user not found"));


        // 무조건 최신 유저로 업데이트
        device.updateUser(targetUser);

        // 데이터 이전
        messageRepository.migrateGuestToUser(targetUser, deviceUuid);
        analysisHistoryRepository.migrateGuestToUser(targetUser.getId(), deviceUuid);
    }
}