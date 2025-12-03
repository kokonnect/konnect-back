package com.example.konnect_backend.domain.schedule.service;

import com.example.konnect_backend.domain.schedule.dto.request.ScheduleCreateRequest;
import com.example.konnect_backend.domain.schedule.dto.request.ScheduleRepeatRequest;
import com.example.konnect_backend.domain.schedule.dto.response.CalendarDateResponse;
import com.example.konnect_backend.domain.schedule.dto.response.ScheduleResponse;
import com.example.konnect_backend.domain.schedule.entity.Schedule;
import com.example.konnect_backend.domain.schedule.entity.ScheduleRepeat;
import com.example.konnect_backend.domain.schedule.entity.status.RepeatEndType;
import com.example.konnect_backend.domain.schedule.entity.status.RepeatType;
import com.example.konnect_backend.domain.schedule.repository.ScheduleAlarmRepository;
import com.example.konnect_backend.domain.schedule.repository.ScheduleRepeatRepository;
import com.example.konnect_backend.domain.schedule.repository.ScheduleRepository;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.repository.ChildRepository;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService 테스트")
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleRepeatRepository scheduleRepeatRepository;

    @Mock
    private ScheduleAlarmRepository scheduleAlarmRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChildRepository childRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    private User testUser;
    private Schedule testSchedule;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("테스트유저")
                .build();

        // 반복 없는 일반 일정 (12월 5일)
        testSchedule = Schedule.builder()
                .scheduleId(1L)
                .user(testUser)
                .title("일반 일정")
                .startDate(LocalDateTime.of(2025, 12, 5, 10, 0))
                .endDate(LocalDateTime.of(2025, 12, 5, 11, 0))
                .isAllDay(false)
                .build();
    }

    @Nested
    @DisplayName("달력 조회 (getCalendarDates)")
    class GetCalendarDatesTest {

        @Test
        @DisplayName("반복 없는 일정 - 해당 날짜에만 표시")
        void noRepeatSchedule_showsOnlyOnStartDate() {
            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                // given
                securityUtil.when(SecurityUtil::getCurrentUserIdOrNull).thenReturn(1L);
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

                Schedule schedule = Schedule.builder()
                        .scheduleId(1L)
                        .user(testUser)
                        .title("일반 일정")
                        .startDate(LocalDateTime.of(2025, 12, 15, 10, 0))
                        .endDate(LocalDateTime.of(2025, 12, 15, 11, 0))
                        .scheduleRepeat(null)
                        .build();

                when(scheduleRepository.findAllByUserWithRepeat(testUser))
                        .thenReturn(List.of(schedule));

                // when
                List<CalendarDateResponse> result = scheduleService.getCalendarDates(2025, 12);

                // then
                assertThat(result).hasSize(31); // 12월은 31일

                // 15일만 일정 있음
                CalendarDateResponse day15 = result.stream()
                        .filter(r -> r.getDate().getDayOfMonth() == 15)
                        .findFirst().orElseThrow();
                assertThat(day15.getHasSchedule()).isTrue();
                assertThat(day15.getScheduleCount()).isEqualTo(1);

                // 다른 날짜는 일정 없음
                CalendarDateResponse day10 = result.stream()
                        .filter(r -> r.getDate().getDayOfMonth() == 10)
                        .findFirst().orElseThrow();
                assertThat(day10.getHasSchedule()).isFalse();
                assertThat(day10.getScheduleCount()).isEqualTo(0);
            }
        }

        @Test
        @DisplayName("매주 반복 일정 - 같은 요일에 모두 표시 (수요일)")
        void weeklyRepeat_showsOnSameDayOfWeek() {
            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                // given
                securityUtil.when(SecurityUtil::getCurrentUserIdOrNull).thenReturn(1L);
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

                // 12월 3일 수요일 시작, 매주 반복
                ScheduleRepeat repeat = ScheduleRepeat.builder()
                        .id(1L)
                        .repeatType(RepeatType.WEEKLY)
                        .repeatEndType(RepeatEndType.FOREVER)
                        .build();

                Schedule schedule = Schedule.builder()
                        .scheduleId(1L)
                        .user(testUser)
                        .title("매주 수요일 회의")
                        .startDate(LocalDateTime.of(2025, 12, 3, 10, 0)) // 수요일
                        .endDate(LocalDateTime.of(2025, 12, 3, 11, 0))
                        .scheduleRepeat(repeat)
                        .build();

                when(scheduleRepository.findAllByUserWithRepeat(testUser))
                        .thenReturn(List.of(schedule));

                // when
                List<CalendarDateResponse> result = scheduleService.getCalendarDates(2025, 12);

                // then
                // 12월의 수요일: 3, 10, 17, 24, 31일
                List<Integer> wednesdaysInDec = List.of(3, 10, 17, 24, 31);

                for (CalendarDateResponse response : result) {
                    int day = response.getDate().getDayOfMonth();
                    if (wednesdaysInDec.contains(day)) {
                        assertThat(response.getHasSchedule())
                                .as("12월 %d일(수요일)에 일정이 있어야 함", day)
                                .isTrue();
                    } else {
                        assertThat(response.getHasSchedule())
                                .as("12월 %d일에는 일정이 없어야 함", day)
                                .isFalse();
                    }
                }
            }
        }

        @Test
        @DisplayName("매달 반복 일정 - 같은 날짜에 표시 (매달 15일)")
        void monthlyRepeat_showsOnSameDayOfMonth() {
            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                // given
                securityUtil.when(SecurityUtil::getCurrentUserIdOrNull).thenReturn(1L);
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

                // 11월 15일 시작, 매달 반복
                ScheduleRepeat repeat = ScheduleRepeat.builder()
                        .id(1L)
                        .repeatType(RepeatType.MONTHLY)
                        .repeatEndType(RepeatEndType.FOREVER)
                        .build();

                Schedule schedule = Schedule.builder()
                        .scheduleId(1L)
                        .user(testUser)
                        .title("매달 15일 급여일")
                        .startDate(LocalDateTime.of(2025, 11, 15, 10, 0)) // 11월 15일 시작
                        .endDate(LocalDateTime.of(2025, 11, 15, 11, 0))
                        .scheduleRepeat(repeat)
                        .build();

                when(scheduleRepository.findAllByUserWithRepeat(testUser))
                        .thenReturn(List.of(schedule));

                // when
                List<CalendarDateResponse> result = scheduleService.getCalendarDates(2025, 12);

                // then
                // 12월 15일에만 일정이 있어야 함
                CalendarDateResponse day15 = result.stream()
                        .filter(r -> r.getDate().getDayOfMonth() == 15)
                        .findFirst().orElseThrow();
                assertThat(day15.getHasSchedule()).isTrue();
                assertThat(day15.getScheduleCount()).isEqualTo(1);

                // 다른 날짜는 일정 없음
                long daysWithSchedule = result.stream()
                        .filter(CalendarDateResponse::getHasSchedule)
                        .count();
                assertThat(daysWithSchedule).isEqualTo(1);
            }
        }

        @Test
        @DisplayName("매일 반복 일정 - 기간 내 모든 날짜에 표시")
        void dailyRepeat_showsOnEveryDay() {
            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                // given
                securityUtil.when(SecurityUtil::getCurrentUserIdOrNull).thenReturn(1L);
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

                // 12월 1일 시작, 매일 반복, 12월 10일까지
                ScheduleRepeat repeat = ScheduleRepeat.builder()
                        .id(1L)
                        .repeatType(RepeatType.DAILY)
                        .repeatEndType(RepeatEndType.UNTIL_DATE)
                        .repeatEndDate(LocalDateTime.of(2025, 12, 10, 23, 59))
                        .build();

                Schedule schedule = Schedule.builder()
                        .scheduleId(1L)
                        .user(testUser)
                        .title("매일 운동")
                        .startDate(LocalDateTime.of(2025, 12, 1, 7, 0))
                        .endDate(LocalDateTime.of(2025, 12, 1, 8, 0))
                        .scheduleRepeat(repeat)
                        .build();

                when(scheduleRepository.findAllByUserWithRepeat(testUser))
                        .thenReturn(List.of(schedule));

                // when
                List<CalendarDateResponse> result = scheduleService.getCalendarDates(2025, 12);

                // then
                // 12월 1일~10일에만 일정이 있어야 함
                for (CalendarDateResponse response : result) {
                    int day = response.getDate().getDayOfMonth();
                    if (day >= 1 && day <= 10) {
                        assertThat(response.getHasSchedule())
                                .as("12월 %d일에 일정이 있어야 함", day)
                                .isTrue();
                    } else {
                        assertThat(response.getHasSchedule())
                                .as("12월 %d일에는 일정이 없어야 함", day)
                                .isFalse();
                    }
                }
            }
        }

        @Test
        @DisplayName("반복 횟수 제한 - COUNT 만큼만 표시")
        void repeatWithCount_showsLimitedTimes() {
            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                // given
                securityUtil.when(SecurityUtil::getCurrentUserIdOrNull).thenReturn(1L);
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

                // 12월 3일 수요일 시작, 매주 반복, 3회만
                ScheduleRepeat repeat = ScheduleRepeat.builder()
                        .id(1L)
                        .repeatType(RepeatType.WEEKLY)
                        .repeatEndType(RepeatEndType.COUNT)
                        .repeatCount(3L)
                        .build();

                Schedule schedule = Schedule.builder()
                        .scheduleId(1L)
                        .user(testUser)
                        .title("매주 수요일 회의 (3회)")
                        .startDate(LocalDateTime.of(2025, 12, 3, 10, 0))
                        .endDate(LocalDateTime.of(2025, 12, 3, 11, 0))
                        .scheduleRepeat(repeat)
                        .build();

                when(scheduleRepository.findAllByUserWithRepeat(testUser))
                        .thenReturn(List.of(schedule));

                // when
                List<CalendarDateResponse> result = scheduleService.getCalendarDates(2025, 12);

                // then
                // 12월 3, 10, 17일에만 일정이 있어야 함 (3회)
                List<Integer> expectedDays = List.of(3, 10, 17);
                long daysWithSchedule = result.stream()
                        .filter(CalendarDateResponse::getHasSchedule)
                        .count();
                assertThat(daysWithSchedule).isEqualTo(3);

                for (CalendarDateResponse response : result) {
                    int day = response.getDate().getDayOfMonth();
                    if (expectedDays.contains(day)) {
                        assertThat(response.getHasSchedule()).isTrue();
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("일별 일정 조회 (getDailySchedules)")
    class GetDailySchedulesTest {

        @Test
        @DisplayName("반복 일정이 해당 날짜에 확장되어 조회됨")
        void repeatSchedule_expandedToTargetDate() {
            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                // given
                securityUtil.when(SecurityUtil::getCurrentUserIdOrNull).thenReturn(1L);
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

                // 12월 3일 수요일 시작, 매주 반복
                ScheduleRepeat repeat = ScheduleRepeat.builder()
                        .id(1L)
                        .repeatType(RepeatType.WEEKLY)
                        .repeatEndType(RepeatEndType.FOREVER)
                        .build();

                Schedule schedule = Schedule.builder()
                        .scheduleId(1L)
                        .user(testUser)
                        .title("매주 수요일 회의")
                        .startDate(LocalDateTime.of(2025, 12, 3, 14, 0)) // 오후 2시
                        .endDate(LocalDateTime.of(2025, 12, 3, 15, 0))   // 오후 3시
                        .scheduleRepeat(repeat)
                        .build();

                when(scheduleRepository.findAllByUserWithRepeat(testUser))
                        .thenReturn(List.of(schedule));

                // when - 12월 10일 (수요일) 조회
                List<ScheduleResponse> result = scheduleService.getDailySchedules(LocalDate.of(2025, 12, 10));

                // then
                assertThat(result).hasSize(1);

                ScheduleResponse response = result.get(0);
                assertThat(response.getTitle()).isEqualTo("매주 수요일 회의");
                // 날짜는 조회한 날짜(12월 10일)로 변경되어야 함
                assertThat(response.getStartDate().toLocalDate())
                        .isEqualTo(LocalDate.of(2025, 12, 10));
                // 시간은 원래 일정 시간 유지
                assertThat(response.getStartDate().getHour()).isEqualTo(14);
                assertThat(response.getEndDate().getHour()).isEqualTo(15);
            }
        }

        @Test
        @DisplayName("해당 요일이 아닌 날짜 조회 시 빈 결과")
        void wrongDayOfWeek_returnsEmpty() {
            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                // given
                securityUtil.when(SecurityUtil::getCurrentUserIdOrNull).thenReturn(1L);
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

                // 12월 3일 수요일 시작, 매주 반복
                ScheduleRepeat repeat = ScheduleRepeat.builder()
                        .id(1L)
                        .repeatType(RepeatType.WEEKLY)
                        .repeatEndType(RepeatEndType.FOREVER)
                        .build();

                Schedule schedule = Schedule.builder()
                        .scheduleId(1L)
                        .user(testUser)
                        .title("매주 수요일 회의")
                        .startDate(LocalDateTime.of(2025, 12, 3, 14, 0))
                        .endDate(LocalDateTime.of(2025, 12, 3, 15, 0))
                        .scheduleRepeat(repeat)
                        .build();

                when(scheduleRepository.findAllByUserWithRepeat(testUser))
                        .thenReturn(List.of(schedule));

                // when - 12월 11일 (목요일) 조회
                List<ScheduleResponse> result = scheduleService.getDailySchedules(LocalDate.of(2025, 12, 11));

                // then
                assertThat(result).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("월별 일정 조회 (getMonthlySchedules)")
    class GetMonthlySchedulesTest {

        @Test
        @DisplayName("매주 반복 일정이 해당 월의 모든 요일에 확장됨")
        void weeklyRepeat_expandedToAllWeeksInMonth() {
            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                // given
                securityUtil.when(SecurityUtil::getCurrentUserIdOrNull).thenReturn(1L);
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

                // 12월 3일 수요일 시작, 매주 반복
                ScheduleRepeat repeat = ScheduleRepeat.builder()
                        .id(1L)
                        .repeatType(RepeatType.WEEKLY)
                        .repeatEndType(RepeatEndType.FOREVER)
                        .build();

                Schedule schedule = Schedule.builder()
                        .scheduleId(1L)
                        .user(testUser)
                        .title("매주 수요일 회의")
                        .startDate(LocalDateTime.of(2025, 12, 3, 14, 0))
                        .endDate(LocalDateTime.of(2025, 12, 3, 15, 0))
                        .scheduleRepeat(repeat)
                        .build();

                when(scheduleRepository.findAllByUserWithRepeat(testUser))
                        .thenReturn(List.of(schedule));

                // when
                List<ScheduleResponse> result = scheduleService.getMonthlySchedules(2025, 12);

                // then
                // 12월의 수요일: 3, 10, 17, 24, 31일 = 5개
                assertThat(result).hasSize(5);

                List<Integer> expectedDays = List.of(3, 10, 17, 24, 31);
                List<Integer> actualDays = result.stream()
                        .map(r -> r.getStartDate().getDayOfMonth())
                        .toList();

                assertThat(actualDays).containsExactlyElementsOf(expectedDays);
            }
        }
    }

    @Nested
    @DisplayName("매년 반복 일정 (YEARLY)")
    class YearlyRepeatTest {

        @Test
        @DisplayName("매년 반복 일정 - 같은 월/일에 표시")
        void yearlyRepeat_showsOnSameDateEveryYear() {
            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                // given
                securityUtil.when(SecurityUtil::getCurrentUserIdOrNull).thenReturn(1L);
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

                // 2024년 12월 25일 시작, 매년 반복
                ScheduleRepeat repeat = ScheduleRepeat.builder()
                        .id(1L)
                        .repeatType(RepeatType.YEARLY)
                        .repeatEndType(RepeatEndType.FOREVER)
                        .build();

                Schedule schedule = Schedule.builder()
                        .scheduleId(1L)
                        .user(testUser)
                        .title("크리스마스")
                        .startDate(LocalDateTime.of(2024, 12, 25, 0, 0))
                        .endDate(LocalDateTime.of(2024, 12, 25, 23, 59))
                        .scheduleRepeat(repeat)
                        .build();

                when(scheduleRepository.findAllByUserWithRepeat(testUser))
                        .thenReturn(List.of(schedule));

                // when - 2025년 12월 조회
                List<CalendarDateResponse> result = scheduleService.getCalendarDates(2025, 12);

                // then
                // 12월 25일에만 일정이 있어야 함
                CalendarDateResponse day25 = result.stream()
                        .filter(r -> r.getDate().getDayOfMonth() == 25)
                        .findFirst().orElseThrow();
                assertThat(day25.getHasSchedule()).isTrue();
                assertThat(day25.getScheduleCount()).isEqualTo(1);

                // 다른 날짜는 일정 없음
                long daysWithSchedule = result.stream()
                        .filter(CalendarDateResponse::getHasSchedule)
                        .count();
                assertThat(daysWithSchedule).isEqualTo(1);
            }
        }

        @Test
        @DisplayName("윤년 2월 29일 - 평년에는 2월 28일로 표시")
        void leapYearDate_adjustedToValidDate() {
            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                // given
                securityUtil.when(SecurityUtil::getCurrentUserIdOrNull).thenReturn(1L);
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

                // 2024년 2월 29일 시작 (윤년), 매년 반복
                ScheduleRepeat repeat = ScheduleRepeat.builder()
                        .id(1L)
                        .repeatType(RepeatType.YEARLY)
                        .repeatEndType(RepeatEndType.FOREVER)
                        .build();

                Schedule schedule = Schedule.builder()
                        .scheduleId(1L)
                        .user(testUser)
                        .title("윤년 생일")
                        .startDate(LocalDateTime.of(2024, 2, 29, 10, 0))
                        .endDate(LocalDateTime.of(2024, 2, 29, 11, 0))
                        .scheduleRepeat(repeat)
                        .build();

                when(scheduleRepository.findAllByUserWithRepeat(testUser))
                        .thenReturn(List.of(schedule));

                // when - 2025년 2월 조회 (평년)
                List<CalendarDateResponse> result = scheduleService.getCalendarDates(2025, 2);

                // then
                // 2월 28일에 일정이 있어야 함 (29일이 없으므로)
                CalendarDateResponse day28 = result.stream()
                        .filter(r -> r.getDate().getDayOfMonth() == 28)
                        .findFirst().orElseThrow();
                assertThat(day28.getHasSchedule()).isTrue();

                // 2025년 2월은 28일까지만 있음
                assertThat(result).hasSize(28);
            }
        }

        @Test
        @DisplayName("매년 반복 - 횟수 제한 적용")
        void yearlyRepeat_withCountLimit() {
            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                // given
                securityUtil.when(SecurityUtil::getCurrentUserIdOrNull).thenReturn(1L);
                when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

                // 2024년 12월 25일 시작, 매년 반복, 2회만
                ScheduleRepeat repeat = ScheduleRepeat.builder()
                        .id(1L)
                        .repeatType(RepeatType.YEARLY)
                        .repeatEndType(RepeatEndType.COUNT)
                        .repeatCount(2L)
                        .build();

                Schedule schedule = Schedule.builder()
                        .scheduleId(1L)
                        .user(testUser)
                        .title("한정 이벤트")
                        .startDate(LocalDateTime.of(2024, 12, 25, 10, 0))
                        .endDate(LocalDateTime.of(2024, 12, 25, 11, 0))
                        .scheduleRepeat(repeat)
                        .build();

                when(scheduleRepository.findAllByUserWithRepeat(testUser))
                        .thenReturn(List.of(schedule));

                // when - 2024년 12월 조회 (첫 번째)
                List<CalendarDateResponse> result2024 = scheduleService.getCalendarDates(2024, 12);

                // then - 2024년에는 있어야 함
                CalendarDateResponse day25_2024 = result2024.stream()
                        .filter(r -> r.getDate().getDayOfMonth() == 25)
                        .findFirst().orElseThrow();
                assertThat(day25_2024.getHasSchedule()).isTrue();

                // when - 2025년 12월 조회 (두 번째)
                List<CalendarDateResponse> result2025 = scheduleService.getCalendarDates(2025, 12);

                // then - 2025년에도 있어야 함 (2회째)
                CalendarDateResponse day25_2025 = result2025.stream()
                        .filter(r -> r.getDate().getDayOfMonth() == 25)
                        .findFirst().orElseThrow();
                assertThat(day25_2025.getHasSchedule()).isTrue();

                // when - 2026년 12월 조회 (세 번째 - 초과)
                List<CalendarDateResponse> result2026 = scheduleService.getCalendarDates(2026, 12);

                // then - 2026년에는 없어야 함 (2회 초과)
                CalendarDateResponse day25_2026 = result2026.stream()
                        .filter(r -> r.getDate().getDayOfMonth() == 25)
                        .findFirst().orElseThrow();
                assertThat(day25_2026.getHasSchedule()).isFalse();
            }
        }
    }
}
