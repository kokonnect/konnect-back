package com.example.konnect_backend.domain.ai.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class DiscordWebhookService {

    private final RestTemplate restTemplate;
    private final String DISCORD_WEBHOOK_URL;

    public DiscordWebhookService(RestTemplate restTemplate,
                                 @Value("${discord.webhook-url:#{null}}") String discordWebhookUrl) {
        this.restTemplate = restTemplate;
        this.DISCORD_WEBHOOK_URL = discordWebhookUrl;
    }

    public void notifyStateChange(boolean isLlmDown) {
        if(StringUtils.hasText(DISCORD_WEBHOOK_URL)) {
            return;
        }

        String status = isLlmDown ? "🔴 DOWN" : "🟢 UP";
        String timestamp = java.time.OffsetDateTime.now().toString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> embed = Map.of(
            "title", "LLM 상태 변경",
            "description", status,
            "timestamp", timestamp
        );

        Map<String, Object> payload = Map.of(
            "embeds", List.of(embed)
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        restTemplate.postForEntity(DISCORD_WEBHOOK_URL, entity, String.class);
    }
}
