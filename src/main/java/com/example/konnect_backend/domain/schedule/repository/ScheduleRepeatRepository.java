package com.example.konnect_backend.domain.schedule.repository;

import com.example.konnect_backend.domain.schedule.entity.Schedule;
import com.example.konnect_backend.domain.schedule.entity.ScheduleRepeat;
import com.example.konnect_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepeatRepository extends JpaRepository<ScheduleRepeat, Long> {
    
    Optional<ScheduleRepeat> findBySchedule(Schedule schedule);
    
    List<ScheduleRepeat> findByUser(User user);
    
    void deleteBySchedule(Schedule schedule);
}