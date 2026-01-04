// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/deepseek/DeepSeekApiClient.java
package com.nexusarchive.integration.erp.ai.llm.deepseek;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * DeepSeek API 客户端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeepSeekApiClient {

    private final AiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // DeepSeek API 端点
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";

    /**
     * 发送 completion 请求
     */
    public String complete(String userPrompt) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("AI generation is disabled");
        }

        log.info("Calling DeepSeek API with model: {}", properties.getModel());

        try {
            // 构建请求
            DeepSeekCompletionRequest request = DeepSeekCompletionRequest.builder()
                .model(properties.getModel())
                .messages(List.of(
                    DeepSeekCompletionRequest.Message.builder()
                        .role("user")
                        .content(userPrompt)
                        .build()
                ))
                .temperature(properties.getTemperature())
                .max_tokens(properties.getMaxTokens())
                .build();

            // 序列化为 JSON 字符串
            String requestBody = objectMapper.writeValueAsString(request);
            log.debug("Request body: {}", requestBody);

            // 设置 HTTP 头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getApiKey());

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.postForObject(API_URL, entity, String.class);

            // 解析响应
            DeepSeekCompletionResponse deepSeekResponse = objectMapper.readValue(response, DeepSeekCompletionResponse.class);

            // 空值安全检查
            if (deepSeekResponse.getChoices() == null || deepSeekResponse.getChoices().isEmpty()) {
                throw new RuntimeException("Invalid response: choices array is empty");
            }

            String generatedText = deepSeekResponse.getChoices().get(0).getMessage().getContent();
            if (generatedText == null) {
                throw new RuntimeException("Invalid response: content field is null");
            }

            log.info("DeepSeek API returned {} characters", generatedText.length());

            return generatedText;

        } catch (org.springframework.web.client.RestClientException e) {
            log.error("HTTP request to DeepSeek API failed", e);
            throw new RuntimeException("AI generation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("DeepSeek API call failed", e);
            throw new RuntimeException("AI generation failed: " + e.getMessage(), e);
        }
    }
}
