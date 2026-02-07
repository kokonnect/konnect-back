package com.example.konnect_backend.domain.ai.service.textextractor;

import com.example.konnect_backend.domain.ai.exception.TextExtractionException;
import com.example.konnect_backend.domain.ai.model.vo.TextExtractionResult;
import com.example.konnect_backend.domain.ai.model.vo.UploadFile;
import com.example.konnect_backend.domain.ai.service.textextractor.ocr.OcrService;
import com.example.konnect_backend.domain.ai.type.FileType;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageTextExtractor implements TextExtractor {

    private final OcrService ocrService;

    @Override
    public TextExtractionResult extract(UploadFile file) {
        if (!ocrService.supports(file.mimeType())) {
            // Todo 열거형 상수 file type과 MIME type 분리
            throw new GeneralException(ErrorStatus.UNSUPPORTED_FILE_TYPE);
        }

        try {
            byte[] imageBytes = file.bytes();
            String extractedText = ocrService.extractText(imageBytes, file.mimeType());

            if (extractedText.trim().isEmpty()) {
                return TextExtractionResult.failure("이미지에서 텍스트를 추출할 수 없습니다");
            }

            String ocrMethod = ocrService.getServiceName();
            log.info("이미지 텍스트 추출 완료: {} 글자, OCR: {}", extractedText.length(), ocrMethod);
            return TextExtractionResult.success(extractedText, ocrMethod, 1);
        } catch (Exception e) {
            log.error("이미지 텍스트 추출 중 오류", e);
            throw new TextExtractionException(ErrorStatus.TEXT_EXTRACTION_FAILED);
        }
    }

    @Override
    public boolean supports(FileType fileType) {
        return fileType.equals(FileType.IMAGE);
    }
}
