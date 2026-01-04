// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/glm/GlmApiClient.java
package com.nexusarchive.integration.erp.ai.llm.glm;

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
 * GLM (智谱AI) API 客户端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlmApiClient {

    private final AiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // GLM API 端点
    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    /**
     * 发送 completion 请求
     */
    public String complete(String userPrompt) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("AI generation is disabled");
        }

        log.info("Calling GLM API with model: {}", properties.getModel());

        try {
            // 构建请求（使用 GLM-4.7 或配置的模型）
            GlmCompletionRequest request = GlmCompletionRequest.builder()
                .model(properties.getModel())
                .messages(List.of(
                    GlmCompletionRequest.Message.builder()
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
            GlmCompletionResponse glmResponse = objectMapper.readValue(response, GlmCompletionResponse.class);

            // 空值安全检查
            if (glmResponse.getChoices() == null || glmResponse.getChoices().isEmpty()) {
                throw new RuntimeException("Invalid response: choices array is empty");
            }

            String generatedText = glmResponse.getChoices().get(0).getMessage().getContent();
            if (generatedText == null) {
                throw new RuntimeException("Invalid response: content field is null");
            }

            log.info("GLM API returned {} characters", generatedText.length());

            return generatedText;

        } catch (org.springframework.web.client.RestClientException e) {
            log.error("HTTP request to GLM API failed", e);
            throw new RuntimeException("AI generation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("GLM API call failed", e);
            throw new RuntimeException("AI generation failed: " + e.getMessage(), e);
        }
    }
}
