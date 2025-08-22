package com.example.konnect_backend.domain.ai.service;

import com.example.konnect_backend.domain.ai.dto.request.TranslationRequest;
import com.example.konnect_backend.domain.ai.dto.response.TranslationResponse;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.code.status.ErrorStatus;
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
public class TranslationService {
    
    private final ChatModel chatModel;
    
    private static final String TRANSLATION_PROMPT_TEMPLATE = """
            다음 {sourceLanguage} 텍스트를 한국어로 번역해주세요.
            {useSimpleLanguage}
            
            원본 텍스트:
            {text}
            
            번역 지침:
            - 자연스럽고 정확한 번역을 해주세요
            - 문맥과 의미를 충분히 고려해주세요
            - 번역문만 출력하고 다른 설명은 하지 마세요
            
            번역 결과:
            """;
    
    public TranslationResponse translate(TranslationRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("텍스트 번역 시작: {} -> 한국어", request.getSourceLanguage().getDisplayName());
            
            String simpleLanguageNote = request.isUseSimpleLanguage() 
                ? "가능한 한 간단하고 이해하기 쉬운 언어로 번역해주세요." 
                : "";
            
            PromptTemplate promptTemplate = new PromptTemplate(TRANSLATION_PROMPT_TEMPLATE);
            Prompt prompt = promptTemplate.create(Map.of(
                    "text", request.getText(),
                    "sourceLanguage", request.getSourceLanguage().getDisplayName(),
                    "useSimpleLanguage", simpleLanguageNote
            ));
            
            String translatedText = chatModel.call(prompt).getResult().getOutput().getContent();
            
            if (translatedText == null || translatedText.trim().isEmpty()) {
                log.error("번역 결과가 비어있음");
                throw new GeneralException(ErrorStatus.TRANSLATION_FAILED);
            }
            
            long endTime = System.currentTimeMillis();
            log.info("번역 완료: {}ms", endTime - startTime);
            
            return TranslationResponse.builder()
                    .originalText(request.getText())
                    .translatedText(translatedText.trim())
                    .sourceLanguage(request.getSourceLanguage())
                    .sourceLanguageName(request.getSourceLanguage().getDisplayName())
                    .targetLanguage("한국어")
                    .usedSimpleLanguage(request.isUseSimpleLanguage())
                    .originalTextLength(request.getText().length())
                    .translatedTextLength(translatedText.trim().length())
                    .processingTimeMs(endTime - startTime)
                    .build();
                    
        } catch (Exception e) {
            log.error("번역 중 오류 발생", e);
            throw new GeneralException(ErrorStatus.TRANSLATION_FAILED);
        }
    }
}
