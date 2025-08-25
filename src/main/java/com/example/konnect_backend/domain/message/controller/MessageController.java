package com.example.konnect_backend.domain.message.controller;

import com.example.konnect_backend.domain.message.dto.request.MessageComposeRequest;
import com.example.konnect_backend.domain.message.dto.response.MessageComposeResponse;
import com.example.konnect_backend.domain.message.service.MessageTranslationService;
import com.example.konnect_backend.global.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Message Services", description = "메시지 관련 서비스 API")
public class MessageController {

    private final MessageTranslationService messageTranslationService;

    @PostMapping("/compose")
    @Operation(summary = "메시지 번역 작성", description = "메시지를 사용자 설정 언어로 번역합니다.")
    public ResponseEntity<ApiResponse<MessageComposeResponse>> composeMessage(
            @RequestBody @Valid MessageComposeRequest request) {
        
        try {
            log.info("메시지 번역 요청: 메시지 길이={}, 대상언어={}", 
                    request.getMessage().length(), request.getTargetLanguage());
            
            MessageComposeResponse response = messageTranslationService.translateMessage(request);
            
            return ResponseEntity.ok(ApiResponse.onSuccess(response));
            
        } catch (Exception e) {
            log.error("메시지 번역 API 오류: {}", e.getMessage(), e);
            throw e;
        }
    }
}