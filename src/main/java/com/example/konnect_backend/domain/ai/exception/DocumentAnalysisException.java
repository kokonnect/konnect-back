package com.example.konnect_backend.domain.ai.exception;

import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;

public class DocumentAnalysisException extends GeneralException {

    public DocumentAnalysisException(ErrorStatus errorStatus) {
        super(errorStatus);
    }
}
