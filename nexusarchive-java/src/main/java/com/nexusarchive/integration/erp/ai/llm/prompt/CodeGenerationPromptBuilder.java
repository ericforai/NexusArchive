// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/prompt/CodeGenerationPromptBuilder.java
package com.nexusarchive.integration.erp.ai.llm.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeGenerationPromptBuilder {

    private final ResourceLoader resourceLoader;

    /**
     * 构建代码生成 Prompt
     */
    public String buildPrompt(PromptContext context) {
        try {
            // 读取模板
            String template = loadTemplate();

            // 替换变量 (处理 null 值)
            return template
                .replace("{erpType}", nonNull(context.getErpType()))
                .replace("{erpName}", nonNull(context.getErpName()))
                .replace("{className}", nonNull(context.getClassName()))
                .replace("{packageName}", nonNull(context.getPackageName()))
                .replace("{baseUrl}", nonNull(context.getBaseUrl()))
                .replace("{authType}", nonNull(context.getAuthType()))
                .replace("{timestamp}", String.valueOf(System.currentTimeMillis()))
                .replace("{apiDefinitions}", formatApiDefinitions(context.getApiDefinitions()))
                .replace("{authTemplate}", buildAuthTemplate(context.getAuthType()))
                .replace("{scenarioList}", buildScenarioList(context.getApiDefinitions()));

        } catch (IOException e) {
            log.error("Failed to load prompt template", e);
            throw new RuntimeException("Prompt template loading failed", e);
        }
    }

    private String nonNull(String value) {
        return value != null ? value : "https://api.example.com";
    }

    private String loadTemplate() throws IOException {
        var resource = resourceLoader.getResource("classpath:prompts/adapter-template.txt");
        try (var is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String formatApiDefinitions(List<PromptContext.ApiDefinition> definitions) {
        return definitions.stream()
            .map(d -> String.format("""
                - %s %s: %s
                  请求: %s
                  响应: %s
                """, d.getMethod(), d.getPath(), d.getSummary(),
                  d.getRequestSchema(), d.getResponseSchema()))
            .collect(Collectors.joining("\n"));
    }

    private String buildAuthTemplate(String authType) {
        String type = authType != null ? authType : "none";
        return switch (type.toLowerCase()) {
            case "appkey" -> """
                使用 AppKey + AppSecret + Timestamp 签名：
                ```java
                String signature = calculateSignature(appKey, appSecret, timestamp);
                headers.set("X-App-Key", appKey);
                headers.set("X-Timestamp", timestamp);
                headers.set("X-Signature", signature);
                ```
                """;

            case "oauth2" -> """
                使用 OAuth2 Bearer Token：
                ```java
                headers.set("Authorization", "Bearer " + token);
                ```
                """;

            default -> "无需认证";
        };
    }

    private String buildScenarioList(List<PromptContext.ApiDefinition> definitions) {
        return definitions.stream()
            .map(d -> "\"" + extractScenario(d.getOperationId()) + "\"")
            .collect(Collectors.joining(", "));
    }

    private String extractScenario(String operationId) {
        // Convert "salesOutList" -> "salesOutSync"
        if (operationId.endsWith("List")) {
            return operationId.substring(0, operationId.length() - 4) + "Sync";
        }
        return operationId + "Sync"; // Default fallback
    }
}
