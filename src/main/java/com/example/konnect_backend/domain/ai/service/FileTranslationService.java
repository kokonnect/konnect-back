package com.example.konnect_backend.domain.ai.service;

import com.example.konnect_backend.domain.ai.dto.FileType;
import com.example.konnect_backend.domain.ai.dto.TargetLanguage;
import com.example.konnect_backend.domain.ai.dto.request.FileTranslationRequest;
import com.example.konnect_backend.domain.ai.dto.response.FileTranslationResponse;
import com.example.konnect_backend.global.exception.GeneralException;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileTranslationService {
    
    private final ChatModel chatModel;
    
    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;
    
    private static final String IMAGE_OCR_PROMPT = """
            이 이미지에 포함된 모든 텍스트를 정확하게 추출해주세요.
            
            요구사항:
            - 이미지의 모든 텍스트를 빠짐없이 읽어주세요
            - 텍스트의 순서와 레이아웃을 최대한 보존해주세요
            - 한글, 영어, 숫자, 특수문자 등 모든 문자를 정확히 인식해주세요
            - 표, 목록, 제목 등의 구조가 있다면 유지해주세요
            - 텍스트만 반환하고 다른 설명은 추가하지 마세요
            
            추출된 텍스트:
            """;
    
    private static final String TRANSLATION_PROMPT_TEMPLATE = """
            다음 텍스트를 {targetLanguage}로 번역해주세요.
            {useSimpleLanguage}
            
            원본 텍스트:
            {text}
            
            번역 지침:
            - 자연스럽고 정확한 번역을 해주세요
            - 전문 용어는 해당 분야의 일반적인 용어로 번역해주세요
            - 문맥과 의미를 충분히 고려해주세요
            - 원본 텍스트의 구조와 형식을 최대한 유지해주세요
            - 번역문만 출력하고 다른 설명은 하지 마세요
            
            번역 결과:
            """;
    
    public FileTranslationResponse translateFile(FileTranslationRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("파일 번역 시작: {}, 타입: {}, 목표언어: {}", 
                    request.getFile().getOriginalFilename(), 
                    request.getFileType(), 
                    request.getTargetLanguage());
            
            // 파일 검증
            validateFile(request.getFile(), request.getFileType());
            
            // 이미지 파일만 지원
            if (request.getFileType() != FileType.IMAGE) {
                throw new GeneralException(ErrorStatus.UNSUPPORTED_FILE_TYPE);
            }
            
            // 1단계: 이미지에서 텍스트 추출 (OCR)
            String extractedText = extractTextFromImage(request.getFile());
            
            if (extractedText == null || extractedText.trim().isEmpty()) {
                log.error("텍스트 추출 결과가 비어있음");
                throw new GeneralException(ErrorStatus.TEXT_EXTRACTION_FAILED);
            }
            
            log.info("OCR 추출 완료: {} 글자", extractedText.length());
            
            // 2단계: 추출된 텍스트 번역
            String translatedText = translateText(extractedText, request.getTargetLanguage(), request.getUseSimpleLanguage());
            
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;
            
            log.info("번역 완료: 총 처리시간 {}ms", processingTime);
            
            return FileTranslationResponse.builder()
                    .extractedText(extractedText)
                    .translatedText(translatedText)
                    .originalFileName(request.getFile().getOriginalFilename())
                    .fileType(request.getFileType())
                    .targetLanguage(request.getTargetLanguage())
                    .targetLanguageName(request.getTargetLanguage().getDisplayName())
                    .usedSimpleLanguage(request.getUseSimpleLanguage())
                    .fileSize(request.getFile().getSize())
                    .originalTextLength(extractedText.length())
                    .translatedTextLength(translatedText.length())
                    .totalProcessingTimeMs(processingTime)
                    .pageCount(null)
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
        
        // OpenAI Vision API 파일 크기 제한 (20MB)
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new GeneralException(ErrorStatus.FILE_SIZE_EXCEEDED);
        }
        
        // 이미지 파일만 지원
        if (fileType != FileType.IMAGE) {
            throw new GeneralException(ErrorStatus.UNSUPPORTED_FILE_TYPE);
        }
        
        // MIME 타입 검증
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new GeneralException(ErrorStatus.INVALID_IMAGE_FILE);
        }
        
        // OpenAI Vision API에서 지원하는 이미지 형식 확인
        if (!isSupportedImageFormat(contentType)) {
            throw new GeneralException(ErrorStatus.INVALID_IMAGE_FORMAT);
        }
    }
    
    private boolean isSupportedImageFormat(String contentType) {
        return contentType.equals("image/jpeg") ||
               contentType.equals("image/jpg") ||
               contentType.equals("image/png") ||
               contentType.equals("image/gif") ||
               contentType.equals("image/webp");
    }
    
    private String extractTextFromImage(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            
            log.info("이미지 OCR 처리 시작: {}", file.getContentType());
            
            // Media 객체 생성 - Spring AI M2 방식
            Media media = new Media(MimeTypeUtils.parseMimeType(file.getContentType()),
                                   new ByteArrayResource(fileBytes));
            
            // UserMessage 생성
            UserMessage userMessage = new UserMessage(IMAGE_OCR_PROMPT, List.of(media));
            
            // Vision 모델 옵션 설정
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withModel("gpt-4o")
                    .withMaxTokens(4000)
                    .withTemperature(0.1f)
                    .build();
            
            // Prompt 생성
            Prompt prompt = new Prompt(List.of(userMessage), options);
            
            // API 호출
            String result = chatModel.call(prompt).getResult().getOutput().getContent();
            
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
    
    private String translateText(String text, TargetLanguage targetLanguage, Boolean useSimpleLanguage) {
        try {
            String simpleLanguageNote = Boolean.TRUE.equals(useSimpleLanguage) 
                ? "가능한 한 간단하고 이해하기 쉬운 언어로 번역해주세요." 
                : "";
            
            PromptTemplate promptTemplate = new PromptTemplate(TRANSLATION_PROMPT_TEMPLATE);
            Prompt prompt = promptTemplate.create(Map.of(
                    "text", text,
                    "targetLanguage", targetLanguage.getDisplayName(),
                    "useSimpleLanguage", simpleLanguageNote
            ));
            
            String result = chatModel.call(prompt).getResult().getOutput().getContent();
            
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
}