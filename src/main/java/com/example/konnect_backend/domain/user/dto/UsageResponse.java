package com.example.konnect_backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UsageResponse {

    private int documentUsed;
    private int documentLimit;

    private int messageUsed;
    private int messageLimit;

    public static UsageResponse of(
            int documentUsed, int documentLimit,
            int messageUsed, int messageLimit
    ) {
        return UsageResponse.builder()
                .documentUsed(documentUsed)
                .documentLimit(documentLimit)
                .messageUsed(messageUsed)
                .messageLimit(messageLimit)
                .build();
    }
}