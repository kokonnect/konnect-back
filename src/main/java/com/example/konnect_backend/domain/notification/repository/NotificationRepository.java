package com.example.konnect_backend.domain.notification.repository;

import com.example.konnect_backend.domain.notification.entity.Notification;
import com.example.konnect_backend.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 사용자의 모든 알림 조회 (최신순)
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // 사용자의 읽지 않은 알림 조회
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    // 사용자의 읽지 않은 알림 개수
    long countByUserAndIsReadFalse(User user);

    // 사용자의 모든 알림을 읽음 처리
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    int markAllAsReadByUser(@Param("user") User user);

    // 특정 기간 이전의 알림 삭제 (정리용)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :before")
    int deleteOldNotifications(@Param("before") LocalDateTime before);

    // 발송되지 않은 알림 조회 (재시도용)
    List<Notification> findByIsSentFalseAndCreatedAtAfter(LocalDateTime after);
}
