package com.example.konnect_backend.domain.ai.service.textextractor;

import com.example.konnect_backend.domain.ai.model.vo.TextExtractionResult;
import com.example.konnect_backend.domain.ai.model.vo.UploadFile;
import com.example.konnect_backend.domain.ai.type.FileType;

public interface TextExtractor {

    TextExtractionResult extract(UploadFile file);

    boolean supports(FileType fileType);
}
