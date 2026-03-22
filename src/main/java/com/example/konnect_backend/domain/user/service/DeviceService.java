package com.example.konnect_backend.domain.user.service;

import com.example.konnect_backend.domain.user.entity.Device;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.entity.status.Language;
import com.example.konnect_backend.domain.user.repository.DeviceRepository;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerDevice(String deviceUuid, Language language) {
        if (deviceUuid == null || deviceUuid.isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_DEVICE);
        }

        deviceRepository.findById(deviceUuid)
                .map(device -> {
                    // 이미 존재하면 language 업데이트
                    if ( language != null) {
                        device.updateLanguage(language);
                    }
                    return device;
                })
                .orElseGet(() ->
                        deviceRepository.save(
                                Device.builder()
                                        .deviceUuid(deviceUuid)
                                        .language(language) // 추가
                                        .build()
                        )
                );
    }


    @Transactional
    public Device findOrCreateDevice(String deviceUuid) {

        return deviceRepository.findById(deviceUuid)
                .orElseGet(() ->
                        deviceRepository.save(
                                Device.builder()
                                        .deviceUuid(deviceUuid)
                                        .build()
                        )
                );
    }

    @Transactional
    public void connectDevice(Long userId, String deviceUuid) {

        if (userId == null && (deviceUuid == null || deviceUuid.isBlank())) {
            throw new GeneralException(ErrorStatus.INVALID_DEVICE);
        }

        Device device = findOrCreateDevice(deviceUuid);

        User user = userRepository.findById(userId).orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        if (device.getUser() == null || device.getUser().isGuest()) {
            device.updateUser(user);
        }
    }


}