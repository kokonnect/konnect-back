package com.example.konnect_backend.domain.ai.aop;

import java.util.Map;

public record PromptContext(String moduleName, Integer promptVersion, Map<String, String> vars) {
}
