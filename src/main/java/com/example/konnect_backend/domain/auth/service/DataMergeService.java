// src/main/java/com/example/konnect_backend/domain/auth/service/DataMergeService.java
package com.example.konnect_backend.domain.auth.service;

import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface DataMergeService {
    void mergeGuestToUser(String deviceUuid, Long userId);
}