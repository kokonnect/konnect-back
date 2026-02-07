package com.example.konnect_backend.domain.ai.infra;

import com.example.konnect_backend.domain.ai.exception.OcrException;
import com.example.konnect_backend.domain.ai.service.textextractor.ocr.OcrService;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Base64;

import static com.example.konnect_backend.domain.ai.service.prompt.OcrPrompt.OCR_PROMPT;

/**
 * Gemini Vision OCR 서비스
 *
 * ## 모델: gemini-2.0-flash (Vision)
 * - 이유: Gemini Vision은 이미지 내 텍스트 인식에 뛰어남
 * - 멀티모달 지원으로 이미지 + 프롬프트 동시 처리
 * - 한국어 OCR 품질이 우수함
 * - Tesseract 대비 인식률 높음
 *
 * ## RPD 고려사항
 * - Primary 모델 사용 (200회/일 제한)
 * - 이미지 OCR은 필수 기능이므로 Primary 모델 사용
 * - 호출 제한 도달 시 OcrException 발생
 */
@Service("geminiVisionOcr")
@Primary  // Gemini Vision을 기본 OCR로 사용
@RequiredArgsConstructor
@Slf4j
public class GeminiVisionOcrService implements OcrService {

    private final GeminiService geminiService;

    private static final double TEMPERATURE = 0.1;  // 정확한 텍스트 추출 위해 낮은 온도
    private static final int MAX_TOKENS = 8000;     // 긴 문서 텍스트 추출을 위해 충분한 토큰

    @Override
    public String extractText(byte[] imageBytes, String mimeType) {
        try {
            log.info("Gemini Vision OCR 시작, 이미지 크기: {} bytes, MIME: {}", imageBytes.length, mimeType);

            // 이미지를 Base64로 인코딩
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Gemini Vision API 호출
            String extractedText = geminiService.generateContentWithImage(
                    OCR_PROMPT,
                    base64Image,
                    mimeType,
                    TEMPERATURE,
                    MAX_TOKENS
            );

            if (extractedText == null || extractedText.trim().isEmpty()) {
                log.warn("Gemini Vision OCR: 텍스트 추출 결과 없음");
                return "";
            }

            log.info("Gemini Vision OCR 완료: {} 글자 추출", extractedText.length());
            return extractedText.trim();

        } catch (Exception e) {
            log.error("Gemini Vision OCR 처리 중 오류: {}", e.getMessage(), e);
            throw new OcrException(ErrorStatus.OCR_FAILED);
        }
    }

    @Override
    public boolean supports(String mimeType) {
        // Gemini Vision이 지원하는 이미지 형식
        return mimeType != null && (
                mimeType.equals("image/jpeg") ||
                mimeType.equals("image/jpg") ||
                mimeType.equals("image/png") ||
                mimeType.equals("image/gif") ||
                mimeType.equals("image/webp") ||
                mimeType.equals("image/heic") ||
                mimeType.equals("image/heif")
        );
    }
}
