package com.example.konnect_backend.domain.ai.service;

import com.example.konnect_backend.domain.ai.dto.response.AnalysisHistoryResponse;
import com.example.konnect_backend.domain.ai.entity.log.AnalysisHistory;
import com.example.konnect_backend.domain.ai.model.vo.ExtractedText;
import com.example.konnect_backend.domain.ai.model.vo.UploadFile;
import com.example.konnect_backend.domain.ai.repository.AnalysisHistoryRepository;
import com.example.konnect_backend.domain.ai.type.TargetLanguage;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 문서 분석/번역 내역 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisHistoryService {

    private final UserRepository userRepository;

    private final AnalysisHistoryRepository historyRepository;

    private static final int DEFAULT_HISTORY_LIMIT = 10;

    @Transactional
    public Long saveHistory(Long userId, UploadFile file, TargetLanguage targetLanguage, Long requestLogId,
                            ExtractedText extractedText, String translatedText, String summary,
                            LocalDateTime timestamp) {
        if (userId == null) {
            log.info("비로그인 사용자 - DB 저장 건너뜀");
            return null;
        }

        AnalysisHistory toSave = AnalysisHistory.builder()
            .requestLogId(requestLogId)
            .userId(userId)
            .fileName(file.originalName())
            .fileType(file.fileType())
            .extractedText(extractedText.text())
            .translatedLanguage(targetLanguage.getLanguageCode())
            .translatedText(translatedText)
            .summary(summary)
            .createdAt(timestamp)
            .build();

        AnalysisHistory saved = historyRepository.save(toSave);

        return saved.getId();
    }

    @Transactional(readOnly = true)
    public AnalysisHistoryResponse getHistory() {
        return getHistory(DEFAULT_HISTORY_LIMIT);
    }

    @Transactional(readOnly = true)
    public AnalysisHistoryResponse getHistory(int limit) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        if (userId == null) {
            log.warn("인증되지 않은 사용자의 내역 조회 요청");
            return AnalysisHistoryResponse.emptyResponse();
        }

        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            log.warn("존재하지 않는 사용자: userId={}", userId);
            return AnalysisHistoryResponse.emptyResponse();
        }

        Pageable pageable = PageRequest.of(0, limit);

        Page<AnalysisHistory> pageResponse = historyRepository.findByUserId(userId, pageable);
        List<AnalysisHistory> histories = pageResponse.getContent();
        return AnalysisHistoryResponse.from(histories);
    }
}