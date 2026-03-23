package com.example.konnect_backend.domain.message.service;

import com.example.konnect_backend.domain.ai.infra.GeminiService;
import com.example.konnect_backend.domain.message.dto.request.MessageComposeRequest;
import com.example.konnect_backend.domain.message.dto.response.MessageComposeResponse;
import com.example.konnect_backend.domain.message.dto.response.MessageHistoryResponse;
import com.example.konnect_backend.domain.message.entity.UserGeneratedMessage;
import com.example.konnect_backend.domain.message.repository.UserGeneratedMessageRepository;
import com.example.konnect_backend.domain.user.entity.Device;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.entity.status.Language;
import com.example.konnect_backend.domain.user.entity.status.UsageType;
import com.example.konnect_backend.domain.user.repository.DeviceRepository;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.domain.user.service.UsageFacade;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.MDC;

import java.util.List;
import java.util.UUID;
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
    private final UsageFacade usageFacade;
    private final DeviceRepository deviceRepository;

    private static final String MESSAGE_GENERATION_PROMPT_TEMPLATE = """
당신은 외국인 학부모를 대신해 한국 학교 선생님에게 보낼 메시지를 작성하는 도우미입니다.

[상황]
%s

[작성 규칙]
- 메시지는 반드시 한국어로 작성하세요
- 실제 학부모가 선생님께 보내는 자연스럽고 공손한 메시지로 작성하세요
- 상황에 대한 간단한 설명 + 양해 표현 + 마무리 인사를 포함하세요
- 3~5문장 정도로 적절한 길이로 작성하세요
- 너무 딱딱한 шаблон 문장 대신 자연스럽게 작성하세요

[중요 규칙]
- placeholder는 반드시 아래 형식을 그대로 사용하세요 (절대 변경 금지)
%s
- 위 placeholder를 그대로 사용하세요
- 다른 placeholder를 만들지 마세요

[출력 규칙]
- 설명 금지
- 하나의 메시지만 출력

메시지:
""";

    @Transactional
    public MessageComposeResponse generatedMessage(MessageComposeRequest request, String deviceUuid){
        long startTime = System.currentTimeMillis();

        try {
            MDC.put("requestId", UUID.randomUUID().toString());

            // 현재 로그인한 사용자 정보 가져오기 (게스트 사용자 포함)
            Long userId = SecurityUtil.getCurrentUserIdOrNull();
            User user = null;
            if (userId != null) {
                user = userRepository.findById(userId).orElse(null);
            }

            if (user == null && (deviceUuid == null || deviceUuid.isBlank())) {
                throw new GeneralException(ErrorStatus.INVALID_DEVICE);
            }

            usageFacade.validateAndIncrease(UsageType.MESSAGE, deviceUuid);

            // 번역 대상 언어 결정 (요청에 지정된 언어 > 사용자 설정 언어 > 기본값 한국어)
            String targetLanguage = determineTargetLanguage(request, user, deviceUuid);

            if (request.getTargetLanguage() != null && !request.getTargetLanguage().isBlank()) {
                targetLanguage = request.getTargetLanguage();
            }

            log.info("번역 대상 언어: {} (사용자: {})", targetLanguage, user != null ? user.getId() : "guest");

            // 메시지 번역
            String generatedMessage = generateMessage(request.getMessage(), targetLanguage);

            UserGeneratedMessage entity;

            if (user != null) {
                entity = UserGeneratedMessage.builder()
                        .user(user)
                        .deviceUuid(deviceUuid)
                        .inputPrompt(request.getMessage())
                        .generatedKorean(generatedMessage)
                        .build();
            } else {
                entity = UserGeneratedMessage.builder()
                        .user(null)
                        .deviceUuid(deviceUuid)
                        .inputPrompt(request.getMessage())
                        .generatedKorean(generatedMessage)
                        .build();
            }

            userGeneratedMessageRepository.save(entity);

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            return MessageComposeResponse.builder()
                    .originalMessage(request.getMessage())
                    .generatedMessage(generatedMessage)
                    .targetLanguage(targetLanguage)
                    .processingTimeMs(processingTime)
                    .build();

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("메시지 번역 중 예상치 못한 오류 발생", e);
            throw new GeneralException(ErrorStatus.TRANSLATION_FAILED);
        } finally {
            MDC.remove("key");
        }
    }

    /**
     * 현재 로그인한 사용자의 메시지 번역 히스토리 조회
     */
    @Transactional(readOnly = true)
    public List<MessageHistoryResponse> getMessageHistory(String deviceUuid) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();

        List<UserGeneratedMessage> messages;

        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

            messages = userGeneratedMessageRepository
                    .findByUserOrderByCreatedAtDesc(user);

        } else {
            if (deviceUuid == null || deviceUuid.isBlank()) {
                throw new GeneralException(ErrorStatus.INVALID_DEVICE);
            }

            messages = userGeneratedMessageRepository
                    .findByDeviceUuidAndUserIsNullOrderByCreatedAtDesc(deviceUuid);
        }


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

    private String determineTargetLanguage(MessageComposeRequest request, User user, String deviceUuid) {

        // user 우선
        if (user != null && user.getLanguage() != null) {
            return convertLanguageEnumToCode(user.getLanguage());
        }

        // device fallback
        if (deviceUuid != null) {
            return deviceRepository.findById(deviceUuid)
                    .map(Device::getLanguage)
                    .filter(lang -> lang != null)
                    .map(this::convertLanguageEnumToCode)
                    .orElse("ko");
        }

        // 그외
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

    private String getPlaceholder(String lang) {
        return switch (lang) {
            case "ko" -> "[선생님 이름], [학생 이름], [학부모 이름]";
            case "en" -> "[Teacher Name], [Child Name], [Parent Name]";
            case "vi" -> "[Tên giáo viên], [Tên học sinh], [Tên phụ huynh]";
            case "zh" -> "[老师姓名], [学生姓名], [家长姓名]";
            case "ja" -> "[先生の名前], [生徒の名前], [保護者の名前]";
            case "th" -> "[ชื่อครู], [ชื่อนักเรียน], [ชื่อผู้ปกครอง]";
            case "tl" -> "[Pangalan ng Guro], [Pangalan ng Mag-aaral], [Pangalan ng Magulang]";
            case "km" -> "[ឈ្មោះគ្រូ], [ឈ្មោះសិស្ស], [ឈ្មោះមាតាបិតា]";
            default -> "[Teacher Name], [Child Name], [Parent Name]";
        };
    }

    /**
     * 텍스트 번역 (Gemini Lite 모델 사용)
     */
    private String generateMessage(String message, String targetLanguage) {
        try {
            String placeholder = getPlaceholder(targetLanguage);

            String prompt = String.format(
                    MESSAGE_GENERATION_PROMPT_TEMPLATE,
                    message,
                    placeholder
            );

            // Gemini Lite 모델 사용 (단순 번역)
            String result = geminiService.generateSimpleContent(prompt, 0.3, 2000).response();

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
