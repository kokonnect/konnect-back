package com.example.konnect_backend.domain.ai.service.history;

import com.example.konnect_backend.domain.ai.domain.entity.log.AnalysisHistory;
import com.example.konnect_backend.domain.ai.domain.vo.ExtractedText;
import com.example.konnect_backend.domain.ai.domain.vo.UploadFile;
import com.example.konnect_backend.domain.ai.dto.response.AnalysisHistoryResponse;
import com.example.konnect_backend.domain.ai.repository.AnalysisHistoryRepository;
import com.example.konnect_backend.domain.ai.type.TargetLanguage;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    public Long saveHistory(Long userId, String deviceUuid, UploadFile file, TargetLanguage targetLanguage, Long requestLogId,
                            ExtractedText extractedText, String translatedText, String summary,
                            LocalDateTime timestamp) {

        AnalysisHistory toSave;

        if (userId == null && (deviceUuid == null || deviceUuid.isBlank())) {
            throw new GeneralException(ErrorStatus.INVALID_DEVICE);
        }

        if (userId != null) {
            toSave = AnalysisHistory.builder()
                    .requestLogId(requestLogId)
                    .userId(userId)
                    .deviceUuid(deviceUuid)
                    .fileName(file.originalName())
                    .fileType(file.fileType())
                    .extractedText(extractedText.text())
                    .translatedLanguage(targetLanguage.getLanguageCode())
                    .translatedText(translatedText)
                    .summary(summary)
                    .createdAt(timestamp)
                    .build();
        } else {
            toSave = AnalysisHistory.builder()
                    .requestLogId(requestLogId)
                    .userId(null)
                    .deviceUuid(deviceUuid)
                    .fileName(file.originalName())
                    .fileType(file.fileType())
                    .extractedText(extractedText.text())
                    .translatedLanguage(targetLanguage.getLanguageCode())
                    .translatedText(translatedText)
                    .summary(summary)
                    .createdAt(timestamp)
                    .build();
        }

        AnalysisHistory saved = historyRepository.save(toSave);

        return saved.getId();
    }

    @Transactional(readOnly = true)
    public AnalysisHistoryResponse getHistory(String deviceUuid) {
        return getHistory(deviceUuid, DEFAULT_HISTORY_LIMIT);
    }

    @Transactional(readOnly = true)
    public AnalysisHistoryResponse getHistory(String deviceUuid, int limit) {

        Long userId = SecurityUtil.getCurrentUserIdOrNull();

        Pageable pageable = PageRequest.of(0, limit);
        Page<AnalysisHistory> pageResponse;

        if (userId != null) {
            pageResponse = historyRepository.findByUserId(userId, pageable);
        } else {
            if (deviceUuid == null || deviceUuid.isBlank()) {
                log.warn("deviceUuid 없음");
                return AnalysisHistoryResponse.emptyResponse();
            }
            pageResponse = historyRepository.findByDeviceUuidAndUserIdIsNull(deviceUuid, pageable);
        }

        return AnalysisHistoryResponse.from(pageResponse.getContent());
    }
}