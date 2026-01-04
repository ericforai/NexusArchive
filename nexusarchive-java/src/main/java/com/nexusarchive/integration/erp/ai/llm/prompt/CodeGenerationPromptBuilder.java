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

            // 替换变量
            return template
                .replace("{erpType}", context.getErpType())
                .replace("{erpName}", context.getErpName())
                .replace("{className}", context.getClassName())
                .replace("{packageName}", context.getPackageName())
                .replace("{baseUrl}", context.getBaseUrl())
                .replace("{authType}", context.getAuthType())
                .replace("{apiDefinitions}", formatApiDefinitions(context.getApiDefinitions()))
                .replace("{authTemplate}", buildAuthTemplate(context.getAuthType()))
                .replace("{scenarioList}", buildScenarioList(context.getApiDefinitions()));

        } catch (IOException e) {
            log.error("Failed to load prompt template", e);
            throw new RuntimeException("Prompt template loading failed", e);
        }
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
        return switch (authType.toLowerCase()) {
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
        // 从 operationId 提取场景名
        return operationId.replaceAll("([A-Z])", "_$1").toLowerCase();
    }
}
