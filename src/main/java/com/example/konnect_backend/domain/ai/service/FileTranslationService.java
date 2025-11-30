package com.example.konnect_backend.domain.ai.service;

import com.example.konnect_backend.domain.ai.dto.FileType;
import com.example.konnect_backend.domain.ai.dto.TargetLanguage;
import com.example.konnect_backend.domain.ai.dto.request.FileTranslationRequest;
import com.example.konnect_backend.domain.ai.dto.response.FileTranslationResponse;
import com.example.konnect_backend.domain.ai.dto.response.TranslationHistoryResponse;
import com.example.konnect_backend.domain.document.entity.DocumentFile;
import com.example.konnect_backend.domain.document.entity.DocumentTranslation;
import com.example.konnect_backend.domain.document.repository.DocumentRepository;
import com.example.konnect_backend.domain.document.repository.DocumentFileRepository;
import com.example.konnect_backend.domain.document.repository.DocumentTranslationRepository;
import com.example.konnect_backend.domain.user.entity.User;
import com.example.konnect_backend.domain.user.repository.UserRepository;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 파일 번역 서비스 (Gemini API 사용)
 *
 * ## 모델 선택 전략
 * - 이미지 OCR: gemini-2.0-flash (Vision 기능 필요, Primary 모델)
 * - 번역: gemini-2.0-flash-lite (단순 번역, Lite 모델)
 * - 요약: gemini-2.0-flash-lite (단순 요약, Lite 모델)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileTranslationService {

    private record PdfExtractionResult(String extractedText, Integer pageCount) {}

    private final GeminiService geminiService;
    private final DocumentRepository documentRepository;
    private final DocumentFileRepository documentFileRepository;
    private final DocumentTranslationRepository documentTranslationRepository;
    private final UserRepository userRepository;


    private static final String IMAGE_OCR_PROMPT = """
            Please extract all text from this image accurately and completely.

            Requirements:
            - Extract ALL visible text without missing anything
            - Preserve the order and layout of text as much as possible
            - Accurately recognize all characters including letters, numbers, and special characters in any language
            - Maintain structure such as tables, lists, and headings if present
            - Return ONLY the extracted text without any additional explanations, comments, or refusals
            - Do not say "I can't" or "I'm sorry" - simply extract whatever text is visible
            - If the image contains text in Korean, English, or any other language, extract it faithfully

            Extracted text:
            """;

    private static final String TRANSLATION_PROMPT_TEMPLATE = """
            다음 텍스트를 %s로 번역해주세요.
            %s

            원본 텍스트:
            %s

            번역 지침:
            - 자연스럽고 정확한 번역을 해주세요
            - 전문 용어는 해당 분야의 일반적인 용어로 번역해주세요
            - 문맥과 의미를 충분히 고려해주세요
            - OCR로 추출된 텍스트의 경우 잘못된 줄바꿈, 불필요한 공백, 단어 분리 등을 자연스럽게 연결해주세요
            - 문단과 문장을 논리적으로 재구성하여 읽기 쉽게 만들어주세요
            - 표, 목록, 제목 등의 구조적 요소는 의미에 따라 적절히 정리해주세요
            - 번역문만 출력하고 다른 설명은 하지 마세요

            번역 결과:
            """;

    private static final String SUMMARY_PROMPT_TEMPLATE = """
            다음 번역된 텍스트를 %s로 간단히 요약해주세요.

            번역된 텍스트:
            %s

            요약 지침:
            - 핵심 내용만을 3-5줄로 요약해주세요
            - 중요한 정보나 결론을 우선적으로 포함해주세요
            - 간결하고 이해하기 쉽게 작성해주세요
            - %s로 요약문만 출력하고 다른 설명은 하지 마세요

            요약:
            """;

    @Transactional
    public FileTranslationResponse translateFile(FileTranslationRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("파일 번역 시작: {}, 타입: {}, 목표언어: {}",
                    request.getFile().getOriginalFilename(),
                    request.getFileType(),
                    request.getTargetLanguage());

            // 파일 검증
            validateFile(request.getFile(), request.getFileType());

            // 파일 타입에 따른 처리
            String extractedText;
            Integer pageCount = null;

            if (request.getFileType() == FileType.IMAGE) {
                // 1단계: 이미지에서 텍스트 추출 (Gemini Vision OCR)
                extractedText = extractTextFromImage(request.getFile());
            } else if (request.getFileType() == FileType.PDF) {
                // 1단계: PDF에서 텍스트 추출
                PdfExtractionResult result = extractTextFromPdfWithPageCount(request.getFile());
                extractedText = result.extractedText();
                pageCount = result.pageCount();
            } else {
                throw new GeneralException(ErrorStatus.UNSUPPORTED_FILE_TYPE);
            }

            if (extractedText.trim().isEmpty()) {
                log.error("텍스트 추출 결과가 비어있음");
                throw new GeneralException(ErrorStatus.TEXT_EXTRACTION_FAILED);
            }

            log.info("텍스트 추출 완료: {} 글자", extractedText.length());

            // 2단계: 추출된 텍스트 번역 (Gemini Lite 모델)
            String translatedText = translateText(extractedText, request.getTargetLanguage(), request.getUseSimpleLanguage());

            // 3단계: 번역된 텍스트 요약 (Gemini Lite 모델)
            String summary = generateSummary(translatedText, request.getTargetLanguage());

            // 4단계: DB에 저장
            // 현재 로그인한 사용자 정보 가져오기 (게스트 포함)
            Long userId = SecurityUtil.getCurrentUserIdOrNull();
            User user = null;

            if (userId != null) {
                user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    log.info("사용자 정보 확인: userId={}, isGuest={}", userId, user.isGuest());
                }
            }

            // Document 엔티티 생성 및 저장
            com.example.konnect_backend.domain.document.entity.Document document = null;
            if (user != null) {
                document = com.example.konnect_backend.domain.document.entity.Document.builder()
                        .user(user)
                        .title(request.getFile().getOriginalFilename())
                        .description("파일 번역: " + request.getTargetLanguage().getDisplayName())
                        .build();

                // DocumentFile 엔티티 생성
                DocumentFile documentFile = DocumentFile.builder()
                        .fileName(request.getFile().getOriginalFilename())
                        .fileType(request.getFileType().name())
                        .fileSize(request.getFile().getSize())
                        .extractedText(extractedText)
                        .pageCount(pageCount != null ? pageCount : 1)
                        .build();

                // DocumentTranslation 엔티티 생성
                DocumentTranslation documentTranslation = DocumentTranslation.builder()
                        .translatedLanguage(request.getTargetLanguage().getLanguageCode())
                        .translatedText(translatedText)
                        .summary(summary)
                        .build();

                // 관계 설정
                document.addDocumentFile(documentFile);
                document.addTranslation(documentTranslation);

                // 저장
                document = documentRepository.save(document);
                log.info("문서 저장 완료: documentId={}", document.getId());
            }

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            log.info("번역 및 요약 완료: 총 처리시간 {}ms", processingTime);

            return FileTranslationResponse.builder()
                    .extractedText(extractedText)
                    .translatedText(translatedText)
                    .summary(summary)
                    .originalFileName(request.getFile().getOriginalFilename())
                    .fileType(request.getFileType())
                    .targetLanguage(request.getTargetLanguage())
                    .targetLanguageName(request.getTargetLanguage().getDisplayName())
                    .usedSimpleLanguage(request.getUseSimpleLanguage())
                    .fileSize(request.getFile().getSize())
                    .originalTextLength(extractedText.length())
                    .translatedTextLength(translatedText.length())
                    .totalProcessingTimeMs(processingTime)
                    .pageCount(pageCount)
                    .sourceLanguageHint(request.getSourceLanguageHint())
                    .build();

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("파일 번역 중 예상치 못한 오류 발생", e);
            throw new GeneralException(ErrorStatus.FILE_TRANSLATION_ERROR);
        }
    }

    private void validateFile(MultipartFile file, FileType fileType) {
        if (file.isEmpty()) {
            throw new GeneralException(ErrorStatus.FILE_EMPTY);
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new GeneralException(ErrorStatus.FILE_NAME_MISSING);
        }

        // 파일 크기 제한 (20MB)
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new GeneralException(ErrorStatus.FILE_SIZE_EXCEEDED);
        }

        // 지원되는 파일 타입 검증
        if (fileType != FileType.IMAGE && fileType != FileType.PDF) {
            throw new GeneralException(ErrorStatus.UNSUPPORTED_FILE_TYPE);
        }

        // MIME 타입 검증
        String contentType = file.getContentType();
        if (fileType == FileType.PDF) {
            if (!"application/pdf".equals(contentType)) {
                throw new GeneralException(ErrorStatus.INVALID_PDF_FILE);
            }
        } else if (fileType == FileType.IMAGE) {
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new GeneralException(ErrorStatus.INVALID_IMAGE_FILE);
            }
            // Gemini Vision에서 지원하는 이미지 형식 확인
            if (!isSupportedImageFormat(contentType)) {
                throw new GeneralException(ErrorStatus.INVALID_IMAGE_FORMAT);
            }
        }
    }

    private boolean isSupportedImageFormat(String contentType) {
        return contentType.equals("image/jpeg") ||
               contentType.equals("image/jpg") ||
               contentType.equals("image/png") ||
               contentType.equals("image/gif") ||
               contentType.equals("image/webp") ||
               contentType.equals("image/heic") ||
               contentType.equals("image/heif");
    }

    /**
     * 이미지에서 텍스트 추출 (Gemini Vision 사용)
     * Primary 모델 사용 (Vision 기능 필요)
     */
    private String extractTextFromImage(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();

            log.info("Gemini Vision OCR 처리 시작: {}", file.getContentType());

            // 이미지를 Base64로 인코딩
            String base64Image = Base64.getEncoder().encodeToString(fileBytes);

            // Gemini Vision API 호출
            String result = geminiService.generateContentWithImage(
                    IMAGE_OCR_PROMPT,
                    base64Image,
                    file.getContentType(),
                    0.1,  // 낮은 온도로 정확한 텍스트 추출
                    8000  // 충분한 토큰
            );

            if (result == null || result.trim().isEmpty()) {
                log.error("이미지 OCR 결과가 비어있음");
                throw new GeneralException(ErrorStatus.TEXT_EXTRACTION_FAILED);
            }

            log.info("OCR 추출 성공: {} 글자", result.trim().length());
            return result.trim();

        } catch (Exception e) {
            log.error("이미지 OCR 중 오류 발생", e);
            throw new GeneralException(ErrorStatus.TEXT_EXTRACTION_FAILED);
        }
    }

    /**
     * 텍스트 번역 (Gemini Lite 모델 사용)
     * 단순 번역이므로 Lite 모델로 처리
     */
    private String translateText(String text, TargetLanguage targetLanguage, Boolean useSimpleLanguage) {
        try {
            String simpleLanguageNote = Boolean.TRUE.equals(useSimpleLanguage)
                ? "가능한 한 간단하고 이해하기 쉬운 언어로 번역해주세요."
                : "";

            String prompt = String.format(TRANSLATION_PROMPT_TEMPLATE,
                    targetLanguage.getDisplayName(),
                    simpleLanguageNote,
                    text);

            // Gemini Lite 모델 사용
            String result = geminiService.generateSimpleContent(prompt, 0.3, 4000);

            if (result == null || result.trim().isEmpty()) {
                log.error("번역 결과가 비어있음");
                throw new GeneralException(ErrorStatus.TRANSLATION_FAILED);
            }

            return result.trim();

        } catch (Exception e) {
            log.error("번역 중 오류 발생", e);
            throw new GeneralException(ErrorStatus.TRANSLATION_FAILED);
        }
    }

    private PdfExtractionResult extractTextFromPdfWithPageCount(MultipartFile file) {
        try {
            log.info("PDF 텍스트 추출 시작: {}", file.getOriginalFilename());

            // Spring AI PagePdfDocumentReader 사용
            ByteArrayResource resource = new ByteArrayResource(file.getBytes());

            // PDF 읽기 설정
            PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                    .withPageTopMargin(0)
                    .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                            .withNumberOfTopTextLinesToDelete(0)
                            .build())
                    .withPagesPerDocument(1) // 페이지당 1개 문서
                    .build();

            // PagePdfDocumentReader로 PDF 텍스트 추출
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource, config);
            List<Document> documents = pdfReader.read();

            if (documents.isEmpty()) {
                log.error("PDF에서 추출된 문서가 없음");
                throw new GeneralException(ErrorStatus.TEXT_EXTRACTION_FAILED);
            }

            // 모든 페이지의 텍스트를 하나로 합치기
            String extractedText = documents.stream()
                    .map(Document::getContent)
                    .collect(Collectors.joining("\n\n"));

            if (extractedText == null || extractedText.trim().isEmpty()) {
                log.error("PDF 텍스트 추출 결과가 비어있음");
                throw new GeneralException(ErrorStatus.TEXT_EXTRACTION_FAILED);
            }

            log.info("PDF 텍스트 추출 성공: {} 페이지, {} 글자", documents.size(), extractedText.length());
            return new PdfExtractionResult(extractedText.trim(), documents.size());

        } catch (GeneralException e) {
            throw e;
        } catch (IOException e) {
            log.error("PDF 파일 읽기 중 오류 발생", e);
            throw new GeneralException(ErrorStatus.TEXT_EXTRACTION_FAILED);
        } catch (Exception e) {
            log.error("PDF 텍스트 추출 중 예상치 못한 오류 발생", e);
            throw new GeneralException(ErrorStatus.TEXT_EXTRACTION_FAILED);
        }
    }

    /**
     * 요약 생성 (Gemini Lite 모델 사용)
     * 단순 요약이므로 Lite 모델로 처리
     */
    private String generateSummary(String translatedText, TargetLanguage targetLanguage) {
        try {
            log.info("요약 생성 시작: {} 글자, 목표언어: {}", translatedText.length(), targetLanguage.getDisplayName());

            String prompt = String.format(SUMMARY_PROMPT_TEMPLATE,
                    targetLanguage.getDisplayName(),
                    translatedText,
                    targetLanguage.getDisplayName());

            // Gemini Lite 모델 사용
            String result = geminiService.generateSimpleContent(prompt, 0.3, 500);

            if (result == null || result.trim().isEmpty()) {
                log.error("요약 결과가 비어있음");
                throw new GeneralException(ErrorStatus.TRANSLATION_FAILED);
            }

            log.info("요약 생성 성공: {} 글자", result.trim().length());
            return result.trim();

        } catch (Exception e) {
            log.error("요약 생성 중 오류 발생", e);
            throw new GeneralException(ErrorStatus.TRANSLATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public TranslationHistoryResponse getTranslationHistory() {
        try {
            // 현재 로그인한 사용자 정보 가져오기 (게스트 포함)
            Long userId = SecurityUtil.getCurrentUserIdOrNull();
            if (userId == null) {
                log.info("인증되지 않은 사용자의 번역 내역 조회 요청");
                return TranslationHistoryResponse.builder()
                        .histories(List.of())
                        .totalCount(0)
                        .build();
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("존재하지 않는 사용자: userId={}", userId);
                return TranslationHistoryResponse.builder()
                        .histories(List.of())
                        .totalCount(0)
                        .build();
            }

            log.info("번역 내역 조회 시작: userId={}, isGuest={}", userId, user.isGuest());

            // 최근 10개의 번역 내역 조회 (2단계로 조회)
            Pageable pageable = PageRequest.of(0, 10);

            // 1단계: Document와 DocumentFile을 함께 조회
            List<com.example.konnect_backend.domain.document.entity.Document> documents =
                    documentRepository.findByUserWithFilesOrderByCreatedAtDesc(user, pageable);

            // 2단계: 조회된 Document들의 Translation을 조회
            if (!documents.isEmpty()) {
                documents = documentRepository.findWithTranslationsByDocuments(documents);
            }

            // DTO로 변환
            List<TranslationHistoryResponse.TranslationHistoryItem> histories = documents.stream()
                    .map(this::convertToHistoryItem)
                    .collect(Collectors.toList());

            log.info("번역 내역 조회 완료: 총 {}개", histories.size());

            return TranslationHistoryResponse.builder()
                    .histories(histories)
                    .totalCount(histories.size())
                    .build();

        } catch (Exception e) {
            log.error("번역 내역 조회 중 오류 발생", e);
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }

    private TranslationHistoryResponse.TranslationHistoryItem convertToHistoryItem(
            com.example.konnect_backend.domain.document.entity.Document document) {

        // 첫 번째 파일과 번역 정보 가져오기 (현재는 1:1 관계)
        DocumentFile documentFile = document.getDocumentFiles().stream().findFirst().orElse(null);
        DocumentTranslation translation = document.getTranslations().stream().findFirst().orElse(null);

        return TranslationHistoryResponse.TranslationHistoryItem.builder()
                .documentId(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .createdAt(document.getCreatedAt())
                // 파일 정보
                .fileName(documentFile != null ? documentFile.getFileName() : null)
                .fileType(documentFile != null ? FileType.valueOf(documentFile.getFileType()) : null)
                .fileSize(documentFile != null ? documentFile.getFileSize() : null)
                .pageCount(documentFile != null ? documentFile.getPageCount() : null)
                .extractedText(documentFile != null ? documentFile.getExtractedText() : null)
                // 번역 정보
                .translatedLanguage(translation != null ? translation.getTranslatedLanguage() : null)
                .translatedText(translation != null ? translation.getTranslatedText() : null)
                .summary(translation != null ? translation.getSummary() : null)
                .build();
    }

}
