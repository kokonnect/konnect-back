package com.example.konnect_backend.domain.ai.exception;

import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;

public class TextExtractionException extends GeneralException {

    public TextExtractionException() {
        super(ErrorStatus.TEXT_EXTRACTION_FAILED);
    }

    public TextExtractionException(ErrorStatus errorStatus) {
        super(errorStatus);
    }
}
