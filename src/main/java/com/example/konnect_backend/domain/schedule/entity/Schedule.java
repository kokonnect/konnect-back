package com.example.konnect_backend.domain.schedule.entity;

import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.entity.Child;
import com.example.konnect_backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scheduleId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "child_id")
    private Child child;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String memo;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Boolean isAllDay;
    private Boolean createdFromNotice;
}
