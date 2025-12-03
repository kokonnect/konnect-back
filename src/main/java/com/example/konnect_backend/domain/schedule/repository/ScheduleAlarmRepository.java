package com.example.konnect_backend.domain.schedule.repository;

import com.example.konnect_backend.domain.schedule.entity.Schedule;
import com.example.konnect_backend.domain.schedule.entity.ScheduleAlarm;
import com.example.konnect_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleAlarmRepository extends JpaRepository<ScheduleAlarm, Long> {
    
    List<ScheduleAlarm> findBySchedule(Schedule schedule);
    
    List<ScheduleAlarm> findByUser(User user);
    
    void deleteBySchedule(Schedule schedule);
    
    @Query("SELECT sa FROM ScheduleAlarm sa JOIN sa.schedule s " +
           "WHERE sa.user = :user AND s.startDate BETWEEN :startTime AND :endTime")
    List<ScheduleAlarm> findAlarmsInTimeRange(@Param("user") User user,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    @Query("SELECT sa FROM ScheduleAlarm sa " +
           "WHERE sa.alarmTimeType = 'CUSTOM' " +
           "AND sa.customMinutesBefore IS NOT NULL " +
           "AND sa.customMinutesBefore BETWEEN :startTime AND :endTime")
    List<ScheduleAlarm> findCustomAlarmsInTimeRange(@Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);
}