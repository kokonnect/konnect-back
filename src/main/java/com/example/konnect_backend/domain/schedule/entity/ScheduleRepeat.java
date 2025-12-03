package com.example.konnect_backend.domain.schedule.entity;

import com.example.konnect_backend.domain.schedule.entity.status.RepeatEndType;
import com.example.konnect_backend.domain.schedule.entity.status.RepeatType;
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
public class ScheduleRepeat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepeatType repeatType; // DAILY, WEEKLY, MONTHLY, YEARLY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepeatEndType repeatEndType; // FOREVER, UNTIL_DATE, COUNT

    private LocalDateTime repeatEndDate;
    private Long repeatCount;
}
