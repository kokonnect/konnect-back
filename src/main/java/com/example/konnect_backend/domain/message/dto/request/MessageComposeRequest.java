package com.example.konnect_backend.domain.message.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageComposeRequest {
    
    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Size(max = 2000, message = "메시지는 2000자를 초과할 수 없습니다.")
    private String message;
    
    @Size(max = 10, message = "언어 코드는 10자를 초과할 수 없습니다.")
    private String targetLanguage; // 선택적: 특정 언어로 번역 요청 시 사용 (사용자 설정 언어보다 우선)
}