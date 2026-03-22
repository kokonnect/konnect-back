package com.example.konnect_backend.domain.ai.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextExtractionResult {

    private String text;

    private String ocrMethod;

    private Integer pageCount;

    private boolean success;

    private String errorMessage;

    public static TextExtractionResult success(String text, String ocrMethod, int pageCount) {
        return TextExtractionResult.builder()
                .text(text)
                .ocrMethod(ocrMethod)
                .pageCount(pageCount)
                .success(true)
                .build();
    }

    public static TextExtractionResult failure(String errorMessage) {
        return TextExtractionResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    public boolean isFailed() {
        if (!success || text == null || text.trim().isEmpty()) {
            return true;
        }

        return false;
    }
}
