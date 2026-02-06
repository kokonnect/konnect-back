package com.example.konnect_backend.domain.ai.controller;

import com.example.konnect_backend.domain.ai.dto.response.DocumentAnalysisResponse;
import com.example.konnect_backend.domain.ai.dto.response.TranslationHistoryResponse;
import com.example.konnect_backend.domain.ai.service.DocumentHistoryService;
import com.example.konnect_backend.domain.ai.service.model.UploadFile;
import com.example.konnect_backend.domain.ai.service.pipeline.DocumentAnalysisPipeline;
import com.example.konnect_backend.domain.ai.type.FileType;
import com.example.konnect_backend.global.ApiResponse;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Services", description = "AI 기반 서비스 API")
public class AIController {

    private final DocumentAnalysisPipeline documentAnalysisPipeline;
    private final DocumentHistoryService documentHistoryService;

    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    @Operation(summary = "가정통신문 분석",
               description = "가정통신문(PDF/이미지)을 분석하여 문서 유형 분류, 일정 추출, 번역, 요약을 수행합니다. " +
                   "사용자 설정 언어로 자동 번역됩니다. 중간에 실패 시 analysisId를 사용하여 재시도할 수 있습니다.")
    public ResponseEntity<ApiResponse<DocumentAnalysisResponse>> analyzeDocument(
        @RequestParam("file") MultipartFile multipartFile,
        @RequestParam("fileType") FileType fileType) {

        validateFileInput(multipartFile, fileType);

        log.info("문서 분석 API 요청: {}, 타입: {}",
            multipartFile.getOriginalFilename(), fileType);

        UploadFile file;
        try {
            file = new UploadFile(
                multipartFile.getOriginalFilename(),
                multipartFile.getContentType(), multipartFile.getSize(),
                multipartFile.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DocumentAnalysisResponse response = documentAnalysisPipeline.analyze(file, fileType);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/history")
    @Operation(summary = "분석 내역 조회", description = "사용자의 최근 문서 분석 내역을 최대 10개까지 조회합니다.")
    public ResponseEntity<ApiResponse<TranslationHistoryResponse>> getHistory() {
        TranslationHistoryResponse response = documentHistoryService.getHistory();
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    private void validateFileInput(MultipartFile file, FileType fileType) {
        if (file == null || file.isEmpty()) {
            throw new GeneralException(ErrorStatus.FILE_EMPTY);
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename()
                                                      .isBlank()) {
            throw new GeneralException(ErrorStatus.FILE_NAME_MISSING);
        }

        if (fileType == null) {
            throw new GeneralException(ErrorStatus.UNSUPPORTED_FILE_TYPE);
        }

        String contentType = file.getContentType();
        if (fileType == FileType.PDF && !"application/pdf".equals(
            contentType)) {
            throw new GeneralException(ErrorStatus.INVALID_PDF_FILE);
        }

        if (fileType == FileType.IMAGE && (contentType == null || !contentType.startsWith(
            "image/"))) {
            throw new GeneralException(ErrorStatus.INVALID_IMAGE_FILE);
        }
    }
}