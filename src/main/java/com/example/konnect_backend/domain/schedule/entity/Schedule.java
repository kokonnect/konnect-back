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
import java.util.List;

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
    
    @OneToOne(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private ScheduleRepeat scheduleRepeat;
    
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduleAlarm> scheduleAlarms;
    
    public void update(String title, String memo, LocalDateTime startDate, 
                      LocalDateTime endDate, Boolean isAllDay, Child child) {
        this.title = title;
        this.memo = memo;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isAllDay = isAllDay;
        this.child = child;
    }
}
