package com.example.konnect_backend.domain.ai.service.prompt.management;

import com.example.konnect_backend.domain.ai.domain.entity.AiModel;
import com.example.konnect_backend.domain.ai.domain.entity.PromptSlot;
import com.example.konnect_backend.domain.ai.domain.entity.PromptTemplate;
import com.example.konnect_backend.domain.ai.dto.response.PromptResponse;
import com.example.konnect_backend.domain.ai.infra.GeminiService;
import com.example.konnect_backend.domain.ai.repository.AiModelRepository;
import com.example.konnect_backend.domain.ai.repository.PromptTemplateRepository;
import com.example.konnect_backend.domain.ai.service.prompt.PromptTemplateResolver;
import com.example.konnect_backend.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptManagementServiceTest {

    @Mock
    GeminiService geminiService;
    @Mock
    PromptLoader promptLoader;
    @Mock
    PromptTemplateResolver resolver;
    @Mock
    PromptTemplateRepository promptRepository;
    @Mock
    AiModelRepository aiModelRepository;

    @InjectMocks
    PromptManagementService service;

    private static final String MODULE_NAME = "test-module";
    private static final String MODEL_NAME = "gemini-pro";
    private static final String TEMPLATE = "Hello {name}, your code is {code}";

    @DisplayName("변수 목록이 일치하면 정상적으로 추가된다.")
    @Test
    void Should_Succeed_When_NewTemplateIsValid() {
        // given
        AiModel aiModel = mock(AiModel.class);
        given(aiModel.getId()).willReturn(1L);

        PromptTemplate maxVersionPrompt = mock(PromptTemplate.class);
        given(maxVersionPrompt.getVersion()).willReturn(2);

        PromptTemplate activePrompt = mock(PromptTemplate.class);
        PromptSlot slot1 = new PromptSlot("name", 1, activePrompt);
        PromptSlot slot2 = new PromptSlot("code", 2, activePrompt);
        given(activePrompt.getSlots()).willReturn(List.of(slot1, slot2));

        PromptTemplate saved = new PromptTemplate(MODULE_NAME, 3, TEMPLATE, 1000, 1L);
        ReflectionTestUtils.setField(saved, "id", 10L);

        given(aiModelRepository.findByName(MODEL_NAME)).willReturn(Optional.of(aiModel));
        given(promptRepository.getMaxVersionOfModule(MODULE_NAME)).willReturn(maxVersionPrompt);
        given(promptLoader.getActivePromptTemplate(MODULE_NAME)).willReturn(activePrompt);
        given(resolver.getSlotKeys(TEMPLATE)).willReturn(Set.of("name", "code"));
        given(promptRepository.save(any())).willReturn(saved);

        // when
        PromptResponse result = service.createNewVersion(MODULE_NAME, TEMPLATE, MODEL_NAME, 1000);

        // then
        assertThat(result).isNotNull();
        verify(promptRepository).save(any(PromptTemplate.class));
    }

    @DisplayName("변수 목록이 일치하지 않으면 예외가 발생한다.")
    @Test
    void Should_ThrowException_When_NewTemplateIsInvalid() {
        // given
        AiModel aiModel = mock(AiModel.class);
        given(aiModel.getId()).willReturn(1L);

        PromptTemplate maxVersionPrompt = mock(PromptTemplate.class);
        given(maxVersionPrompt.getVersion()).willReturn(2);

        PromptTemplate activePrompt = mock(PromptTemplate.class);
        PromptSlot slot1 = new PromptSlot("name", 1, activePrompt);
        PromptSlot slot2 = new PromptSlot("code", 2, activePrompt);
        given(activePrompt.getSlots()).willReturn(List.of(slot1, slot2));

        given(aiModelRepository.findByName(MODEL_NAME)).willReturn(Optional.of(aiModel));
        given(promptRepository.getMaxVersionOfModule(MODULE_NAME)).willReturn(maxVersionPrompt);
        given(promptLoader.getActivePromptTemplate(MODULE_NAME)).willReturn(activePrompt);
        given(resolver.getSlotKeys(TEMPLATE)).willReturn(Set.of("name")); // code 빠짐

        // when & then
        assertThatThrownBy(() ->
            service.createNewVersion(MODULE_NAME, TEMPLATE, MODEL_NAME, 1000))
            .isInstanceOf(GeneralException.class);

        verify(promptRepository, never()).save(any());
    }
}