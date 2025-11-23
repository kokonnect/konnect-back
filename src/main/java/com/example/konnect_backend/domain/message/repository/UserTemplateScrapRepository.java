package com.example.konnect_backend.domain.message.repository;

import com.example.konnect_backend.domain.message.entity.MessageTemplate;
import com.example.konnect_backend.domain.message.entity.UserTemplateScrap;
import com.example.konnect_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTemplateScrapRepository extends JpaRepository<UserTemplateScrap, Long> {

    /**
     * 사용자별 스크랩 목록 조회 (최신순)
     */
    List<UserTemplateScrap> findByUserOrderByCreatedAtDesc(User user);

    /**
     * 특정 사용자가 특정 템플릿을 스크랩했는지 확인
     */
    Optional<UserTemplateScrap> findByUserAndMessageTemplate(User user, MessageTemplate messageTemplate);

    /**
     * 특정 사용자가 특정 템플릿을 스크랩했는지 여부
     */
    boolean existsByUserAndMessageTemplate(User user, MessageTemplate messageTemplate);

    /**
     * 특정 템플릿의 스크랩 개수
     */
    Long countByMessageTemplate(MessageTemplate messageTemplate);
}
