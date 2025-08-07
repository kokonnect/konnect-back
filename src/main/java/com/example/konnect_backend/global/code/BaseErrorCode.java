package com.example.konnect_backend.global.code;

public interface BaseErrorCode {
    ErrorReasonDTO getReason();
    ErrorReasonDTO getReasonHttpStatus();
}
