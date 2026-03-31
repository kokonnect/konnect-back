package com.example.konnect_backend.domain.ai.controller;

import com.example.konnect_backend.domain.ai.dto.request.CreatePromptRequest;
import com.example.konnect_backend.domain.ai.dto.request.RunPromptRequest;
import com.example.konnect_backend.domain.ai.dto.response.*;
import com.example.konnect_backend.domain.ai.model.vo.TextExtractionResult;
import com.example.konnect_backend.domain.ai.model.vo.UploadFile;
import com.example.konnect_backend.domain.ai.service.pipeline.PipelineContext;
import com.example.konnect_backend.domain.ai.service.prompt.management.PromptManagementService;
import com.example.konnect_backend.domain.ai.service.textextractor.TextExtractorFacade;
import com.example.konnect_backend.domain.ai.type.FileType;
import com.example.konnect_backend.domain.ai.type.PromptStatus;
import com.example.konnect_backend.domain.ai.type.TargetLanguage;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
@Hidden
@Tag(name = "프롬프트 관리")
public class PromptManagementController {

    private final PromptManagementService promptManagementService;
    private final TextExtractorFacade textExtractorFacade;

    @Operation(summary = "프롬프트 목록 조회")
    @GetMapping("/prompts")
    public ResponseEntity<PromptSummaryListResponse> getPrompts(
        @RequestParam(value = "status", required = false) PromptStatus status,
        @RequestParam(value = "moduleName", required = false) String moduleName) {
        return ResponseEntity.ok(promptManagementService.getPrompts(status, moduleName));
    }

    @Operation(summary = "개별 프롬프트 조회")
    @GetMapping("/prompts/{promptId}")
    public ResponseEntity<PromptResponse> getPrompt(@PathVariable Long promptId) {
        return ResponseEntity.ok(promptManagementService.getPrompt(promptId));
    }

    @Operation(summary = "사용 가능한 모델 조회")
    @GetMapping("/models")
    public ResponseEntity<ModelListResponse> getModels() {
        return ResponseEntity.ok(promptManagementService.getModels());
    }

    @Operation(summary = "프롬프트 활성화", description = "해당 프롬프트를 현재 사용하는 프롬프트로 변경합니다.")
    @PostMapping("/prompts/{promptId}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long promptId) {
        promptManagementService.activate(promptId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "프롬프트 실행", description = "사용자가 제공한 프롬프트, 입력, max tokens, 모델명을 기준으로 모델을 호출합니다.")
    @PostMapping("/prompts/run")
    public ResponseEntity<RunResultResponse> run(@Valid @RequestBody RunPromptRequest request) {
        return ResponseEntity.ok(promptManagementService.run(request));
    }

    @Operation(summary = "새 버전의 프롬프트 생성", description = "사용자 설정값으로 새 프롬프트를 생성합니다.")
    @PostMapping("/prompts")
    public ResponseEntity<PromptResponse> createNewVersion(
        @Valid @RequestBody CreatePromptRequest request) {
        return ResponseEntity.ok(
            promptManagementService.createNewVersion(request.moduleName(), request.template(), request.model(),
                request.maxTokens()));
    }

    @Operation(summary = "편의용 텍스트 추출 API")
    @PostMapping(value = "/extract-text", consumes = "multipart/form-data")
    public ResponseEntity<String> extractText (
        @RequestParam("file") MultipartFile multipartFile,
        @RequestParam("fileType") FileType fileType) {
        validateFileInput(multipartFile, fileType);

        UploadFile file;
        try {
            file = new UploadFile(multipartFile.getOriginalFilename(), fileType,
                multipartFile.getContentType(), multipartFile.getSize(),
                multipartFile.getInputStream().readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 원래는 서비스에 있어야 하나 바뀌지 않고 재사용할 일 없는 로직이므로 편의 상 컨트롤러에 작성
        PipelineContext context = PipelineContext.builder().targetLanguage(TargetLanguage.KOREAN)
            .requestId(UUID.randomUUID()) // id는 상관없으나 혹시 모를 null 문제 방지
            .filename(file.originalName())
            .fileType(file.fileType())
            .completedStage(PipelineContext.PipelineStage.NONE)
            .processingLogs(new ArrayList<>()).build();

        TextExtractionResult result = textExtractorFacade.extract(file, context);

        return ResponseEntity.ok(result.getText());
    }

    private void validateFileInput(MultipartFile file, FileType fileType) {
        if (file == null || file.isEmpty()) {
            throw new GeneralException(ErrorStatus.FILE_EMPTY);
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new GeneralException(ErrorStatus.FILE_NAME_MISSING);
        }

        if (fileType == null) {
            throw new GeneralException(ErrorStatus.UNSUPPORTED_FILE_TYPE);
        }

        String contentType = file.getContentType();
        if (fileType == FileType.PDF && !"application/pdf".equals(contentType)) {
            throw new GeneralException(ErrorStatus.INVALID_PDF_FILE);
        }

        if (fileType == FileType.IMAGE && (contentType == null || !contentType.startsWith(
            "image/"))) {
            throw new GeneralException(ErrorStatus.INVALID_IMAGE_FILE);
        }
    }
}
