package com.example.konnect_backend.domain.ai.controller;

import com.example.konnect_backend.domain.ai.dto.request.FileTranslationRequest;
import com.example.konnect_backend.domain.ai.dto.request.TranslationRequest;
import com.example.konnect_backend.domain.ai.dto.response.DocumentAnalysisResponse;
import com.example.konnect_backend.global.ApiResponse;
import com.example.konnect_backend.domain.ai.dto.response.FileTranslationResponse;
import com.example.konnect_backend.domain.ai.dto.response.TranslationHistoryResponse;
import com.example.konnect_backend.domain.ai.service.FileTranslationService;
import com.example.konnect_backend.domain.ai.service.pipeline.DocumentAnalysisPipeline;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    private final FileTranslationService fileTranslationService;
    private final DocumentAnalysisPipeline documentAnalysisPipeline;
    
    @PostMapping(value = "/translate", consumes = "multipart/form-data")
    @Operation(summary = "파일 번역", description = "PDF 또는 이미지 파일의 텍스트를 추출하여 번역합니다.")
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
    
    @GetMapping("/translate/history")
    @Operation(summary = "번역 내역 조회", description = "사용자의 최근 번역 내역을 최대 10개까지 조회합니다.")
    public ResponseEntity<ApiResponse<TranslationHistoryResponse>> getTranslationHistory() {

        try {
            TranslationHistoryResponse response = fileTranslationService.getTranslationHistory();

            return ResponseEntity.ok(ApiResponse.onSuccess(response));

        } catch (Exception e) {
            log.error("번역 내역 조회 API 오류: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    @Operation(summary = "가정통신문 분석",
            description = "가정통신문(PDF/이미지)을 분석하여 문서 유형 분류, 일정 추출, 번역, 요약을 수행합니다. " +
                    "사용자 설정 언어로 자동 번역됩니다. 중간에 실패 시 analysisId를 사용하여 재시도할 수 있습니다.")
    public ResponseEntity<ApiResponse<DocumentAnalysisResponse>> analyzeDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") FileType fileType) {

        log.info("문서 분석 API 요청: {}, 타입: {}", file.getOriginalFilename(), fileType);

        DocumentAnalysisResponse response = documentAnalysisPipeline.analyze(file, fileType);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PostMapping(value = "/analyze/retry", consumes = "multipart/form-data")
    @Operation(summary = "가정통신문 분석 재시도",
            description = "이전에 실패한 분석을 재시도합니다. analysisId와 동일한 파일을 전송해야 합니다. " +
                    "이미 완료된 단계는 건너뛰고 실패한 단계부터 재개합니다. " +
                    "캐시 유효기간은 30분입니다.")
    public ResponseEntity<ApiResponse<DocumentAnalysisResponse>> retryAnalysis(
            @RequestParam("analysisId") Long analysisId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") FileType fileType) {

        log.info("문서 분석 재시도 API 요청: analysisId={}, 파일={}", analysisId, file.getOriginalFilename());

        DocumentAnalysisResponse response = documentAnalysisPipeline.retry(analysisId, file, fileType);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
