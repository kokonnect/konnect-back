package com.example.konnect_backend.domain.ai.dto;

public enum FileType {
    PDF("PDF 파일"),
    IMAGE("이미지 파일");
    
    private final String description;
    
    FileType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
