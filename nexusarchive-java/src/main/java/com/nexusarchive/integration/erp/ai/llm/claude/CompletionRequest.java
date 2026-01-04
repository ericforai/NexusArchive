// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/claude/CompletionRequest.java
package com.nexusarchive.integration.erp.ai.llm.claude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletionRequest {
    private String model;
    private List<Message> messages;
    private int maxTokens;
    private double temperature;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}
