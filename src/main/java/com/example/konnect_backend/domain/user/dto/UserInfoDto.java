package com.example.konnect_backend.domain.user.dto;

import com.example.konnect_backend.domain.user.entity.status.Language;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDto {
    private String name;
    private Language language;
    private String email;
}
