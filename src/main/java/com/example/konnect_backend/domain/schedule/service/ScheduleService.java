package com.example.konnect_backend.domain.schedule.service;

import com.example.konnect_backend.domain.schedule.dto.request.ScheduleAlarmRequest;
import com.example.konnect_backend.domain.schedule.dto.request.ScheduleCreateRequest;
import com.example.konnect_backend.domain.schedule.dto.request.ScheduleRepeatRequest;
import com.example.konnect_backend.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.example.konnect_backend.domain.schedule.dto.response.CalendarDateResponse;
import com.example.konnect_backend.domain.schedule.dto.response.ScheduleAlarmResponse;
import com.example.konnect_backend.domain.schedule.dto.response.ScheduleRepeatResponse;
import com.example.konnect_backend.domain.schedule.dto.response.ScheduleResponse;
import com.example.konnect_backend.domain.schedule.entity.Schedule;
import com.example.konnect_backend.domain.schedule.entity.ScheduleAlarm;
import com.example.konnect_backend.domain.schedule.entity.ScheduleRepeat;
import com.example.konnect_backend.domain.schedule.entity.status.RepeatEndType;
import com.example.konnect_backend.domain.schedule.entity.status.RepeatType;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // ==================== 생성/수정/삭제 ====================

    @Transactional
    public ScheduleResponse createSchedule(ScheduleCreateRequest request) {
        User currentUser = getCurrentUser();
        Child child = validateAndGetChild(request.getChildId(), currentUser.getId());

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

        saveRepeatSetting(saved, request.getRepeat());
        saveAlarmSettings(saved, currentUser, request.getAlarms());

        return getScheduleWithDetails(saved.getScheduleId());
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long scheduleId, ScheduleUpdateRequest request) {
        User currentUser = getCurrentUser();
        Schedule schedule = getScheduleWithOwnerCheck(scheduleId, currentUser.getId());
        Child child = validateAndGetChild(request.getChildId(), currentUser.getId());

        schedule.update(
                request.getTitle(),
                request.getMemo(),
                request.getStartDate(),
                request.getEndDate(),
                request.getIsAllDay(),
                child
        );

        // 기존 설정 삭제 후 새로 저장
        scheduleRepeatRepository.deleteBySchedule(schedule);
        scheduleAlarmRepository.deleteBySchedule(schedule);

        saveRepeatSetting(schedule, request.getRepeat());
        saveAlarmSettings(schedule, currentUser, request.getAlarms());

        return getScheduleWithDetails(schedule.getScheduleId());
    }

    @Transactional
    public void deleteSchedule(Long scheduleId) {
        User currentUser = getCurrentUser();
        Schedule schedule = getScheduleWithOwnerCheck(scheduleId, currentUser.getId());
        scheduleRepository.delete(schedule);
    }

    // ==================== 조회 ====================

    public ScheduleResponse getScheduleWithDetails(Long scheduleId) {
        User currentUser = getCurrentUser();
        Schedule schedule = getScheduleWithOwnerCheck(scheduleId, currentUser.getId());

        ScheduleRepeatResponse repeat = schedule.getScheduleRepeat() != null
                ? ScheduleRepeatResponse.from(schedule.getScheduleRepeat())
                : null;

        List<ScheduleAlarmResponse> alarms = scheduleAlarmRepository.findBySchedule(schedule)
                .stream()
                .map(ScheduleAlarmResponse::from)
                .collect(Collectors.toList());

        return ScheduleResponse.fromWithDetails(schedule, repeat, alarms);
    }

    /**
     * 월별 일정 조회 (반복 일정 확장 포함)
     */
    public List<ScheduleResponse> getMonthlySchedules(int year, int month) {
        User user = getCurrentUser();
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate rangeStart = yearMonth.atDay(1);
        LocalDate rangeEnd = yearMonth.atEndOfMonth();

        return getExpandedSchedulesForRange(user, rangeStart, rangeEnd);
    }

    /**
     * 주별 일정 조회 (반복 일정 확장 포함)
     */
    public List<ScheduleResponse> getWeeklySchedules(LocalDate startDate) {
        User user = getCurrentUser();
        LocalDate weekStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        return getExpandedSchedulesForRange(user, weekStart, weekEnd);
    }

    /**
     * 일별 일정 조회 (반복 일정 확장 포함)
     */
    public List<ScheduleResponse> getDailySchedules(LocalDate date) {
        User user = getCurrentUser();
        return getExpandedSchedulesForRange(user, date, date);
    }

    /**
     * 오늘 일정 조회 (반복 일정 확장 포함)
     */
    public List<ScheduleResponse> getTodaySchedules() {
        User user = getCurrentUser();
        LocalDate today = LocalDate.now();
        return getExpandedSchedulesForRange(user, today, today);
    }

    /**
     * 최근 예정 일정 조회 (반복 일정 확장 포함)
     */
    public List<ScheduleResponse> getRecentSchedules(int limit) {
        User user = getCurrentUser();
        LocalDate today = LocalDate.now();
        LocalDate futureEnd = today.plusMonths(3); // 향후 3개월

        List<ScheduleResponse> expanded = getExpandedSchedulesForRange(user, today, futureEnd);

        return expanded.stream()
                .filter(s -> !s.getStartDate().toLocalDate().isBefore(today))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 달력용 날짜별 일정 존재 여부 조회 (반복 일정 확장 포함)
     */
    public List<CalendarDateResponse> getCalendarDates(int year, int month) {
        User user = getCurrentUser();
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate rangeStart = yearMonth.atDay(1);
        LocalDate rangeEnd = yearMonth.atEndOfMonth();

        Map<LocalDate, Integer> dateCountMap = calculateScheduleCountByDate(user, rangeStart, rangeEnd);

        List<CalendarDateResponse> result = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            Integer count = dateCountMap.getOrDefault(date, 0);
            result.add(CalendarDateResponse.of(date, count));
        }

        return result;
    }

    // ==================== 반복 일정 확장 핵심 로직 ====================

    /**
     * 주어진 기간 내 모든 일정을 조회하고 반복 일정을 확장합니다.
     * 단일 쿼리로 모든 데이터를 가져온 후 메모리에서 처리합니다.
     */
    private List<ScheduleResponse> getExpandedSchedulesForRange(User user, LocalDate rangeStart, LocalDate rangeEnd) {
        // Fetch Join으로 한 번에 조회 (N+1 방지)
        List<Schedule> allSchedules = scheduleRepository.findAllByUserWithRepeat(user);

        List<ScheduleResponse> result = new ArrayList<>();

        for (Schedule schedule : allSchedules) {
            ScheduleRepeat repeat = schedule.getScheduleRepeat();

            if (repeat == null) {
                // 반복 없는 일정: 범위 내에 있으면 추가
                LocalDate scheduleDate = schedule.getStartDate().toLocalDate();
                if (isDateInRange(scheduleDate, rangeStart, rangeEnd)) {
                    result.add(ScheduleResponse.from(schedule));
                }
            } else {
                // 반복 일정: 범위 내 모든 날짜로 확장
                List<LocalDate> expandedDates = expandRepeatSchedule(schedule, repeat, rangeStart, rangeEnd);
                for (LocalDate date : expandedDates) {
                    result.add(createExpandedScheduleResponse(schedule, date));
                }
            }
        }

        // 시작 날짜 기준 정렬
        result.sort((a, b) -> a.getStartDate().compareTo(b.getStartDate()));
        return result;
    }

    /**
     * 날짜별 일정 개수를 계산합니다 (달력 표시용)
     */
    private Map<LocalDate, Integer> calculateScheduleCountByDate(User user, LocalDate rangeStart, LocalDate rangeEnd) {
        List<Schedule> allSchedules = scheduleRepository.findAllByUserWithRepeat(user);
        Map<LocalDate, Integer> dateCountMap = new HashMap<>();

        for (Schedule schedule : allSchedules) {
            ScheduleRepeat repeat = schedule.getScheduleRepeat();

            if (repeat == null) {
                // 반복 없는 일정
                LocalDate scheduleDate = schedule.getStartDate().toLocalDate();
                if (isDateInRange(scheduleDate, rangeStart, rangeEnd)) {
                    dateCountMap.merge(scheduleDate, 1, Integer::sum);
                }
            } else {
                // 반복 일정 확장
                List<LocalDate> expandedDates = expandRepeatSchedule(schedule, repeat, rangeStart, rangeEnd);
                for (LocalDate date : expandedDates) {
                    dateCountMap.merge(date, 1, Integer::sum);
                }
            }
        }

        return dateCountMap;
    }

    /**
     * 반복 일정을 주어진 범위 내의 모든 날짜로 확장합니다.
     */
    private List<LocalDate> expandRepeatSchedule(Schedule schedule, ScheduleRepeat repeat,
                                                  LocalDate rangeStart, LocalDate rangeEnd) {
        List<LocalDate> expandedDates = new ArrayList<>();
        LocalDate scheduleStartDate = schedule.getStartDate().toLocalDate();
        RepeatType repeatType = repeat.getRepeatType();

        // 반복 종료일 계산
        LocalDate effectiveEndDate = calculateEffectiveEndDate(repeat, rangeEnd);

        // rangeStart 이전의 날짜는 스킵하고 첫 번째 유효한 날짜부터 시작
        LocalDate currentDate = findFirstDateInRange(scheduleStartDate, rangeStart, repeatType);

        int count = 0;
        int maxIterations = 1000; // 무한 루프 방지

        while (currentDate != null && !currentDate.isAfter(effectiveEndDate) && count < maxIterations) {
            // COUNT 제한 확인
            if (repeat.getRepeatEndType() == RepeatEndType.COUNT && repeat.getRepeatCount() != null) {
                long currentCount = calculateRepeatCount(scheduleStartDate, currentDate, repeatType);
                if (currentCount >= repeat.getRepeatCount()) {
                    break;
                }
            }

            // 범위 내 날짜만 추가
            if (isDateInRange(currentDate, rangeStart, rangeEnd)) {
                expandedDates.add(currentDate);
            }

            currentDate = getNextRepeatDate(currentDate, repeatType, scheduleStartDate);
            count++;
        }

        return expandedDates;
    }

    /**
     * 반복 타입에 따라 범위 내 첫 번째 유효한 날짜를 찾습니다.
     */
    private LocalDate findFirstDateInRange(LocalDate scheduleStart, LocalDate rangeStart, RepeatType repeatType) {
        if (!scheduleStart.isBefore(rangeStart)) {
            return scheduleStart;
        }

        // rangeStart 이전에 시작한 반복 일정의 경우, rangeStart 이후 첫 번째 날짜 계산
        switch (repeatType) {
            case DAILY:
                return rangeStart;
            case WEEKLY:
                // 같은 요일 중 rangeStart 이후 첫 번째 날짜
                DayOfWeek targetDayOfWeek = scheduleStart.getDayOfWeek();
                return rangeStart.with(TemporalAdjusters.nextOrSame(targetDayOfWeek));
            case MONTHLY:
                // 같은 일(day) 중 rangeStart 이후 첫 번째 날짜
                int targetDay = scheduleStart.getDayOfMonth();
                LocalDate candidate = rangeStart.withDayOfMonth(Math.min(targetDay, rangeStart.lengthOfMonth()));
                if (candidate.isBefore(rangeStart)) {
                    candidate = candidate.plusMonths(1);
                    candidate = candidate.withDayOfMonth(Math.min(targetDay, candidate.lengthOfMonth()));
                }
                return candidate;
            case YEARLY:
                // 같은 월/일 중 rangeStart 이후 첫 번째 날짜
                LocalDate yearlyCandidate = LocalDate.of(rangeStart.getYear(),
                        scheduleStart.getMonth(), scheduleStart.getDayOfMonth());
                if (yearlyCandidate.isBefore(rangeStart)) {
                    yearlyCandidate = yearlyCandidate.plusYears(1);
                }
                return yearlyCandidate;
            default:
                return rangeStart;
        }
    }

    /**
     * 반복 종료일을 계산합니다.
     */
    private LocalDate calculateEffectiveEndDate(ScheduleRepeat repeat, LocalDate rangeEnd) {
        if (repeat.getRepeatEndType() == RepeatEndType.UNTIL_DATE && repeat.getRepeatEndDate() != null) {
            LocalDate repeatEndDate = repeat.getRepeatEndDate().toLocalDate();
            return repeatEndDate.isBefore(rangeEnd) ? repeatEndDate : rangeEnd;
        }
        return rangeEnd;
    }

    /**
     * 다음 반복 날짜를 계산합니다.
     */
    private LocalDate getNextRepeatDate(LocalDate current, RepeatType repeatType, LocalDate originalStart) {
        switch (repeatType) {
            case DAILY:
                return current.plusDays(1);
            case WEEKLY:
                return current.plusWeeks(1);
            case MONTHLY:
                int targetDay = originalStart.getDayOfMonth();
                LocalDate nextMonth = current.plusMonths(1);
                int lastDayOfMonth = nextMonth.lengthOfMonth();
                return nextMonth.withDayOfMonth(Math.min(targetDay, lastDayOfMonth));
            case YEARLY:
                return current.plusYears(1);
            default:
                return current.plusDays(1);
        }
    }

    /**
     * 시작일부터 대상일까지의 반복 횟수를 계산합니다.
     */
    private long calculateRepeatCount(LocalDate startDate, LocalDate targetDate, RepeatType repeatType) {
        switch (repeatType) {
            case DAILY:
                return ChronoUnit.DAYS.between(startDate, targetDate);
            case WEEKLY:
                return ChronoUnit.WEEKS.between(startDate, targetDate);
            case MONTHLY:
                return ChronoUnit.MONTHS.between(startDate, targetDate);
            case YEARLY:
                return ChronoUnit.YEARS.between(startDate, targetDate);
            default:
                return 0;
        }
    }

    /**
     * 반복 일정의 특정 날짜 인스턴스에 대한 응답을 생성합니다.
     */
    private ScheduleResponse createExpandedScheduleResponse(Schedule schedule, LocalDate targetDate) {
        LocalTime startTime = schedule.getStartDate().toLocalTime();
        LocalTime endTime = schedule.getEndDate() != null
                ? schedule.getEndDate().toLocalTime()
                : startTime;

        return ScheduleResponse.from(schedule).toBuilder()
                .startDate(targetDate.atTime(startTime))
                .endDate(targetDate.atTime(endTime))
                .build();
    }

    // ==================== 헬퍼 메서드 ====================

    private User getCurrentUser() {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    private Schedule getScheduleWithOwnerCheck(Long scheduleId, Long userId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SCHEDULE_NOT_FOUND));

        if (!schedule.getUser().getId().equals(userId)) {
            throw new GeneralException(ErrorStatus.FORBIDDEN_ACCESS);
        }

        return schedule;
    }

    private Child validateAndGetChild(Long childId, Long userId) {
        if (childId == null) {
            return null;
        }

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.CHILD_NOT_FOUND));

        if (!child.getUser().getId().equals(userId)) {
            throw new GeneralException(ErrorStatus.FORBIDDEN_ACCESS);
        }

        return child;
    }

    private void saveRepeatSetting(Schedule schedule, ScheduleRepeatRequest repeatRequest) {
        if (repeatRequest == null) {
            return;
        }

        ScheduleRepeat repeat = ScheduleRepeat.builder()
                .schedule(schedule)
                .repeatType(repeatRequest.getRepeatType())
                .repeatEndType(repeatRequest.getRepeatEndType())
                .repeatEndDate(repeatRequest.getRepeatEndDate())
                .repeatCount(repeatRequest.getRepeatCount())
                .build();

        scheduleRepeatRepository.save(repeat);
    }

    private void saveAlarmSettings(Schedule schedule, User user, List<ScheduleAlarmRequest> alarmRequests) {
        if (alarmRequests == null || alarmRequests.isEmpty()) {
            return;
        }

        List<ScheduleAlarm> alarms = alarmRequests.stream()
                .map(req -> ScheduleAlarm.builder()
                        .schedule(schedule)
                        .user(user)
                        .alarmTimeType(req.getAlarmTimeType())
                        .customMinutesBefore(req.getCustomMinutesBefore())
                        .build())
                .collect(Collectors.toList());

        scheduleAlarmRepository.saveAll(alarms);
    }

    private boolean isDateInRange(LocalDate date, LocalDate rangeStart, LocalDate rangeEnd) {
        return !date.isBefore(rangeStart) && !date.isAfter(rangeEnd);
    }
}
