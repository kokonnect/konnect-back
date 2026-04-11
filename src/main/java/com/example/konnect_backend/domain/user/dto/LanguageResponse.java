package com.example.konnect_backend.domain.user.dto;

import com.example.konnect_backend.domain.user.entity.status.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LanguageResponse {

    private Language language;
    private boolean loggedIn;
}