package com.example.konnect_backend.domain.ai.dto.request;

import com.example.konnect_backend.domain.ai.dto.FileType;
import com.example.konnect_backend.domain.ai.dto.TargetLanguage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAnalysisRequest {

    private MultipartFile file;

    private FileType fileType;

    @Builder.Default
    private TargetLanguage targetLanguage = TargetLanguage.KOREAN;

    @Builder.Default
    private Boolean autoCreateSchedule = false;

    private Long childId;

    @Builder.Default
    private Boolean useSimpleLanguage = false;
}
