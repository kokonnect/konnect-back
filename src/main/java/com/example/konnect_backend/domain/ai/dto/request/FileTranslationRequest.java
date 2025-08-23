package com.example.konnect_backend.domain.ai.dto.request;

import com.example.konnect_backend.domain.ai.dto.FileType;
import com.example.konnect_backend.domain.ai.dto.TargetLanguage;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileTranslationRequest {
    
    @NotNull(message = "파일은 필수입니다.")
    private MultipartFile file;
    
    @NotNull(message = "파일 타입은 필수입니다.")
    private FileType fileType;
    
    @NotNull(message = "번역할 언어는 필수입니다.")
    private TargetLanguage targetLanguage;
    
    // 간단한 언어 사용 여부 (옵션)
    private Boolean useSimpleLanguage = true;
    
    // 원본 언어 힌트 (옵션)
    private String sourceLanguageHint;
}
