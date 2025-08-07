package com.example.konnect_backend.global.exception;


import com.example.konnect_backend.global.code.BaseErrorCode;
import com.example.konnect_backend.global.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {
    private BaseErrorCode code;

    public ErrorReasonDTO getErrorReason(){
        return this.code.getReason();
    }

    public ErrorReasonDTO getErrorReasonHttpStatus(){
        return this.code.getReasonHttpStatus();
    }
}
