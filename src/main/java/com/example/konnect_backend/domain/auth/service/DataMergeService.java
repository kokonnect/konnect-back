// src/main/java/com/example/konnect_backend/domain/auth/service/DataMergeService.java
package com.example.konnect_backend.domain.auth.service;

import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DataMergeService {

    private final ChildRepository childRepository;

    /** 게스트가 만든 데이터(Child 등)를 실제 유저로 이전 */
    @Transactional
    public void mergeGuestToUser(User guest, User realUser) {
        if (guest == null || realUser == null || guest.getId().equals(realUser.getId())) return;
        childRepository.reassignOwner(guest, realUser);
        // 필요 시 다른 도메인(초안, 파일 등)도 같은 방식으로 추가
    }
}
