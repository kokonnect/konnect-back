package com.example.konnect_backend.domain.ai.model.vo;

import com.example.konnect_backend.domain.ai.type.FileType;

public record UploadFile(
    String originalName,
    FileType fileType,
    String mimeType,
    long size,
    byte[] bytes
) {}
