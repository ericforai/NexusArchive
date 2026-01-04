// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/glm/GlmCompletionResponse.java
package com.nexusarchive.integration.erp.ai.llm.glm;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * GLM API 响应格式（兼容 OpenAI 格式）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlmCompletionResponse {
    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private Integer index;
        private Message message;
        private String finish_reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        private Integer prompt_tokens;
        private Integer completion_tokens;
        private Integer total_tokens;
    }
}
