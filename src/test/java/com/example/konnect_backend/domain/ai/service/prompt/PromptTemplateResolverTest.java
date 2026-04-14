package com.example.konnect_backend.domain.ai.service.prompt;

import com.example.konnect_backend.domain.ai.domain.entity.PromptTemplate;
import com.example.konnect_backend.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PromptTemplateResolverTest {

    PromptTemplateResolver resolver = new PromptTemplateResolver();

    private static final Pattern SLOT_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    @DisplayName("정상 완성")
    @Test
    void resolve() {
        PromptTemplate template = new PromptTemplate("TEST_MODULE", 1, "다음 변수를 치환해주세요. {{text}}", 100, 1L);
        Map<String, String> vars = Map.of("text", "치환된 텍스트");

        String resolved = resolver.resolve(template, vars);

        assertThat(isResolved(resolved)).isTrue();
    }

    @DisplayName("변수 누락 시 예외")
    @Test
    void resolve_fail() {
        PromptTemplate template = new PromptTemplate("TEST_MODULE", 1, "다음 변수를 치환해주세요. {{text}}", 100, 1L);
        Map<String, String> vars = Map.of("wrong_key", "잘못된 키");

        assertThatThrownBy(() -> resolver.resolve(template, vars))
            .isInstanceOf(GeneralException.class);
    }

    private boolean isResolved(String template) {
        if (template == null || template.isEmpty()) return true;

        return !SLOT_PATTERN.matcher(template).find();
    }
}