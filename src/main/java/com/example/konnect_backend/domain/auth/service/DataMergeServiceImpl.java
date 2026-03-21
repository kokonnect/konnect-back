package com.example.konnect_backend.domain.auth.service;

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
    // TODO: 옮겨야할 데이터 repository들 추가

    @Override
    public void mergeGuestToUser(String deviceUuid, Long userId) {

        Device device = deviceRepository.findById(deviceUuid)
                .orElseThrow(() -> new GeneralException(ErrorStatus.INVALID_DEVICE));

        User guestUser = device.getUser();

        //  이미 로그인된 유저면 스킵
        if (guestUser == null || !guestUser.isGuest()) {
            return;
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("user not found"));


        // device user 변경
        device.updateUser(targetUser);

        // guest 유저 정리 (선택)
        userRepository.delete(guestUser);
    }
}