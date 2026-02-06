package com.example.konnect_backend.domain.ai.service.extractor;

import com.example.konnect_backend.domain.ai.dto.internal.TextExtractionResult;
import com.example.konnect_backend.domain.ai.service.model.UploadFile;

public interface TextExtractorService {

    TextExtractionResult extract(UploadFile file);

    boolean supports(String mimeType);
}
