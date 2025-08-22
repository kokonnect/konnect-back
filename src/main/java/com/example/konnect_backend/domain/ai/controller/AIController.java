package com.example.konnect_backend.domain.ai.controller;

import com.example.konnect_backend.domain.ai.dto.request.GenerationRequest;
import com.example.konnect_backend.domain.ai.dto.request.FileTranslationRequest;
import com.example.konnect_backend.domain.ai.dto.request.TranslationRequest;
import com.example.konnect_backend.global.ApiResponse;
import com.example.konnect_backend.domain.ai.dto.response.GenerationResponse;
import com.example.konnect_backend.domain.ai.dto.response.FileTranslationResponse;
import com.example.konnect_backend.domain.ai.dto.response.TranslationResponse;
import com.example.konnect_backend.domain.ai.service.GenerationService;
import com.example.konnect_backend.domain.ai.service.FileTranslationService;
import com.example.konnect_backend.domain.ai.service.TranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.konnect_backend.domain.ai.dto.FileType;
import com.example.konnect_backend.domain.ai.dto.TargetLanguage;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Services", description = "AI 기반 서비스 API")
public class AIController {

    private final TranslationService translationService;
    private final FileTranslationService fileTranslationService;
    private final GenerationService generationService;

    @PostMapping("/translate/text")
    @Operation(summary = "텍스트 번역", description = "외국어를 한국어로 번역합니다.")
    public ResponseEntity<ApiResponse<TranslationResponse>> translate(
            @Valid @RequestBody TranslationRequest request) {

        TranslationResponse response = translationService.translate(request);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
    
    @PostMapping(value = "/translate", consumes = "multipart/form-data")
    @Operation(summary = "파일 번역", description = "PDF 또는 이미지 파일의 텍스트를 추출하여 번역합니다. OpenAI Vision API를 사용합니다.")
    public ResponseEntity<ApiResponse<FileTranslationResponse>> translateFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") FileType fileType,
            @RequestParam("targetLanguage") TargetLanguage targetLanguage,
            @RequestParam(value = "useSimpleLanguage", defaultValue = "true") Boolean useSimpleLanguage,
            @RequestParam(value = "sourceLanguageHint", required = false) String sourceLanguageHint) {

        try {
            FileTranslationRequest request = new FileTranslationRequest(
                    file, fileType, targetLanguage, useSimpleLanguage, sourceLanguageHint);

            FileTranslationResponse response = fileTranslationService.translateFile(request);
            
            return ResponseEntity.ok(ApiResponse.onSuccess(response));
            
        } catch (Exception e) {
            log.error("파일 번역 API 오류: {}", e.getMessage(), e);
            throw e;
        }
    }

    /*
    @PostMapping("/generate")
    @Operation(summary = "콘텐츠 생성", description = "AI를 사용하여 콘텐츠를 생성합니다.")
    public ResponseEntity<ApiResponse<GenerationResponse>> generate(
            @Valid @RequestBody GenerationRequest request) {

        GenerationResponse response = generationService.generate(request);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
    */

    @GetMapping("/languages")
    @Operation(summary = "지원 언어 목록", description = "번역 가능한 언어 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSupportedLanguages() {

        var sourceLanguages = Arrays.stream(TranslationRequest.LanguageType.values())
                .filter(lang -> lang != TranslationRequest.LanguageType.KOREAN)
                .collect(Collectors.toMap(
                        Enum::name,
                        Enum::name
                ));

        Map<String, Object> result = Map.of(
                "sourceLanguages", sourceLanguages,
                "targetLanguage", "KOREAN",
                "description", "모든 언어는 한국어로 번역됩니다"
        );

        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }
}
