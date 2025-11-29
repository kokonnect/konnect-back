package com.example.konnect_backend.domain.ai.service.ocr;

import com.example.konnect_backend.domain.ai.exception.OcrException;
import com.example.konnect_backend.domain.ai.service.GeminiService;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Base64;

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

    private static final String OCR_PROMPT = """
            이 이미지에서 모든 텍스트를 추출해주세요.

            ## 추출 지침
            - 이미지에 보이는 모든 텍스트를 정확하게 추출
            - 원본 텍스트의 줄바꿈과 문단 구조 유지
            - 표가 있는 경우 텍스트 내용만 추출 (표 형식 유지 불필요)
            - 손글씨도 가능한 한 정확하게 인식
            - 텍스트가 없는 경우 빈 문자열 반환
            - 추출된 텍스트만 출력하고 다른 설명은 하지 마세요

            ## 출력 형식
            추출된 텍스트를 그대로 출력
            """;

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
