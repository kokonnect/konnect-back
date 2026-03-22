package com.example.konnect_backend.domain.user.controller;

import com.example.konnect_backend.domain.user.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/device")
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/register")
    public void register(@RequestHeader("X-Device-Id") String deviceUuid) {
        deviceService.registerDevice(deviceUuid);
    }
}