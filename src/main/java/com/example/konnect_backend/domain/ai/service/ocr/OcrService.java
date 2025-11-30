package com.example.konnect_backend.domain.ai.service.ocr;

public interface OcrService {

    String extractText(byte[] imageBytes, String mimeType);

    boolean supports(String mimeType);

    /**
     * OCR 서비스 이름 반환
     */
    default String getServiceName() {
        return this.getClass().getSimpleName();
    }
}
