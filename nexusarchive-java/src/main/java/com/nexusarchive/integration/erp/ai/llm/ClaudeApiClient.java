// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/ClaudeApiClient.java
package com.nexusarchive.integration.erp.ai.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.config.AiProperties;
import com.nexusarchive.integration.erp.ai.llm.claude.CompletionRequest;
import com.nexusarchive.integration.erp.ai.llm.claude.CompletionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeApiClient {

    private final AiProperties properties;
    private final RateLimitService rateLimiter;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String API_URL = "https://api.anthropic.com/v1/messages";

    /**
     * 发送 completion 请求
     */
    public String complete(String userPrompt) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("AI generation is disabled");
        }

        // 检查速率限制
        if (!rateLimiter.tryAcquire()) {
            throw new IllegalStateException("AI API rate limit exceeded. Please try again later.");
        }

        log.info("Calling Claude API with model: {}", properties.getModel());

        // 构建请求
        CompletionRequest request = CompletionRequest.builder()
            .model(properties.getModel())
            .messages(List.of(
                CompletionRequest.Message.builder()
                    .role("user")
                    .content(userPrompt)
                    .build()
            ))
            .maxTokens(properties.getMaxTokens())
            .temperature(properties.getTemperature())
            .build();

        // 设置 HTTP 头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<CompletionRequest> entity = new HttpEntity<>(request, headers);

        try {
            // 发送请求
            String response = restTemplate.postForObject(API_URL, entity, String.class);

            // 解析响应 - 使用 CompletionResponse DTO
            CompletionResponse claudeResponse = objectMapper.readValue(response, CompletionResponse.class);

            // 空值安全检查
            if (claudeResponse.getContent() == null || claudeResponse.getContent().isEmpty()) {
                throw new RuntimeException("Invalid response: content array is empty");
            }

            String generatedCode = claudeResponse.getContent().get(0).getText();
            if (generatedCode == null) {
                throw new RuntimeException("Invalid response: text field is null");
            }

            log.info("Claude API returned {} characters", generatedCode.length());

            return generatedCode;

        } catch (org.springframework.web.client.RestClientException e) {
            log.error("HTTP request to Claude API failed", e);
            throw new RuntimeException("AI generation failed: " + e.getMessage(), e);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Failed to parse Claude API response", e);
            throw new RuntimeException("Invalid API response format", e);
        }
    }
}
