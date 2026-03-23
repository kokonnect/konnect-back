package com.example.konnect_backend.domain.user.controller;

import com.example.konnect_backend.domain.user.dto.UsageResponse;
import com.example.konnect_backend.domain.user.service.UsageFacade;
import com.example.konnect_backend.domain.user.service.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/usage")
public class UsageController {

    private final UsageFacade usageFacade;

    @GetMapping("")
    public UsageResponse getUsage(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceUuid
    ) {
        return usageFacade.getUsage(deviceUuid);
    }
}
