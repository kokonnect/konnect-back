package com.example.konnect_backend.domain.schedule.repository;

import com.example.konnect_backend.domain.schedule.entity.Schedule;
import com.example.konnect_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * 사용자의 모든 일정을 반복 설정과 함께 조회 (Fetch Join으로 N+1 방지)
     */
    @Query("SELECT DISTINCT s FROM Schedule s " +
           "LEFT JOIN FETCH s.scheduleRepeat " +
           "WHERE s.user = :user " +
           "ORDER BY s.startDate ASC")
    List<Schedule> findAllByUserWithRepeat(@Param("user") User user);

    /**
     * 특정 기간 내 일정을 반복 설정과 함께 조회
     */
    @Query("SELECT DISTINCT s FROM Schedule s " +
           "LEFT JOIN FETCH s.scheduleRepeat " +
           "WHERE s.user = :user " +
           "AND ((s.startDate >= :startDate AND s.startDate <= :endDate) " +
           "OR (s.endDate >= :startDate AND s.endDate <= :endDate) " +
           "OR (s.startDate <= :startDate AND s.endDate >= :endDate)) " +
           "ORDER BY s.startDate ASC")
    List<Schedule> findByUserAndDateRangeWithRepeat(@Param("user") User user,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    /**
     * 현재 시점 이후의 예정된 일정 조회 (반복 설정 포함)
     */
    @Query("SELECT DISTINCT s FROM Schedule s " +
           "LEFT JOIN FETCH s.scheduleRepeat " +
           "WHERE s.user = :user " +
           "AND s.startDate >= :now " +
           "ORDER BY s.startDate ASC")
    List<Schedule> findUpcomingSchedulesWithRepeat(@Param("user") User user,
                                                    @Param("now") LocalDateTime now);
}