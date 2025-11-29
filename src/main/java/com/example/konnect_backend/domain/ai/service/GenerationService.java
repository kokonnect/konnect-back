package com.example.konnect_backend.domain.ai.service;

import com.example.konnect_backend.domain.ai.dto.request.GenerationRequest;
import com.example.konnect_backend.domain.ai.dto.response.GenerationResponse;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 콘텐츠 생성 서비스 (Gemini API 사용)
 *
 * ## 모델 선택 전략
 * - 일반 생성: gemini-2.0-flash-lite (단순 텍스트 생성)
 * - 복잡한 생성: gemini-2.0-flash (정확도가 중요한 경우)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GenerationService {

    private final GeminiService geminiService;

    public GenerationResponse generate(GenerationRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("콘텐츠 생성 시작 (Gemini): {}", request.getContentType());

            // Gemini Lite 모델로 콘텐츠 생성 (단순 텍스트 생성)
            String generatedContent = geminiService.generateSimpleContent(
                    request.getPrompt(),
                    request.getTemperature(),
                    request.getMaxTokens()
            );

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
