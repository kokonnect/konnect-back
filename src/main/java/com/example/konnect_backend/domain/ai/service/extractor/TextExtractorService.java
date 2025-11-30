package com.example.konnect_backend.domain.ai.service.extractor;

import com.example.konnect_backend.domain.ai.dto.internal.TextExtractionResult;
import org.springframework.web.multipart.MultipartFile;

public interface TextExtractorService {

    TextExtractionResult extract(MultipartFile file);

    boolean supports(String mimeType);
}
