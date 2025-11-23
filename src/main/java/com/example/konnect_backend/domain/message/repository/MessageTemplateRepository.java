package com.example.konnect_backend.domain.message.repository;

import com.example.konnect_backend.domain.message.entity.MessageTemplate;
import com.example.konnect_backend.domain.message.entity.status.ScenarioCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {

    /**
     * 시나리오 카테고리별 템플릿 조회
     */
    List<MessageTemplate> findByScenarioCategory(ScenarioCategory scenarioCategory);

    /**
     * 제목으로 검색 (부분 일치)
     */
    List<MessageTemplate> findByTitleContaining(String keyword);
}
