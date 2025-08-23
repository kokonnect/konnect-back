package com.example.konnect_backend.domain.schedule.service;

import com.example.konnect_backend.domain.schedule.dto.request.ScheduleCreateRequest;
import com.example.konnect_backend.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.example.konnect_backend.domain.schedule.dto.response.CalendarDateResponse;
import com.example.konnect_backend.domain.schedule.dto.response.ScheduleAlarmResponse;
import com.example.konnect_backend.domain.schedule.dto.response.ScheduleRepeatResponse;
import com.example.konnect_backend.domain.schedule.dto.response.ScheduleResponse;
import com.example.konnect_backend.domain.schedule.entity.Schedule;
import com.example.konnect_backend.domain.schedule.entity.ScheduleAlarm;
import com.example.konnect_backend.domain.schedule.entity.ScheduleRepeat;
import com.example.konnect_backend.domain.schedule.repository.ScheduleAlarmRepository;
import com.example.konnect_backend.domain.schedule.repository.ScheduleRepeatRepository;
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

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {
    
    private final ScheduleRepository scheduleRepository;
    private final ScheduleRepeatRepository scheduleRepeatRepository;
    private final ScheduleAlarmRepository scheduleAlarmRepository;
    private final UserRepository userRepository;
    private final ChildRepository childRepository;

    @Transactional
    public ScheduleResponse createSchedule(ScheduleCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        User currentUser = userRepository.findById(userId)
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
                .user(currentUser)
                .child(child)
                .title(request.getTitle())
                .memo(request.getMemo())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isAllDay(request.getIsAllDay())
                .createdFromNotice(request.getCreatedFromNotice())
                .build();
        
        Schedule saved = scheduleRepository.save(schedule);
        
        // 반복 설정 저장
        if (request.getRepeat() != null) {
            ScheduleRepeat repeat = ScheduleRepeat.builder()
                    .schedule(saved)
                    .user(currentUser)
                    .repeatType(request.getRepeat().getRepeatType())
                    .repeatEndType(request.getRepeat().getRepeatEndType())
                    .repeatEndDate(request.getRepeat().getRepeatEndDate())
                    .repeatCount(request.getRepeat().getRepeatCount())
                    .build();
            scheduleRepeatRepository.save(repeat);
        }
        
        // 알림 설정 저장
        if (request.getAlarms() != null && !request.getAlarms().isEmpty()) {
            List<ScheduleAlarm> alarms = request.getAlarms().stream()
                    .map(alarmReq -> ScheduleAlarm.builder()
                            .schedule(saved)
                            .user(currentUser)
                            .alarmTimeType(alarmReq.getAlarmTimeType())
                            .customMinutesBefore(alarmReq.getCustomMinutesBefore())
                            .build())
                    .collect(Collectors.toList());
            scheduleAlarmRepository.saveAll(alarms);
        }
        
        return getScheduleWithDetails(saved.getScheduleId());
    }
    
    @Transactional
    public ScheduleResponse updateSchedule(Long scheduleId, ScheduleUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        
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
        
        // 기존 반복 설정 삭제
        scheduleRepeatRepository.deleteBySchedule(schedule);
        
        // 새 반복 설정 저장
        if (request.getRepeat() != null) {
            ScheduleRepeat repeat = ScheduleRepeat.builder()
                    .schedule(schedule)
                    .user(currentUser)
                    .repeatType(request.getRepeat().getRepeatType())
                    .repeatEndType(request.getRepeat().getRepeatEndType())
                    .repeatEndDate(request.getRepeat().getRepeatEndDate())
                    .repeatCount(request.getRepeat().getRepeatCount())
                    .build();
            scheduleRepeatRepository.save(repeat);
        }
        
        // 기존 알림 설정 삭제
        scheduleAlarmRepository.deleteBySchedule(schedule);
        
        // 새 알림 설정 저장
        if (request.getAlarms() != null && !request.getAlarms().isEmpty()) {
            List<ScheduleAlarm> alarms = request.getAlarms().stream()
                    .map(alarmReq -> ScheduleAlarm.builder()
                            .schedule(schedule)
                            .user(currentUser)
                            .alarmTimeType(alarmReq.getAlarmTimeType())
                            .customMinutesBefore(alarmReq.getCustomMinutesBefore())
                            .build())
                    .collect(Collectors.toList());
            scheduleAlarmRepository.saveAll(alarms);
        }
        
        return getScheduleWithDetails(schedule.getScheduleId());
    }
    
    @Transactional
    public void deleteSchedule(Long scheduleId) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SCHEDULE_NOT_FOUND));
        
        if (!schedule.getUser().getId().equals(userId)) {
            throw new GeneralException(ErrorStatus.FORBIDDEN_ACCESS);
        }
        
        scheduleRepository.delete(schedule);
    }
    
    public List<ScheduleResponse> getMonthlySchedules(int year, int month) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
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
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        
        LocalDateTime now = LocalDateTime.now();
        List<Schedule> schedules = scheduleRepository.findUpcomingSchedules(user, now);
        
        return schedules.stream()
                .limit(limit)
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }
    
    public ScheduleResponse getScheduleWithDetails(Long scheduleId) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SCHEDULE_NOT_FOUND));
        
        if (!schedule.getUser().getId().equals(userId)) {
            throw new GeneralException(ErrorStatus.FORBIDDEN_ACCESS);
        }
        
        ScheduleRepeatResponse repeat = scheduleRepeatRepository.findBySchedule(schedule)
                .map(ScheduleRepeatResponse::from)
                .orElse(null);
        
        List<ScheduleAlarmResponse> alarms = scheduleAlarmRepository.findBySchedule(schedule)
                .stream()
                .map(ScheduleAlarmResponse::from)
                .collect(Collectors.toList());
        
        return ScheduleResponse.fromWithDetails(schedule, repeat, alarms);
    }
    
    public List<ScheduleResponse> getDailySchedules(LocalDate date) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        
        LocalDateTime dateTime = date.atStartOfDay();
        List<Schedule> schedules = scheduleRepository.findByUserAndDate(user, dateTime);
        
        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }
    
    public List<ScheduleResponse> getWeeklySchedules(LocalDate startDate) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        
        LocalDate weekStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(7);
        
        LocalDateTime startDateTime = weekStart.atStartOfDay();
        LocalDateTime endDateTime = weekEnd.atStartOfDay();
        
        List<Schedule> schedules = scheduleRepository.findByUserAndWeek(user, startDateTime, endDateTime);
        
        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }
    
    public List<ScheduleResponse> getTodaySchedules() {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        
        List<Schedule> schedules = scheduleRepository.findTodaySchedules(user);
        
        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }
    
    public List<CalendarDateResponse> getCalendarDates(int year, int month) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        
        List<Object[]> scheduleCounts = scheduleRepository.findScheduleCountsByDate(user, startDate, endDate);
        
        List<CalendarDateResponse> result = new ArrayList<>();
        
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            Integer count = scheduleCounts.stream()
                    .filter(obj -> {
                        if (obj[0] instanceof Date) {
                            return ((Date) obj[0]).toLocalDate().equals(date);
                        }
                        return false;
                    })
                    .map(obj -> ((Number) obj[1]).intValue())
                    .findFirst()
                    .orElse(0);
            
            result.add(CalendarDateResponse.of(date, count));
        }
        
        return result;
    }
}