package com.example.konnect_backend.domain.ai.service.prompt;

import com.example.konnect_backend.domain.ai.entity.PromptTemplate;
import com.example.konnect_backend.global.code.status.ErrorStatus;
import com.example.konnect_backend.global.exception.GeneralException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PromptTemplateResolver {

    private static final Pattern SLOT_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    /**
     * 프롬프트 템플릿의 변수를 실제 입력값으로 치환하여 반환니다. <br />
     *
     * @param promptTemplate 프롬프트 템플릿 엔티티
     * @param vars           변수 키-값 쌍 (e.g. ("target_language", "한국어"))
     * @return 완성된 프롬프트
     * @throws com.example.konnect_backend.global.exception.GeneralException 치환 뒤에도 변수가 남아있는 경우
     */
    public String resolve(PromptTemplate promptTemplate, Map<String, String> vars) {
        String template = promptTemplate.getTemplate();

        return resolve(template, vars);
    }

    // 임시 실행 시에도 사용
    public String resolve(String template, Map<String, String> vars) {
        String resolved = doResolve(vars, template);

        verifyAllSlotsResolved(resolved);

        return resolved;
    }

    private String doResolve(Map<String, String> vars, String template) {
        String resolved = template;

        for (Map.Entry<String, String> entry : vars.entrySet()) {
            if (entry.getValue().trim().isBlank())
                throw new GeneralException(ErrorStatus.PROMPT_TEMPLATE_RESOLUTION_FAILED);

            String target = Pattern.quote("{{" + entry.getKey() + "}}");
            resolved = resolved.replaceAll(target, entry.getValue());
        }

        return resolved;
    }

    public Set<String> getSlotKeys(String template) {
        Matcher matcher = SLOT_PATTERN.matcher(template);
        Set<String> keys = new HashSet<>();

        while (matcher.find()) {
            keys.add(matcher.group(1));
        }

        return keys;
    }

    private void verifyAllSlotsResolved(String template) {
        if (template == null || template.isEmpty()) return;

        boolean hasUnresolved = SLOT_PATTERN.matcher(template).find();
        if (hasUnresolved) {
            throw new GeneralException(ErrorStatus.PROMPT_TEMPLATE_RESOLUTION_FAILED);
        }
    }

}
