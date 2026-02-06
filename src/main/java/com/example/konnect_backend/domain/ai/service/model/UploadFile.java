package com.example.konnect_backend.domain.ai.service.model;

import java.io.InputStream;

public record UploadFile(
    String originalName,
    String contentType,
    long size,
    InputStream inputStream
) {}
