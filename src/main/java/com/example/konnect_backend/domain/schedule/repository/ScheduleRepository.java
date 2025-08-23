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
    
    @Query("SELECT s FROM Schedule s WHERE s.user = :user " +
           "AND ((s.startDate >= :startDate AND s.startDate < :endDate) " +
           "OR (s.endDate > :startDate AND s.endDate <= :endDate) " +
           "OR (s.startDate <= :startDate AND s.endDate >= :endDate)) " +
           "ORDER BY s.startDate ASC")
    List<Schedule> findByUserAndDateRange(@Param("user") User user, 
                                          @Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT s FROM Schedule s WHERE s.user = :user " +
           "AND s.startDate >= :now " +
           "ORDER BY s.startDate ASC")
    List<Schedule> findUpcomingSchedules(@Param("user") User user, 
                                         @Param("now") LocalDateTime now);
    
    List<Schedule> findByUserOrderByStartDateDesc(User user);
}