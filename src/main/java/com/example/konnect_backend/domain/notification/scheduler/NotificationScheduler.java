package com.example.konnect_backend.domain.notification.scheduler;

import com.example.konnect_backend.domain.notification.entity.NotificationType;
import com.example.konnect_backend.domain.notification.service.NotificationService;
import com.example.konnect_backend.domain.schedule.entity.Schedule;
import com.example.konnect_backend.domain.schedule.entity.ScheduleAlarm;
import com.example.konnect_backend.domain.schedule.entity.status.AlarmTimeType;
import com.example.konnect_backend.domain.schedule.repository.ScheduleAlarmRepository;
import com.example.konnect_backend.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleAlarmRepository scheduleAlarmRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M월 d일");

    /**
     * 1분마다 실행하여 발송해야 할 알림 확인
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    @Transactional(readOnly = true)
    public void checkAndSendScheduleAlarms() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkStart = now.minusSeconds(30);
        LocalDateTime checkEnd = now.plusSeconds(30);

        log.debug("스케줄 알림 확인 시작: {} ~ {}", checkStart, checkEnd);

        // 10분 전 알림 체크
        checkAlarms(AlarmTimeType.BEFORE_10M, 10, checkStart, checkEnd);

        // 1시간 전 알림 체크
        checkAlarms(AlarmTimeType.BEFORE_1H, 60, checkStart, checkEnd);

        // 1일 전 알림 체크
        checkAlarms(AlarmTimeType.BEFORE_1D, 1440, checkStart, checkEnd);

        // Custom 알림 체크
        checkCustomAlarms(checkStart, checkEnd);
    }

    private void checkAlarms(AlarmTimeType type, int minutesBefore, LocalDateTime checkStart, LocalDateTime checkEnd) {
        // 알림을 발송해야 할 일정 시작 시간 범위 계산
        LocalDateTime scheduleStart = checkStart.plusMinutes(minutesBefore);
        LocalDateTime scheduleEnd = checkEnd.plusMinutes(minutesBefore);

        List<Schedule> schedules = scheduleRepository.findByStartDateBetween(scheduleStart, scheduleEnd);

        for (Schedule schedule : schedules) {
            List<ScheduleAlarm> alarms = scheduleAlarmRepository.findBySchedule(schedule);
            for (ScheduleAlarm alarm : alarms) {
                if (alarm.getAlarmTimeType() == type) {
                    sendScheduleNotification(schedule, alarm, getAlarmMessage(type));
                }
            }
        }
    }

    private void checkCustomAlarms(LocalDateTime checkStart, LocalDateTime checkEnd) {
        List<ScheduleAlarm> customAlarms = scheduleAlarmRepository.findAll().stream()
                .filter(alarm -> alarm.getAlarmTimeType() == AlarmTimeType.CUSTOM)
                .filter(alarm -> alarm.getCustomMinutesBefore() != null)
                .filter(alarm -> {
                    LocalDateTime alarmTime = alarm.getCustomMinutesBefore();
                    return !alarmTime.isBefore(checkStart) && !alarmTime.isAfter(checkEnd);
                })
                .toList();

        for (ScheduleAlarm alarm : customAlarms) {
            sendScheduleNotification(alarm.getSchedule(), alarm, "예정된 알림");
        }
    }

    private void sendScheduleNotification(Schedule schedule, ScheduleAlarm alarm, String timeLabel) {
        String title = "일정 알림";
        String body = buildNotificationBody(schedule, timeLabel);

        try {
            notificationService.createAndSendNotification(
                    alarm.getUser().getId(),
                    title,
                    body,
                    NotificationType.SCHEDULE,
                    schedule.getScheduleId()
            );
            log.info("스케줄 알림 발송 완료 - scheduleId: {}, userId: {}",
                    schedule.getScheduleId(), alarm.getUser().getId());
        } catch (Exception e) {
            log.error("스케줄 알림 발송 실패 - scheduleId: {}, userId: {}, error: {}",
                    schedule.getScheduleId(), alarm.getUser().getId(), e.getMessage());
        }
    }

    private String buildNotificationBody(Schedule schedule, String timeLabel) {
        String time = schedule.getStartDate().format(TIME_FORMATTER);
        String date = schedule.getStartDate().format(DATE_FORMATTER);

        if (schedule.getIsAllDay()) {
            return String.format("[%s] %s\n%s 예정", timeLabel, schedule.getTitle(), date);
        }
        return String.format("[%s] %s\n%s %s 예정", timeLabel, schedule.getTitle(), date, time);
    }

    private String getAlarmMessage(AlarmTimeType type) {
        return switch (type) {
            case BEFORE_10M -> "10분 전";
            case BEFORE_1H -> "1시간 전";
            case BEFORE_1D -> "1일 전";
            case CUSTOM -> "예정된 알림";
        };
    }

    /**
     * 매일 자정에 30일 이전의 오래된 알림 삭제
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        log.info("오래된 알림 정리 시작: {} 이전 알림 삭제", thirtyDaysAgo);
        // NotificationRepository에서 정리 메서드 호출
        // 이 작업은 NotificationService에서 처리하도록 위임 가능
    }
}
