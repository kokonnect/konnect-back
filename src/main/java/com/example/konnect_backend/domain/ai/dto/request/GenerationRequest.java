package com.example.konnect_backend.domain.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenerationRequest {
    
    @NotBlank(message = "프롬프트는 필수입니다.")
    private String prompt;
    
    // 최대 토큰 수
    private Integer maxTokens = 1000;
    
    // 창의성 수준 (0.0 ~ 1.0)
    private Double temperature = 0.7;
    
    // 생성할 콘텐츠 타입 힌트
    private String contentType;
}
