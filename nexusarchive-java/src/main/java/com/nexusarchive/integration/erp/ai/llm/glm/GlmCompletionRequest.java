// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/glm/GlmCompletionRequest.java
package com.nexusarchive.integration.erp.ai.llm.glm;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GLM API 请求格式（兼容 OpenAI 格式）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlmCompletionRequest {
    private String model;
    private List<Message> messages;
    private Double temperature;
    private Integer max_tokens;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}
