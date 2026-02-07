package com.example.konnect_backend.domain.message.service;

import com.example.konnect_backend.domain.ai.infra.GeminiService;
import com.example.konnect_backend.domain.message.dto.request.MessageComposeRequest;
import com.example.konnect_backend.domain.message.dto.response.MessageComposeResponse;
import com.example.konnect_backend.domain.message.dto.response.MessageHistoryResponse;
import com.example.konnect_backend.domain.message.entity.UserGeneratedMessage;
import com.example.konnect_backend.domain.message.repository.UserGeneratedMessageRepository;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.entity.status.Language;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 메시지 번역 서비스 (Gemini API 사용)
 *
 * ## 모델 선택: gemini-2.0-flash-lite
 * - 이유: 메시지 번역은 단순한 텍스트 변환 작업
 * - RPD: 1,000회/일로 여유로움
 * - 빠른 응답 속도, 비용 효율적
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageTranslationService {

    private final GeminiService geminiService;
    private final UserRepository userRepository;
    private final UserGeneratedMessageRepository userGeneratedMessageRepository;

    private static final String MESSAGE_TRANSLATION_PROMPT_TEMPLATE = """
            다음 메시지를 %s로 번역해주세요.

            원본 메시지:
            %s

            번역 지침:
            - 자연스럽고 정확한 번역을 해주세요
            - 메시지의 톤과 의도를 유지해주세요
            - 문맥과 의미를 충분히 고려해주세요
            - 번역문만 출력하고 다른 설명은 하지 마세요

            번역 결과:
            """;

    @Transactional
    public MessageComposeResponse translateMessage(MessageComposeRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("메시지 번역 시작 (Gemini Lite): 메시지 길이={}", request.getMessage().length());

            // 현재 로그인한 사용자 정보 가져오기 (게스트 사용자 포함)
            Long userId = SecurityUtil.getCurrentUserIdOrNull();
            User user = null;
            if (userId != null) {
                user = userRepository.findById(userId).orElse(null);
            }

            // 번역 대상 언어 결정 (요청에 지정된 언어 > 사용자 설정 언어 > 기본값 한국어)
            String targetLanguage = determineTargetLanguage(request, user);
            String targetLanguageName = getLanguageDisplayName(targetLanguage);

            log.info("번역 대상 언어: {} (사용자: {})", targetLanguage, user != null ? user.getId() : "guest");

            // 메시지 번역
            String translatedMessage = translateText(request.getMessage(), targetLanguageName);

            // 로그인한 사용자인 경우 DB에 저장
            if (user != null) {
                UserGeneratedMessage userGeneratedMessage = UserGeneratedMessage.builder()
                        .user(user)
                        .inputPrompt(request.getMessage())
                        .generatedKorean(translatedMessage)
                        .build();
                userGeneratedMessageRepository.save(userGeneratedMessage);
                log.info("번역 결과 DB 저장 완료: userId={}", user.getId());
            } else {
                log.info("게스트 사용자는 DB에 저장하지 않음");
            }

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            log.info("메시지 번역 완료: 처리시간={}ms", processingTime);

            return MessageComposeResponse.builder()
                    .originalMessage(request.getMessage())
                    .translatedMessage(translatedMessage)
                    .targetLanguage(targetLanguage)
                    .processingTimeMs(processingTime)
                    .build();

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("메시지 번역 중 예상치 못한 오류 발생", e);
            throw new GeneralException(ErrorStatus.TRANSLATION_FAILED);
        }
    }

    /**
     * 현재 로그인한 사용자의 메시지 번역 히스토리 조회
     */
    @Transactional(readOnly = true)
    public List<MessageHistoryResponse> getMessageHistory() {
        // 현재 로그인한 사용자 조회
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        if (userId == null) {
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        log.info("메시지 히스토리 조회: userId={}", userId);

        // 사용자의 생성된 메시지 목록 조회 (최신순)
        List<UserGeneratedMessage> messages = userGeneratedMessageRepository
                .findByUserOrderByCreatedAtDesc(user);

        log.info("조회된 메시지 개수: {}", messages.size());

        // DTO로 변환
        return messages.stream()
                .map(message -> MessageHistoryResponse.builder()
                        .id(message.getId())
                        .inputPrompt(message.getInputPrompt())
                        .generatedKorean(message.getGeneratedKorean())
                        .createdAt(message.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private String determineTargetLanguage(MessageComposeRequest request, User user) {
        // 무조건 한국어로 번역
        return "ko";
    }

    private String convertLanguageEnumToCode(Language language) {
        return switch (language) {
            case KOREAN -> "ko";
            case ENGLISH -> "en";
            case VIETNAMESE -> "vi";
            case CHINESE -> "zh";
            case JAPANESE -> "ja";
            case THAI -> "th";
            case FILIPINO -> "tl";
            case KHMER -> "km";
        };
    }

    private String getLanguageDisplayName(String languageCode) {
        return switch (languageCode.toLowerCase()) {
            case "ko" -> "한국어";
            case "en" -> "영어";
            case "vi" -> "베트남어";
            case "zh" -> "중국어";
            case "ja" -> "일본어";
            case "th" -> "태국어";
            case "tl" -> "필리핀어";
            case "km" -> "캄보디아어";
            default -> "한국어";
        };
    }

    /**
     * 텍스트 번역 (Gemini Lite 모델 사용)
     */
    private String translateText(String message, String targetLanguage) {
        try {
            String prompt = String.format(MESSAGE_TRANSLATION_PROMPT_TEMPLATE,
                    targetLanguage,
                    message);

            // Gemini Lite 모델 사용 (단순 번역)
            String result = geminiService.generateSimpleContent(prompt, 0.3, 2000);

            if (result == null || result.trim().isEmpty()) {
                log.error("메시지 번역 결과가 비어있음");
                throw new GeneralException(ErrorStatus.TRANSLATION_FAILED);
            }

            return result.trim();

        } catch (Exception e) {
            log.error("메시지 번역 중 오류 발생", e);
            throw new GeneralException(ErrorStatus.TRANSLATION_FAILED);
        }
    }
}
