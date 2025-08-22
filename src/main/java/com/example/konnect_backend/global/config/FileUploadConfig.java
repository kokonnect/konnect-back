package com.example.konnect_backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

@Configuration
public class FileUploadConfig {
    
    @Value("${spring.servlet.multipart.max-file-size:20MB}")
    private String maxFileSize;
    
    @Value("${spring.servlet.multipart.max-request-size:25MB}")
    private String maxRequestSize;
    
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}
