// src/main/java/com/example/konnect_backend/domain/auth/dto/request/ChildCreateDto.java
package com.example.konnect_backend.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Date;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChildCreateDto {
    @Schema(example = "김철수")
    private String name;
    @Schema(example = "서울초등학교")
    private String school;
    @Schema(example = "3학년")
    private String grade;
    @Schema(example = "2017-03-01")
    private Date birthDate;
}
