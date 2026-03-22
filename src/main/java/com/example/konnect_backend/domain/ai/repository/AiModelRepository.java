package com.example.konnect_backend.domain.ai.repository;

import com.example.konnect_backend.domain.ai.entity.AiModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiModelRepository extends JpaRepository<AiModel, Long> {
    Optional<AiModel> findByName(String name);
}
