package com.example.konnect_backend.domain.user.service;

import com.example.konnect_backend.domain.user.dto.LanguageResponse;
import com.example.konnect_backend.domain.user.entity.status.Language;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LanguagePreferenceService {

    private final UserService userService;
    private final DeviceService deviceService;

    @Transactional
    public LanguageResponse updateLanguage(String deviceUuid, Language language) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();

        if (language == null) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }

        // 로그인 사용자
        if (userId != null) {
            userService.updateLanguage(userId, language);

            // 선택적으로 디바이스 언어도 동기화
            if (deviceUuid != null && !deviceUuid.isBlank()) {
                deviceService.updateLanguage(deviceUuid, language);
            }

            return LanguageResponse.builder()
                    .language(language)
                    .loggedIn(true)
                    .build();
        }

        // 비로그인 사용자
        if (deviceUuid == null || deviceUuid.isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_DEVICE);
        }

        deviceService.updateLanguage(deviceUuid, language);

        return LanguageResponse.builder()
                .language(language)
                .loggedIn(false)
                .build();
    }

    public LanguageResponse getLanguage(String deviceUuid) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();

        // 로그인 상태면 무조건 User.language 우선
        if (userId != null) {
            Language language = userService.getLanguage(userId);
            return LanguageResponse.builder()
                    .language(language)
                    .loggedIn(true)
                    .build();
        }

        if (deviceUuid == null || deviceUuid.isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_DEVICE);
        }

        Language language = deviceService.getLanguage(deviceUuid);
        return LanguageResponse.builder()
                .language(language)
                .loggedIn(false)
                .build();
    }
}