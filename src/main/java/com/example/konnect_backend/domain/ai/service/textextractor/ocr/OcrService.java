package com.example.konnect_backend.domain.ai.service.textextractor.ocr;

public interface OcrService {

    String extractText(byte[] imageBytes, String mimeType);

    boolean supports(String mimeType);

    /**
     * OCR 서비스의 이름을 반환합니다. 기본적으로 클래스 이름을 반환합니다.
     * @return OCR 서비스의 이름 (기본 클래스 이름)
     */
    default String getServiceName() {
        return this.getClass().getSimpleName();
    }
}
