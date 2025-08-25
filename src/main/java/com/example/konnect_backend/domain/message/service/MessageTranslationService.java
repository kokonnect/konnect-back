package com.example.konnect_backend.domain.message.service;

import com.example.konnect_backend.domain.message.dto.request.MessageComposeRequest;
import com.example.konnect_backend.domain.message.dto.response.MessageComposeResponse;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.entity.status.Language;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageTranslationService {
    
    private final ChatModel chatModel;
    private final UserRepository userRepository;
    
    private static final String MESSAGE_TRANSLATION_PROMPT_TEMPLATE = """
            다음 메시지를 {targetLanguage}로 번역해주세요.
            
            원본 메시지:
            {message}
            
            번역 지침:
            - 자연스럽고 정확한 번역을 해주세요
            - 메시지의 톤과 의도를 유지해주세요
            - 문맥과 의미를 충분히 고려해주세요
            - 번역문만 출력하고 다른 설명은 하지 마세요
            
            번역 결과:
            """;
    
    public MessageComposeResponse translateMessage(MessageComposeRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("메시지 번역 시작: 메시지 길이={}", request.getMessage().length());
            
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
    
    private String determineTargetLanguage(MessageComposeRequest request, User user) {
        // 1. 요청에 명시된 언어가 있으면 우선 사용
        if (request.getTargetLanguage() != null && !request.getTargetLanguage().trim().isEmpty()) {
            return request.getTargetLanguage().toLowerCase();
        }
        
        // 2. 사용자 설정 언어 사용
        if (user != null && user.getLanguage() != null) {
            return convertLanguageEnumToCode(user.getLanguage());
        }
        
        // 3. 기본값: 한국어
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
    
    private String translateText(String message, String targetLanguage) {
        try {
            PromptTemplate promptTemplate = new PromptTemplate(MESSAGE_TRANSLATION_PROMPT_TEMPLATE);
            Prompt prompt = promptTemplate.create(Map.of(
                    "message", message,
                    "targetLanguage", targetLanguage
            ));
            
            String result = chatModel.call(prompt).getResult().getOutput().getContent();
            
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