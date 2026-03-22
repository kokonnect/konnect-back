package com.example.konnect_backend.domain.ai.exception;

import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;

public class OcrException extends GeneralException {
    public OcrException(ErrorStatus errorStatus) {
        super(errorStatus);
    }
}
