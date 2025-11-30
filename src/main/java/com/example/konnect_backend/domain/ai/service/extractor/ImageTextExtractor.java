package com.example.konnect_backend.domain.ai.service.extractor;

import com.example.konnect_backend.domain.ai.dto.internal.TextExtractionResult;
import com.example.konnect_backend.domain.ai.exception.TextExtractionException;
import com.example.konnect_backend.domain.ai.service.ocr.OcrService;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageTextExtractor implements TextExtractorService {

    private final OcrService ocrService;

    @Override
    public TextExtractionResult extract(MultipartFile file) {
        try {
            log.info("이미지 텍스트 추출 시작: {}", file.getOriginalFilename());

            String mimeType = file.getContentType();
            if (!supports(mimeType)) {
                throw new TextExtractionException(ErrorStatus.INVALID_IMAGE_FILE);
            }

            byte[] imageBytes = file.getBytes();
            String extractedText = ocrService.extractText(imageBytes, mimeType);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                log.warn("이미지에서 텍스트를 추출할 수 없음");
                return TextExtractionResult.failure("이미지에서 텍스트를 추출할 수 없습니다");
            }

            String ocrMethod = ocrService.getServiceName();
            log.info("이미지 텍스트 추출 완료: {} 글자, OCR: {}", extractedText.length(), ocrMethod);
            return TextExtractionResult.success(extractedText, ocrMethod, 1);

        } catch (TextExtractionException e) {
            throw e;
        } catch (Exception e) {
            log.error("이미지 텍스트 추출 중 오류", e);
            throw new TextExtractionException(ErrorStatus.TEXT_EXTRACTION_FAILED);
        }
    }

    @Override
    public boolean supports(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }
}
