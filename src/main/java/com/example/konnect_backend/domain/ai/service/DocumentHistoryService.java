package com.example.konnect_backend.domain.ai.service;

import com.example.konnect_backend.domain.ai.dto.FileType;
import com.example.konnect_backend.domain.ai.dto.response.TranslationHistoryResponse;
import com.example.konnect_backend.domain.document.entity.Document;
import com.example.konnect_backend.domain.document.entity.DocumentFile;
import com.example.konnect_backend.domain.document.entity.DocumentTranslation;
import com.example.konnect_backend.domain.document.repository.DocumentRepository;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 문서 분석/번역 내역 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentHistoryService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    private static final int DEFAULT_HISTORY_LIMIT = 10;

    @Transactional(readOnly = true)
    public TranslationHistoryResponse getHistory() {
        return getHistory(DEFAULT_HISTORY_LIMIT);
    }

    @Transactional(readOnly = true)
    public TranslationHistoryResponse getHistory(int limit) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        if (userId == null) {
            log.info("인증되지 않은 사용자의 내역 조회 요청");
            return TranslationHistoryResponse.builder()
                    .histories(List.of())
                    .totalCount(0)
                    .build();
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("존재하지 않는 사용자: userId={}", userId);
            return TranslationHistoryResponse.builder()
                    .histories(List.of())
                    .totalCount(0)
                    .build();
        }

        log.info("내역 조회 시작: userId={}, isGuest={}", userId, user.isGuest());

        try {
            Pageable pageable = PageRequest.of(0, limit);

            // 1단계: Document와 DocumentFile을 함께 조회
            List<Document> documents = documentRepository.findByUserWithFilesOrderByCreatedAtDesc(user, pageable);

            // 2단계: 조회된 Document들의 Translation을 조회
            if (!documents.isEmpty()) {
                documents = documentRepository.findWithTranslationsByDocuments(documents);
            }

            // DTO로 변환
            List<TranslationHistoryResponse.TranslationHistoryItem> histories = documents.stream()
                    .map(this::convertToHistoryItem)
                    .collect(Collectors.toList());

            log.info("내역 조회 완료: 총 {}개", histories.size());

            return TranslationHistoryResponse.builder()
                    .histories(histories)
                    .totalCount(histories.size())
                    .build();

        } catch (Exception e) {
            log.error("내역 조회 중 오류 발생", e);
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }

    private TranslationHistoryResponse.TranslationHistoryItem convertToHistoryItem(Document document) {
        DocumentFile documentFile = document.getDocumentFiles().stream().findFirst().orElse(null);
        DocumentTranslation translation = document.getTranslations().stream().findFirst().orElse(null);

        return TranslationHistoryResponse.TranslationHistoryItem.builder()
                .documentId(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .createdAt(document.getCreatedAt())
                // 파일 정보
                .fileName(documentFile != null ? documentFile.getFileName() : null)
                .fileType(documentFile != null ? FileType.valueOf(documentFile.getFileType()) : null)
                .fileSize(documentFile != null ? documentFile.getFileSize() : null)
                .pageCount(documentFile != null ? documentFile.getPageCount() : null)
                .extractedText(documentFile != null ? documentFile.getExtractedText() : null)
                // 번역 정보
                .translatedLanguage(translation != null ? translation.getTranslatedLanguage() : null)
                .translatedText(translation != null ? translation.getTranslatedText() : null)
                .summary(translation != null ? translation.getSummary() : null)
                .build();
    }
}