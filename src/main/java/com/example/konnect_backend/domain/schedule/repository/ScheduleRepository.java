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
     * 특정 시간 범위 내에 시작하는 일정 조회 (알림 스케줄러용)
     */
    @Query("SELECT s FROM Schedule s " +
            "WHERE s.startDate >= :startTime AND s.startDate <= :endTime")
    List<Schedule> findByStartDateBetween(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    @Query("""
    SELECT DISTINCT s FROM Schedule s
    LEFT JOIN FETCH s.scheduleRepeat
    WHERE s.user = :user
      AND s.startDate <= :rangeEnd
      AND (s.endDate IS NULL OR s.endDate >= :rangeStart)
    ORDER BY s.startDate ASC
""")
    List<Schedule> findSchedulesOverlappingRange(
            @Param("user") User user,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd
    );

}

