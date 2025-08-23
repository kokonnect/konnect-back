// src/main/java/com/example/konnect_backend/domain/user/dto/ChildDto.java
package com.example.konnect_backend.domain.user.dto;

import com.example.konnect_backend.domain.user.entity.Child;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChildDto {
    private Long id;
    
    @NotBlank(message = "자녀 이름은 필수입니다")
    private String name;
    
    private String school;
    private String grade;
    private LocalDate birthDate;
    private String className;
    private String teacherName;

    public static ChildDto from(Child child) {
        return ChildDto.builder()
                .id(child.getId())
                .name(child.getName())
                .school(child.getSchool())
                .grade(child.getGrade())
                .birthDate(child.getBirthDate())
                .className(child.getClassName())
                .teacherName(child.getTeacherName())
                .build();
    }
}