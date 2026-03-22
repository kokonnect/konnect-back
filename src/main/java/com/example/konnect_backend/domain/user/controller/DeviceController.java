package com.example.konnect_backend.domain.user.controller;

import com.example.konnect_backend.domain.user.entity.status.Language;
import com.example.konnect_backend.domain.user.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/device")
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/register")
    public void register(
            @RequestHeader("X-Device-Id") String deviceUuid,
            @RequestParam(value = "targetLanguage", required = false)Language language
            ) {
        deviceService.registerDevice(deviceUuid, language);
    }
}