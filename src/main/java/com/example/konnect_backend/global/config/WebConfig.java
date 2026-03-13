package com.example.konnect_backend.global.config;

import com.example.konnect_backend.domain.ai.interceptor.AnalysisInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AnalysisInterceptor())
            .addPathPatterns("/api/ai/analyze");
    }
}