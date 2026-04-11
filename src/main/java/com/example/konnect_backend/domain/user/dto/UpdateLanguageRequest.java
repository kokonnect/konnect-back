package com.example.konnect_backend.domain.user.dto;

import com.example.konnect_backend.domain.user.entity.status.Language;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateLanguageRequest {

    @NotNull
    private Language language;
}