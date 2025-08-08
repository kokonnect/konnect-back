// src/main/java/com/example/konnect_backend/domain/user/entity/Child.java
package com.example.konnect_backend.domain.user.entity;

import com.example.konnect_backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Child extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long childId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // 부모(User) 필수
    private User user;

    @Temporal(TemporalType.DATE)
    private Date birthDate;

    private String name;
    private String school;
    private String grade;
}
