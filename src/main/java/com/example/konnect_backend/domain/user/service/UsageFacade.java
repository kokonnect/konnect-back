package com.example.konnect_backend.domain.user.service;

import com.example.konnect_backend.domain.user.component.UsagePolicy;
import com.example.konnect_backend.domain.user.dto.UsageResponse;
import com.example.konnect_backend.domain.user.entity.Device;
import com.example.konnect_backend.domain.user.entity.Usage;
import com.example.konnect_backend.domain.user.entity.status.IdentityType;
import com.example.konnect_backend.domain.user.entity.status.UsageType;
import com.example.konnect_backend.domain.user.repository.UsageRepository;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.security.SecurityUtil;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UsageFacade {

    private final UsageService usageService;
    private final UsagePolicy usagePolicy;
    private final DeviceService deviceService;
    private final UsageRepository usageRepository;

    @Transactional
    public void validateAndIncrease(UsageType usageType, String deviceUuid) {

        Long userId = SecurityUtil.getCurrentUserIdOrNull();

        IdentityType identityType;
        String identityKey;
        boolean isGuest;

        if (userId != null) {
            identityType = IdentityType.USER;
            identityKey = String.valueOf(userId);
            isGuest = false;
        } else {
            if (deviceUuid == null || deviceUuid.isBlank()) {
                throw new GeneralException(ErrorStatus.INVALID_DEVICE);
            }
            Device device = deviceService.findOrCreateDevice(deviceUuid);

            identityType = IdentityType.DEVICE;
            identityKey = device.getDeviceUuid();
            isGuest = true;
        }

        int limit = usagePolicy.getLimit(isGuest, usageType);

        boolean allowed = usageService.checkAndIncreaseUsage(
                identityType,
                identityKey,
                usageType,
                limit
        );

        if (!allowed) {
            throw new GeneralException(ErrorStatus.USAGE_LIMIT_EXCEEDED);
        }
    }


    @Transactional(readOnly = true)
    public UsageResponse getUsage(String deviceUuid) {

        Long userId = SecurityUtil.getCurrentUserIdOrNull();

        if (userId == null && (deviceUuid == null || deviceUuid.isBlank())) {
            throw new GeneralException(ErrorStatus.INVALID_DEVICE);
        }

        boolean isGuest = (userId == null);

        int documentUsed = getUsedCount(userId, deviceUuid, UsageType.DOCUMENT);
        int messageUsed = getUsedCount(userId, deviceUuid, UsageType.MESSAGE);

        int documentLimit = usagePolicy.getLimit(isGuest, UsageType.DOCUMENT);
        int messageLimit = usagePolicy.getLimit(isGuest, UsageType.MESSAGE);

        return UsageResponse.of(
                documentUsed, documentLimit,
                messageUsed, messageLimit
        );
    }

    private int getUsedCount(Long userId, String deviceUuid, UsageType usageType) {

        LocalDate today = LocalDate.now();

        if (userId != null) {
            return usageRepository
                    .findByIdentityTypeAndIdentityKeyAndUsageTypeAndDate(
                            IdentityType.USER,
                            String.valueOf(userId),
                            usageType,
                            today
                    )
                    .map(Usage::getCount)
                    .orElse(0);
        } else {
            return usageRepository
                    .findByIdentityTypeAndIdentityKeyAndUsageTypeAndDate(
                            IdentityType.DEVICE,
                            deviceUuid,
                            usageType,
                            today
                    )
                    .map(Usage::getCount)
                    .orElse(0);
        }
    }
}