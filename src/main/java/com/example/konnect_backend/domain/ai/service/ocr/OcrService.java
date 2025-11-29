package com.example.konnect_backend.domain.ai.service.ocr;

public interface OcrService {

    String extractText(byte[] imageBytes, String mimeType);

    boolean supports(String mimeType);
}
