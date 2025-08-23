package com.example.konnect_backend.global.code.status;

import com.example.konnect_backend.global.code.BaseErrorCode;
import com.example.konnect_backend.global.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400","잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON401","인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"AUTH401","인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH403", "권한이 없습니다."),
    DATABASE_ERROR(HttpStatus.BAD_REQUEST, "COMMON404", "데이터베이스 에러가 발생하였습니다. 다시 시도해주십시오. "),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND,"COMMON405", "해당 Refresh Token을 찾을 수 없습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST,"COMMON406", "유효하지 않은 Refresh Token입니다."),


    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "MEMBER4001", "사용자가 없습니다."),
    EMAIL_NOT_FOUND(HttpStatus.BAD_REQUEST, "MEMBER4003", "이메일이 없습니다."),
    NICKNAME_NOT_EXIST(HttpStatus.BAD_REQUEST, "MEMBER4002", "닉네임은 필수 입니다."),
    EMAIL_FAILED(HttpStatus.BAD_REQUEST, "MEMBER4004","이메일 전송에 실패하였습니다."),
    EMAIL_DUPLICATE(HttpStatus.BAD_REQUEST,"MEMBER4005","이메일이 이미 존재합니다."),
    NICKNAME_DUPLICATE(HttpStatus.BAD_REQUEST,"MEMBER4006","닉네임이 이미 존재합니다."),
    TYPE_NOT_FOUND(HttpStatus.NOT_FOUND,"MEMBER4007", "해당 유형이 존재하지 않습니다."),
    PASSWORD_FAILED(HttpStatus.BAD_REQUEST, "MEMBER4008","계정이 존재하지 않거나 비밀번호가 틀렸습니다."),
    USER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST,"MEMBER4009","유저가 이미 존재합니다."),

    PASSWORD_VALIDATION_FAILED(HttpStatus.BAD_REQUEST,"MEMBER4010","비밀번호는 영어 대/소문자, 숫자 중 2종류 이상을 조합해야 하며 8글자에서 12글자 사이의 값이여야 합니다."),
    EMAIL_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "MEMBER4011","올바르지 않은 이메일 형식입니다."),

    JWT_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED, "AUTH001", "JWT 서명이 올바르지 않습니다."),
    JWT_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH002", "JWT 토큰이 만료되었습니다."),
    JWT_MALFORMED(HttpStatus.UNAUTHORIZED, "AUTH003", "JWT 토큰이 올바르지 않은 형식입니다."),
    GUEST_TOKEN_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH010", "인증이 필요합니다(게스트 토큰)."),
    GUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH011", "게스트 사용자가 존재하지 않습니다."),
    ALREADY_MEMBER(HttpStatus.BAD_REQUEST, "MEMBER4012", "이미 회원입니다."),
    SOCIAL_ID_DUPLICATE(HttpStatus.CONFLICT, "MEMBER4013", "이미 가입된 socialId 입니다."),
    CHILD_NOT_FOUND(HttpStatus.NOT_FOUND, "CHILD4001", "자녀를 찾을 수 없습니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "AUTH403", "접근 권한이 없습니다."),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "SCHEDULE4001", "일정을 찾을 수 없습니다."),


    // Chat Error
    CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND,"CHAT4001","채팅방을 찾을 수 없습니다."),
    JOINCHAT_ALREADY_EXIST(HttpStatus.BAD_REQUEST,"CHAT4002","이미 채팅방에 추가된 사용자입니다."),
    JOINCHAT_NOT_FOUND(HttpStatus.NOT_FOUND,"CHAT4003","해당 채팅방의 사용자를 찾을 수 없습니다."),
    ALREADY_JOINED_CHATROOM(HttpStatus.BAD_REQUEST,"CHAT4004","이미 참가한 채팅방입니다."),
    CHATROOM_FULL(HttpStatus.BAD_REQUEST,"CHAT4005","채팅방 최대 인원수에 도달했습니다."),


    PAGE_BOUND_ERROR(HttpStatus.BAD_REQUEST, "PAGE4001", "페이징 번호가 적절하지 않습니다."),


    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION4001", "조회할 알림 목록이 없습니다."),
    NOTIFICATION_ALREADY_READ(HttpStatus.BAD_REQUEST, "NOTIFICATION4002", "이미 읽음처리 된 알람입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE4001", "존재하지 않는 리소스입니다."),


    IMAGE_FAILED(HttpStatus.BAD_REQUEST,"IMAGE4001","이미지 올리는 것을 실패하였습니다."),

    IMAGE_TEXT_FAILD(HttpStatus.BAD_REQUEST, "IMAGETEXT4001", "이미지 텍스트 추출을 실패하였습니다.");




    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build()
                ;
    }
}
