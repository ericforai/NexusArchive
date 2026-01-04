// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/claude/CompletionResponse.java
package com.nexusarchive.integration.erp.ai.llm.claude;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CompletionResponse {
    private String id;
    private String role;
    private List<Content> content;

    @JsonProperty("stop_reason")
    private String stopReason;

    @Data
    public static class Content {
        private String type;
        private String text;
    }
}
