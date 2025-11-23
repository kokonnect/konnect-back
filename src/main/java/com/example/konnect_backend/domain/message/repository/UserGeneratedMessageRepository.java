package com.example.konnect_backend.domain.message.repository;

import com.example.konnect_backend.domain.message.entity.UserGeneratedMessage;
import com.example.konnect_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserGeneratedMessageRepository extends JpaRepository<UserGeneratedMessage, Long> {

    /**
     * 사용자별 생성된 메시지 목록 조회 (최신순)
     */
    List<UserGeneratedMessage> findByUserOrderByCreatedAtDesc(User user);

    /**
     * 사용자별 생성된 메시지 개수 조회
     */
    Long countByUser(User user);
}
