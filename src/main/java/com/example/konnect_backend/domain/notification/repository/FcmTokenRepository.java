package com.example.konnect_backend.domain.notification.repository;

import com.example.konnect_backend.domain.notification.entity.FcmToken;
import com.example.konnect_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    // 사용자의 활성화된 토큰 목록 조회
    List<FcmToken> findByUserAndIsActiveTrue(User user);

    // 토큰으로 조회
    Optional<FcmToken> findByToken(String token);

    // 디바이스 ID로 조회
    Optional<FcmToken> findByUserAndDeviceId(User user, String deviceId);

    // 사용자의 모든 토큰 조회
    List<FcmToken> findByUser(User user);

    // 토큰 존재 여부 확인
    boolean existsByToken(String token);

    // 사용자의 모든 토큰 비활성화
    @Modifying
    @Query("UPDATE FcmToken t SET t.isActive = false WHERE t.user = :user")
    int deactivateAllByUser(@Param("user") User user);

    // 특정 토큰 삭제
    void deleteByToken(String token);
}
