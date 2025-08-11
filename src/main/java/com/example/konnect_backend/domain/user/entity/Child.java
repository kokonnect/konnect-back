// src/main/java/com/example/konnect_backend/domain/user/entity/Child.java
package com.example.konnect_backend.domain.user.entity;

import com.example.konnect_backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Child extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // 부모(User) 필수
    private User user;

    @Temporal(TemporalType.DATE)
    private LocalDate birthDate;

    private String name;
    private String school;
    private String grade;
    private String className;
    private String teacherName;

    public void update(String name, String school, String grade, LocalDate birthDate, 
                      String className, String teacherName) {
        if (name != null) this.name = name;
        if (school != null) this.school = school;
        if (grade != null) this.grade = grade;
        if (birthDate != null) this.birthDate = birthDate;
        if (className != null) this.className = className;
        if (teacherName != null) this.teacherName = teacherName;
    }
}
