// src/main/java/com/example/konnect_backend/domain/user/dto/ChildUpdateDto.java
package com.example.konnect_backend.domain.user.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChildUpdateDto {
    private String name;
    private String school;
    private String grade;
    private LocalDate birthDate;
    private String className;
    private String teacherName;
}