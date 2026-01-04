// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/prompt/PromptContext.java
package com.nexusarchive.integration.erp.ai.llm.prompt;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PromptContext {
    private String erpType;
    private String erpName;
    private String className;
    private String packageName;
    private String baseUrl;
    private String authType; // "appkey", "oauth2", "none"
    private List<ApiDefinition> apiDefinitions;

    @Data
    @Builder
    public static class ApiDefinition {
        private String operationId;
        private String method;
        private String path;
        private String summary;
        private String requestSchema;
        private String responseSchema;
    }
}
