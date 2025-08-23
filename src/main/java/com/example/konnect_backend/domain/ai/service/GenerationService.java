package com.example.konnect_backend.domain.ai.service;

import com.example.konnect_backend.domain.ai.dto.request.GenerationRequest;
import com.example.konnect_backend.domain.ai.dto.response.GenerationResponse;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerationService {
    
    private final ChatModel chatModel;
    
    public GenerationResponse generate(GenerationRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("콘텐츠 생성 시작: {}", request.getContentType());
            
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withMaxTokens(request.getMaxTokens())
                    .withTemperature(request.getTemperature().floatValue())
                    .build();
            
            Prompt prompt = new Prompt(request.getPrompt(), options);
            
            var result = chatModel.call(prompt).getResult();
            String generatedContent = result.getOutput().getContent();
            
            if (generatedContent == null || generatedContent.trim().isEmpty()) {
                log.error("생성 결과가 비어있음");
                throw new GeneralException(ErrorStatus.GENERATION_FAILED);
            }
            
            long endTime = System.currentTimeMillis();
            
            return GenerationResponse.builder()
                    .prompt(request.getPrompt())
                    .generatedContent(generatedContent.trim())
                    .contentType(request.getContentType())
                    .processingTimeMs(endTime - startTime)
                    .temperature(request.getTemperature())
                    .maxTokens(request.getMaxTokens())
                    .build();
                    
        } catch (Exception e) {
            log.error("콘텐츠 생성 중 오류 발생", e);
            throw new GeneralException(ErrorStatus.GENERATION_FAILED);
        }
    }
}
