package com.example.konnect_backend.domain.ai.validator;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Component
public class FileUploadValidator {

    // 지원하는 이미지 파일 확장자
    private static final List<String> SUPPORTED_IMAGE_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "webp"
    );

    // 지원하는 문서 파일 확장자
    private static final List<String> SUPPORTED_DOCUMENT_EXTENSIONS = Arrays.asList(
        "pdf"
    );

    // 최대 파일 크기 (20MB)
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;
    
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기가 너무 큽니다. 최대 20MB까지 업로드 가능합니다.");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("유효하지 않은 파일명입니다.");
        }
        
        String extension = getFileExtension(fileName).toLowerCase();
        
        if (!isValidFileType(extension)) {
            throw new IllegalArgumentException(
                "지원하지 않는 파일 형식입니다. " +
                "지원 형식: " + String.join(", ", getAllSupportedExtensions())
            );
        }
        
        // 추가 MIME 타입 검증
        validateMimeType(file, extension);
    }
    
    private void validateMimeType(MultipartFile file, String extension) {
        String contentType = file.getContentType();
        
        if (SUPPORTED_IMAGE_EXTENSIONS.contains(extension)) {
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("이미지 파일의 MIME 타입이 올바르지 않습니다.");
            }
        } else if (SUPPORTED_DOCUMENT_EXTENSIONS.contains(extension)) {
            if (!"application/pdf".equals(contentType)) {
                throw new IllegalArgumentException("PDF 파일의 MIME 타입이 올바르지 않습니다.");
            }
        }
    }
    
    public boolean isImageFile(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        return SUPPORTED_IMAGE_EXTENSIONS.contains(extension);
    }
    
    public boolean isPdfFile(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        return SUPPORTED_DOCUMENT_EXTENSIONS.contains(extension);
    }
    
    public FileType getFileType(String fileName) {
        if (isImageFile(fileName)) {
            return FileType.IMAGE;
        } else if (isPdfFile(fileName)) {
            return FileType.PDF;
        } else {
            return FileType.UNSUPPORTED;
        }
    }
    
    private boolean isValidFileType(String extension) {
        return SUPPORTED_IMAGE_EXTENSIONS.contains(extension) || 
               SUPPORTED_DOCUMENT_EXTENSIONS.contains(extension);
    }
    
    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
    
    private List<String> getAllSupportedExtensions() {
        List<String> allExtensions = Arrays.asList(
            SUPPORTED_IMAGE_EXTENSIONS.toArray(new String[0])
        );
        allExtensions.addAll(SUPPORTED_DOCUMENT_EXTENSIONS);
        return allExtensions;
    }
    
    public enum FileType {
        IMAGE, PDF, UNSUPPORTED
    }
}
