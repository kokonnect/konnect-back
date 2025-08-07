package com.example.konnect_backend.domain.schedule.entity;

import com.example.konnect_backend.domain.schedule.entity.status.AlarmTimeType;
import com.example.konnect_backend.domain.user.entity.User;
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
public class ScheduleAlarm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private AlarmTimeType alarmTimeType; // BEFORE_10M, BEFORE_1H, BEFORE_1D, CUSTOM

    private LocalDateTime customMinutesBefore;
}
