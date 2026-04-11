package com.example.konnect_backend.domain.user.service;

import com.example.konnect_backend.domain.user.entity.Device;
import com.example.konnect_backend.domain.user.entity.status.Language;
import com.example.konnect_backend.domain.user.repository.DeviceRepository;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerDevice(String deviceUuid, Language language) {
        validateDeviceUuid(deviceUuid);

        deviceRepository.findById(deviceUuid)
                .map(device -> {
                    if (language != null) {
                        device.updateLanguage(language);
                    }
                    return device;
                })
                .orElseGet(() ->
                        deviceRepository.save(
                                Device.builder()
                                        .deviceUuid(deviceUuid)
                                        .language(language)
                                        .createdAt(LocalDateTime.now())
                                        .lastUsedAt(LocalDateTime.now())
                                        .build()
                        )
                );
    }

    @Transactional
    public Device findOrCreateDevice(String deviceUuid) {
        validateDeviceUuid(deviceUuid);

        return deviceRepository.findById(deviceUuid)
                .orElseGet(() ->
                        deviceRepository.save(
                                Device.builder()
                                        .deviceUuid(deviceUuid)
                                        .createdAt(LocalDateTime.now())
                                        .lastUsedAt(LocalDateTime.now())
                                        .build()
                        )
                );
    }

    @Transactional
    public void updateLanguage(String deviceUuid, Language language) {
        validateDeviceUuid(deviceUuid);

        Device device = findOrCreateDevice(deviceUuid);
        device.updateLanguage(language);
    }

    @Transactional(readOnly = true)
    public Language getLanguage(String deviceUuid) {
        validateDeviceUuid(deviceUuid);

        return deviceRepository.findById(deviceUuid)
                .map(Device::getLanguage)
                .orElse(null);
    }

    private void validateDeviceUuid(String deviceUuid) {
        if (deviceUuid == null || deviceUuid.isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_DEVICE);
        }
    }
}