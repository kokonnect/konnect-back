package com.example.konnect_backend.domain.schedule.service;

import com.example.konnect_backend.domain.schedule.dto.request.ScheduleCreateRequest;
import com.example.konnect_backend.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.example.konnect_backend.domain.schedule.dto.response.ScheduleResponse;
import com.example.konnect_backend.domain.schedule.entity.Schedule;
import com.example.konnect_backend.domain.schedule.repository.ScheduleRepository;
import com.example.konnect_backend.domain.user.entity.Child;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.repository.ChildRepository;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {
    
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    
    @Transactional
    public ScheduleResponse createSchedule(ScheduleCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        
        Child child = null;
        if (request.getChildId() != null) {
            child = childRepository.findById(request.getChildId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.CHILD_NOT_FOUND));
            
            if (!child.getUser().getId().equals(userId)) {
                throw new GeneralException(ErrorStatus.FORBIDDEN_ACCESS);
            }
        }
        
        Schedule schedule = Schedule.builder()
                .user(user)
                .child(child)
                .title(request.getTitle())
                .memo(request.getMemo())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isAllDay(request.getIsAllDay())
                .createdFromNotice(request.getCreatedFromNotice())
                .build();
        
        Schedule saved = scheduleRepository.save(schedule);
        return ScheduleResponse.from(saved);
    }
    
    @Transactional
    public ScheduleResponse updateSchedule(Long scheduleId, ScheduleUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SCHEDULE_NOT_FOUND));
        
        if (!schedule.getUser().getId().equals(userId)) {
            throw new GeneralException(ErrorStatus.FORBIDDEN_ACCESS);
        }
        
        Child child = null;
        if (request.getChildId() != null) {
            child = childRepository.findById(request.getChildId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.CHILD_NOT_FOUND));
            
            if (!child.getUser().getId().equals(userId)) {
                throw new GeneralException(ErrorStatus.FORBIDDEN_ACCESS);
            }
        }
        
        schedule.update(
                request.getTitle(),
                request.getMemo(),
                request.getStartDate(),
                request.getEndDate(),
                request.getIsAllDay(),
                child
        );
        
        return ScheduleResponse.from(schedule);
    }
    
    @Transactional
    public void deleteSchedule(Long scheduleId) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SCHEDULE_NOT_FOUND));
        
        if (!schedule.getUser().getId().equals(userId)) {
            throw new GeneralException(ErrorStatus.FORBIDDEN_ACCESS);
        }
        
        scheduleRepository.delete(schedule);
    }
    
    public List<ScheduleResponse> getMonthlySchedules(int year, int month) {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        
        List<Schedule> schedules = scheduleRepository.findByUserAndDateRange(user, startDate, endDate);
        
        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }
    
    public List<ScheduleResponse> getRecentSchedules(int limit) {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        
        LocalDateTime now = LocalDateTime.now();
        List<Schedule> schedules = scheduleRepository.findUpcomingSchedules(user, now);
        
        return schedules.stream()
                .limit(limit)
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }
}