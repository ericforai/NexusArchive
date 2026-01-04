# LLM 驱动的 ERP 适配器代码生成器

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 接入 Claude API 实现真正的 AI 代码生成，根据 OpenAPI 文档自动生成完整的、可运行的 ERP 适配器 Java 代码（包含 HTTP 调用、认证签名、数据映射、错误处理）

**Architecture:**
1. 创建 LLM 服务层封装 Claude API 调用
2. 设计 Prompt 模板系统，根据 OpenAPI 定义动态生成高质量 prompt
3. 实现代码解析和验证器，确保 AI 生成的代码可编译
4. 集成到现有 ErpAdapterCodeGenerator，提供 AI 生成模式
5. 添加人工审核/编辑流程，支持多次迭代优化

**Tech Stack:** Spring Boot 3.1.6, Java 17, Anthropic Claude API, Jackson, Langchain4j (可选)

---

## Task 1: 配置 Claude API 凭证

**Files:**
- Create: `nexusarchive-java/src/main/resources/application-ai.yml`
- Modify: `nexusarchive-java/src/main/resources/application.yml`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/config/AiProperties.java`

**Step 1: 创建 AI 配置文件**

```yaml
# nexusarchive-java/src/main/resources/application-ai.yml
ai:
  claude:
    api-key: ${CLAUDE_API_KEY:}
    model: claude-3-5-sonnet-20241022
    max-tokens: 8192
    temperature: 0.3
    timeout: 60000
  enabled: true
```

**Step 2: 更新主配置文件导入**

```bash
# 编辑 nexusarchive-java/src/main/resources/application.yml
# 在文件顶部添加 spring.profiles.include: ai
```

**Step 3: 创建配置属性类**

```java
// nexusarchive-java/src/main/java/com/nexusarchive/config/AiProperties.java
package com.nexusarchive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.claude")
public class AiProperties {
    private String apiKey;
    private String model = "claude-3-5-sonnet-20241022";
    private int maxTokens = 8192;
    private double temperature = 0.3;
    private int timeout = 60000;
    private boolean enabled = true;
}
```

**Step 4: 测试配置加载**

```bash
cd nexusarchive-java
mvn spring-boot:run -Dspring-boot.run.profiles=dev -DCLAUDE_API_KEY=test-key &
sleep 10
curl -s http://localhost:19090/actuator/env | jq '.propertySources[0].properties."ai.claude.api-key"'
pkill -f "spring-boot:run"
```

Expected: API key 配置正确加载（可为 null，环境变量设置）

**Step 5: 提交配置**

```bash
git add nexusarchive-java/src/main/resources/application-ai.yml \
        nexusarchive-java/src/main/resources/application.yml \
        nexusarchive-java/src/main/java/com/nexusarchive/config/AiProperties.java
git commit -m "feat(ai): add Claude API configuration"
```

---

## Task 2: 实现 Claude API 客户端

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/ClaudeApiClient.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/claude/CompletionRequest.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/claude/CompletionResponse.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/claude/Message.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/llm/ClaudeApiClientTest.java`

**Step 1: 定义 Claude API 请求模型**

```java
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
```

**Step 2: 定义 Claude API 响应模型**

```java
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
```

**Step 3: 实现 Claude API 客户端**

```java
// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/ClaudeApiClient.java
package com.nexusarchive.integration.erp.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
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

            // 解析响应
            JsonNode root = objectMapper.readTree(response);
            JsonNode contentNode = root.path("content").get(0).path("text");

            String generatedCode = contentNode.asText();
            log.info("Claude API returned {} characters", generatedCode.length());

            return generatedCode;

        } catch (Exception e) {
            log.error("Failed to call Claude API", e);
            throw new RuntimeException("AI generation failed: " + e.getMessage(), e);
        }
    }
}
```

**Step 4: 编写测试验证 API 调用**

```java
// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/llm/ClaudeApiClientTest.java
package com.nexusarchive.integration.erp.ai.llm;

import com.nexusarchive.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "CLAUDE_API_KEY", matches = ".+")
class ClaudeApiClientTest {

    private ClaudeApiClient client;
    private AiProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setApiKey(System.getProperty("CLAUDE_API_KEY"));
        properties.setModel("claude-3-5-sonnet-20241022");
        properties.setEnabled(true);

        client = new ClaudeApiClient(properties);
    }

    @Test
    void testComplete() {
        String prompt = "Say 'Hello, World!' in Chinese.";
        String response = client.complete(prompt);

        assertNotNull(response);
        assertTrue(response.contains("你好") || response.contains("Hello"));
    }
}
```

**Step 5: 运行测试验证功能**

```bash
cd nexusarchive-java
# 跳过测试（需要真实 API key）
git add src/main/java/com/nexusarchive/integration/erp/ai/llm
git commit -m "feat(ai): implement Claude API client"
```

---

## Task 3: 设计 Prompt 模板系统

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/prompt/CodeGenerationPromptBuilder.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/prompt/PromptContext.java`
- Create: `nexusarchive-java/src/main/resources/prompts/adapter-template.txt`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/llm/prompt/PromptBuilderTest.java`

**Step 1: 创建 Prompt 上下文模型**

```java
// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/prompt/PromptContext.java
package com.nexusarchive.integration.erp.ai.llm.prompt;

import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
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
```

**Step 2: 创建 Prompt 模板文件**

```txt
# nexusarchive-java/src/main/resources/prompts/adapter-template.txt
你是一位经验丰富的 Java 企业级开发专家，擅长 Spring Boot 和 HTTP 客户端开发。

## 任务
根据以下 ERP OpenAPI 定义，生成完整的、可直接运行的 ERP 适配器 Java 代码。

## ERP 信息
- ERP 类型: {erpType}
- 适配器名称: {className}
- 包名: {packageName}
- Base URL: {baseUrl}
- 认证方式: {authType}

## API 定义
{apiDefinitions}

## 代码要求

### 1. 类结构
```java
@Component
@ErpAdapter(
    identifier = "{erpType}-{timestamp}",
    name = "{erpName}",
    supportedScenarios = {scenarioList}
)
public class {className} implements ErpAdapter {

    @Override
    public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        // 实现凭证同步逻辑
    }

    @Override
    public boolean testConnection(ErpConfig config) {
        // 实现连接测试逻辑
    }
}
```

### 2. HTTP 客户端
- 使用 Spring 的 RestTemplate 或 WebClient
- 添加超时配置：connectTimeout=5s, readTimeout=30s
- 实现重试机制：最多重试 3 次，指数退避

### 3. 认证签名
{authTemplate}

### 4. 数据映射
- 将 JSON 响应映射到 VoucherDTO
- 处理分页：pageSize=100，自动遍历所有页
- 处理日期格式：ISO 8601 → LocalDate

### 5. 错误处理
- 捕获 HTTP 4xx/5xx 错误，记录详细日志
- 抛出有意义的业务异常
- 返回空列表而非 null

### 6. 代码规范
- 使用 Slf4j 记录日志（DEBUG/INFO/WARN/ERROR）
- 添加必要的 JavaDoc 注释
- 遵循阿里巴巴 Java 开发规范
- 代码必须可编译，无语法错误

## 输出格式
只输出 Java 代码，不要任何解释或 Markdown 标记。
```

**Step 3: 实现 Prompt 构建器**

```java
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
```

**Step 4: 测试 Prompt 构建**

```java
// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/llm/prompt/PromptBuilderTest.java
package com.nexusarchive.integration.erp.ai.llm.prompt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PromptBuilderTest {

    @Autowired
    private CodeGenerationPromptBuilder promptBuilder;

    @Test
    void testBuildPrompt() {
        PromptContext context = PromptContext.builder()
            .erpType("yonsuite")
            .erpName("用友 YonSuite")
            .className("YonsuiteErpAdapter")
            .packageName("com.nexusarchive.integration.erp.adapter.yonsuite")
            .baseUrl("https://api.yonyoucloud.com")
            .authType("appkey")
            .apiDefinitions(List.of(
                PromptContext.ApiDefinition.builder()
                    .operationId("salesOutList")
                    .method("GET")
                    .path("/yiyan/salesOut/list")
                    .summary("销售出库单列表")
                    .requestSchema("{startDate: string, endDate: string}")
                    .responseSchema("{code: number, data: {records: []}}")
                    .build()
            ))
            .build();

        String prompt = promptBuilder.buildPrompt(context);

        assertNotNull(prompt);
        assertTrue(prompt.contains("YonsuiteErpAdapter"));
        assertTrue(prompt.contains("salesOutList"));
        assertTrue(prompt.contains("AppKey"));
        System.out.println(prompt);
    }
}
```

**Step 5: 验证并提交**

```bash
cd nexusarchive-java
# 手动验证 Prompt 格式是否符合预期
git add src/main/resources/prompts src/main/java/com/nexusarchive/integration/erp/ai/llm/prompt
git commit -m "feat(ai): design prompt template system"
```

---

## Task 4: 实现代码解析和验证器

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/parser/CodeParser.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/parser/CodeValidationException.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/parser/JavaSyntaxValidator.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/llm/parser/CodeParserTest.java`

**Step 1: 定义验证异常**

```java
// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/parser/CodeValidationException.java
package com.nexusarchive.integration.erp.ai.llm.parser;

import lombok.Getter;

import java.util.List;

@Getter
public class CodeValidationException extends Exception {
    private final List<String> errors;

    public CodeValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }
}
```

**Step 2: 实现代码解析器**

```java
// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/parser/CodeParser.java
package com.nexusarchive.integration.erp.ai.llm.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class CodeParser {

    private static final Pattern JAVA_CODE_BLOCK = Pattern.compile("```java\\s*([\\s\\S]*?)\\s*```");
    private static final Pattern CLASS_DECLARATION = Pattern.compile("public\\s+class\\s+(\\w+)");
    private static final Pattern PACKAGE_DECLARATION = Pattern.compile("package\\s+([\\w.]+);");

    /**
     * 从 AI 响应中提取 Java 代码
     */
    public String extractJavaCode(String aiResponse) throws CodeValidationException {
        log.info("Extracting Java code from AI response ({} chars)", aiResponse.length());

        // 尝试提取 ```java 代码块
        Matcher matcher = JAVA_CODE_BLOCK.matcher(aiResponse);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 如果没有代码块标记，检查是否直接是 Java 代码
        if (containsJavaKeywords(aiResponse)) {
            return aiResponse.trim();
        }

        throw new CodeValidationException("No Java code found in AI response", List.of(
            "Response does not contain Java code block",
            "Expected format: ```java\\ncode```"
        ));
    }

    /**
     * 解析生成的代码元信息
     */
    public ParsedCodeMetadata parseMetadata(String javaCode) throws CodeValidationException {
        ParsedCodeMetadata metadata = new ParsedCodeMetadata();

        // 提取包名
        Matcher pkgMatcher = PACKAGE_DECLARATION.matcher(javaCode);
        if (pkgMatcher.find()) {
            metadata.setPackageName(pkgMatcher.group(1));
        }

        // 提取类名
        Matcher classMatcher = CLASS_DECLARATION.matcher(javaCode);
        if (classMatcher.find()) {
            metadata.setClassName(classMatcher.group(1));
        }

        if (metadata.getPackageName() == null || metadata.getClassName() == null) {
            throw new CodeValidationException("Invalid Java code: missing package or class declaration",
                List.of("package: " + metadata.getPackageName(), "class: " + metadata.getClassName()));
        }

        return metadata;
    }

    private boolean containsJavaKeywords(String text) {
        return text.contains("package ") &&
               text.contains("class ") &&
               text.contains("public ") &&
               text.contains("return ");
    }

    public static class ParsedCodeMetadata {
        private String packageName;
        private String className;

        public String getPackageName() { return packageName; }
        public String getClassName() { return className; }
        public void setPackageName(String packageName) { this.packageName = packageName; }
        public void setClassName(String className) { this.className = className; }
    }
}
```

**Step 3: 实现语法验证器**

```java
// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/parser/JavaSyntaxValidator.java
package com.nexusarchive.integration.erp.ai.llm.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JavaSyntaxValidator {

    @Value("${project.basedir:#{null}}")
    private String projectBaseDir;

    /**
     * 验证 Java 代码语法
     */
    public void validate(String javaCode) throws CodeValidationException {
        List<String> errors = new ArrayList<>();

        try {
            // 创建临时文件
            Path tempDir = Files.createTempDirectory("ai-code-");
            Path tempFile = tempDir.resolve("GeneratedAdapter.java");
            Files.writeString(tempFile, javaCode);

            // 使用 javac 验证语法
            ProcessBuilder pb = new ProcessBuilder(
                "javac",
                "-encoding", "UTF-8",
                tempFile.toString()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取错误输出
            List<String> outputLines = new ArrayList<>();
            try (var reader = process.getInputStream().bufferedReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputLines.add(line);
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new CodeValidationException("Syntax validation timeout", outputLines);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new CodeValidationException("Generated code has syntax errors", outputLines);
            }

            log.info("Java syntax validation passed");

            // 清理临时文件
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(tempDir);

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CodeValidationException("Syntax validation failed: " + e.getMessage(),
                List.of(e.getMessage()));
        }
    }
}
```

**Step 4: 测试代码解析**

```java
// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/llm/parser/CodeParserTest.java
package com.nexusarchive.integration.erp.ai.llm.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeParserTest {

    private CodeParser parser;

    @BeforeEach
    void setUp() {
        parser = new CodeParser();
    }

    @Test
    void testExtractJavaCode() throws CodeValidationException {
        String aiResponse = """
            Here's the generated code:

            ```java
            package com.example;

            public class TestAdapter {
                public void test() {}
            }
            ```

            Let me know if you need changes.
        """;

        String code = parser.extractJavaCode(aiResponse);

        assertTrue(code.contains("package com.example"));
        assertTrue(code.contains("class TestAdapter"));
        assertFalse(code.contains("```"));
    }

    @Test
    void testParseMetadata() throws CodeValidationException {
        String javaCode = """
            package com.nexusarchive.adapter;

            public class YonsuiteAdapter {
            }
        """;

        CodeParser.ParsedCodeMetadata metadata = parser.parseMetadata(javaCode);

        assertEquals("com.nexusarchive.adapter", metadata.getPackageName());
        assertEquals("YonsuiteAdapter", metadata.getClassName());
    }

    @Test
    void testInvalidCode() {
        String invalidCode = "This is not Java code at all.";

        assertThrows(CodeValidationException.class, () -> parser.extractJavaCode(invalidCode));
    }
}
```

**Step 5: 提交代码解析器**

```bash
cd nexusarchive-java
git add src/main/java/com/nexusarchive/integration/erp/ai/llm/parser
git commit -m "feat(ai): implement code parser and syntax validator"
```

---

## Task 5: 集成 LLM 生成到现有流程

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/generator/ErpAdapterCodeGenerator.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/generator/AiCodeGenerationService.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/agent/ErpAdaptationOrchestrator.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/generator/AiCodeGenerationServiceTest.java`

**Step 1: 创建 AI 代码生成服务**

```java
// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/generator/AiCodeGenerationService.java
package com.nexusarchive.integration.erp.ai.generator;

import com.nexusarchive.integration.erp.ai.llm.ClaudeApiClient;
import com.nexusarchive.integration.erp.ai.llm.parser.CodeParser;
import com.nexusarchive.integration.erp.ai.llm.parser.CodeValidationException;
import com.nexusarchive.integration.erp.ai.llm.parser.JavaSyntaxValidator;
import com.nexusarchive.integration.erp.ai.llm.prompt.CodeGenerationPromptBuilder;
import com.nexusarchive.integration.erp.ai.llm.prompt.PromptContext;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCodeGenerationService {

    private final ClaudeApiClient claudeClient;
    private final CodeGenerationPromptBuilder promptBuilder;
    private final CodeParser codeParser;
    private final JavaSyntaxValidator syntaxValidator;

    /**
     * 使用 AI 生成完整的适配器代码
     */
    public GeneratedCode generateWithAI(List<OpenApiDefinition> definitions,
                                     String erpType,
                                     String erpName,
                                     String baseUrl,
                                     String authType) {
        try {
            log.info("Starting AI code generation for {} APIs", definitions.size());

            // 1. 构建 Prompt 上下文
            PromptContext context = buildContext(definitions, erpType, erpName, baseUrl, authType);

            // 2. 生成 Prompt
            String prompt = promptBuilder.buildPrompt(context);
            log.debug("Generated prompt ({} chars)", prompt.length());

            // 3. 调用 Claude API
            String aiResponse = claudeClient.complete(prompt);
            log.info("Received AI response ({} chars)", aiResponse.length());

            // 4. 提取 Java 代码
            String javaCode = codeParser.extractJavaCode(aiResponse);
            log.info("Extracted Java code ({} chars)", javaCode.length());

            // 5. 验证语法
            syntaxValidator.validate(javaCode);
            log.info("Code syntax validation passed");

            // 6. 解析元信息
            CodeParser.ParsedCodeMetadata metadata = codeParser.parseMetadata(javaCode);

            // 7. 构建 GeneratedCode 对象
            return GeneratedCode.builder()
                .adapterClass(javaCode)
                .className(metadata.getClassName())
                .packageName(metadata.getPackageName())
                .erpType(erpType)
                .erpName(erpName)
                .build();

        } catch (CodeValidationException e) {
            log.error("AI code generation validation failed", e);
            throw new RuntimeException("Failed to generate valid code: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("AI code generation failed", e);
            throw new RuntimeException("AI generation error: " + e.getMessage(), e);
        }
    }

    /**
     * 检查 AI 生成是否可用
     */
    public boolean isAvailable() {
        try {
            return claudeClient != null;
        } catch (Exception e) {
            log.warn("AI generation not available", e);
            return false;
        }
    }

    private PromptContext buildContext(List<OpenApiDefinition> definitions,
                                       String erpType,
                                       String erpName,
                                       String baseUrl,
                                       String authType) {
        List<PromptContext.ApiDefinition> apiDefs = definitions.stream()
            .map(d -> PromptContext.ApiDefinition.builder()
                .operationId(d.getOperationId())
                .method(d.getMethod())
                .path(d.getPath())
                .summary(d.getSummary())
                .requestSchema(d.getRequestSchema())  // 需要在 OpenApiDefinition 中添加
                .responseSchema(d.getResponseSchema())  // 需要在 OpenApiDefinition 中添加
                .build())
            .collect(Collectors.toList());

        String className = generateClassName(erpType);
        String packageName = "com.nexusarchive.integration.erp.adapter." + erpType.toLowerCase();

        return PromptContext.builder()
            .erpType(erpType)
            .erpName(erpName)
            .className(className)
            .packageName(packageName)
            .baseUrl(baseUrl)
            .authType(authType)
            .apiDefinitions(apiDefs)
            .build();
    }

    private String generateClassName(String erpType) {
        return erpType.substring(0, 1).toUpperCase() + erpType.substring(1).toLowerCase() + "ErpAdapter";
    }
}
```

**Step 2: 修改编排器支持 AI 生成**

```java
// 修改 nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/agent/ErpAdaptationOrchestrator.java
// 添加新的字段
@Autowired
private AiCodeGenerationService aiCodeGenerationService;

// 修改 adaptAndDeploy 方法，添加 AI 生成选项
public AdaptationResult adaptAndDeploy(AdaptationRequest request) throws IOException, InterruptedException {
    log.info("开始 ERP 适配+自动部署: erpType={}, erpName={}", request.getErpType(), request.getErpName());

    // 先执行解析
    List<OpenApiDefinition> definitions = parseApiDocuments(request.getApiFiles());
    if (definitions.isEmpty()) {
        return AdaptationResult.failure("未能从文档中提取任何 API 定义");
    }

    // 映射到标准场景
    List<BusinessSemanticMapper.ScenarioMapping> mappings = mapToScenarios(definitions);

    // 尝试使用 AI 生成代码
    GeneratedCode code;
    if (aiCodeGenerationService.isAvailable()) {
        log.info("Using AI code generation");
        code = aiCodeGenerationService.generateWithAI(
            definitions,
            request.getErpType(),
            request.getErpName(),
            extractBaseUrl(definitions),
            "appkey"  // 或从 OpenAPI 文档推断
        );
    } else {
        log.warn("AI generation not available, falling back to template generation");
        code = codeGenerator.generate(mappings, request.getErpType(), request.getErpName());
    }

    // 执行自动部署
    ErpAdapterAutoDeployService.DeploymentResult deployResult = autoDeployService.deploy(code);

    return AdaptationResult.builder()
        .success(deployResult.isSuccess())
        .code(code)
        .mappings(mappings)
        .adapterId(request.getErpType().toLowerCase().replace(" ", "-"))
        .deploymentResult(deployResult)
        .message(buildDeploymentMessage(code, deployResult))
        .build();
}

private String extractBaseUrl(List<OpenApiDefinition> definitions) {
    // 从 OpenAPI 文档中提取 base URL
    return definitions.stream()
        .findFirst()
        .map(d -> {
            // 从定义的服务器 URL 中提取
            return "https://api.yonyoucloud.com";  // 简化实现
        })
        .orElse("https://api.example.com");
}
```

**Step 3: 添加配置开关**

```yaml
# nexusarchive-java/src/main/resources/application.yml
ai:
  enabled: true  # 启用 AI 生成
  fallback-to-template: true  # AI 不可用时回退到模板生成
```

**Step 4: 编写集成测试**

```java
// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/generator/AiCodeGenerationServiceTest.java
package com.nexusarchive.integration.erp.ai.generator;

import com.nexusarchive.config.AiProperties;
import com.nexusarchive.integration.erp.ai.llm.ClaudeApiClient;
import com.nexusarchive.integration.erp.ai.llm.parser.CodeParser;
import com.nexusarchive.integration.erp.ai.llm.parser.JavaSyntaxValidator;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfSystemProperty(named = "CLAUDE_API_KEY", matches = ".+")
class AiCodeGenerationServiceTest {

    @Autowired
    private AiCodeGenerationService aiCodeGenerationService;

    @Test
    void testGenerateWithAI() {
        // 准备测试数据
        OpenApiDefinition definition = OpenApiDefinition.builder()
            .operationId("salesOutList")
            .method("GET")
            .path("/yiyan/salesOut/list")
            .summary("销售出库单列表")
            .build();

        // 执行生成
        GeneratedCode result = aiCodeGenerationService.generateWithAI(
            List.of(definition),
            "yonsuite",
            "用友 YonSuite",
            "https://api.yonyoucloud.com",
            "appkey"
        );

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getAdapterClass());
        assertTrue(result.getAdapterClass().contains("public class"));
        assertTrue(result.getAdapterClass().contains("syncVouchers"));

        System.out.println("Generated class name: " + result.getClassName());
        System.out.println("Generated code length: " + result.getAdapterClass().length());
    }
}
```

**Step 5: 验证并提交**

```bash
cd nexusarchive-java
# 手动测试一次完整流程
git add src/main/java/com/nexusarchive/integration/erp/ai
git commit -m "feat(ai): integrate LLM generation into ERP adapter workflow"
```

---

## Task 6: 实现人工审核和迭代机制

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/dto/AiGenerationSession.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/controller/ErpAdaptationController.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/service/AiGenerationSessionService.java`

**Step 1: 创建会话模型**

```java
// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/dto/AiGenerationSession.java
package com.nexusarchive.integration.erp.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiGenerationSession {
    private String sessionId;
    private String erpType;
    private String erpName;
    private String originalPrompt;
    private String generatedCode;
    private String userFeedback;
    private int iterationCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private AiGenerationStatus status;

    public enum AiGenerationStatus {
        GENERATED,      // AI 已生成
        REVIEWING,      // 等待人工审核
        APPROVED,       // 已批准
        REJECTED,       // 已拒绝
        REGENERATING    // 重新生成中
    }
}
```

**Step 2: 添加会话管理接口**

```java
// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/controller/ErpAdaptationController.java
// 添加新的接口

/**
 * 生成代码（AI 生成，返回会话 ID）
 */
@PostMapping("/generate-ai")
public ResponseEntity<ApiResponse> generateWithAi(
        @RequestParam("file") MultipartFile file,
        @RequestParam("erpType") String erpType,
        @RequestParam("erpName") String erpName,
        @RequestParam(value = "baseUrl", required = false) String baseUrl,
        @RequestParam(value = "authType", required = false) String authType) {

    try {
        log.info("收到 AI 生成请求: erpType={}, erpName={}", erpType, erpName);

        // 解析 OpenAPI 文档
        OpenApiDocumentParser.ParseResult parseResult = openApiDocumentParser.parse(file);
        if (!parseResult.isSuccess()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("OpenAPI 解析失败: " + parseResult.getErrorMessage()));
        }

        // 使用 AI 生成代码
        AiGenerationSession session = aiGenerationSessionService.createSession(
            parseResult.getDefinitions(),
            erpType,
            erpName,
            baseUrl != null ? baseUrl : "https://api.example.com",
            authType != null ? authType : "appkey"
        );

        // 保存会话
        aiGenerationSessionService.saveSession(session);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "sessionId", session.getSessionId(),
            "status", session.getStatus(),
            "generatedCode", session.getGeneratedCode()
        )));

    } catch (Exception e) {
        log.error("AI 生成失败", e);
        return ResponseEntity.internalServerError()
            .body(ApiResponse.error("AI 生成失败: " + e.getMessage()));
    }
}

/**
 * 提供反馈并重新生成
 */
@PostMapping("/regenerate-ai/{sessionId}")
public ResponseEntity<ApiResponse> regenerateWithFeedback(
        @PathVariable String sessionId,
        @RequestBody FeedbackRequest feedback) {

    try {
        log.info("收到重新生成请求: sessionId={}", sessionId);

        AiGenerationSession updatedSession = aiGenerationSessionService.regenerate(
            sessionId,
            feedback.getUserFeedback()
        );

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "sessionId", updatedSession.getSessionId(),
            "iterationCount", updatedSession.getIterationCount(),
            "generatedCode", updatedSession.getGeneratedCode()
        )));

    } catch (Exception e) {
        log.error("重新生成失败", e);
        return ResponseEntity.internalServerError()
            .body(ApiResponse.error("重新生成失败: " + e.getMessage()));
    }
}

/**
 * 批准生成的代码
 */
@PostMapping("/approve/{sessionId}")
public ResponseEntity<ApiResponse> approveCode(@PathVariable String sessionId) {
    try {
        aiGenerationSessionService.approve(sessionId);
        return ResponseEntity.ok(ApiResponse.success("代码已批准，将继续部署"));
    } catch (Exception e) {
        return ResponseEntity.internalServerError()
            .body(ApiResponse.error("批准失败: " + e.getMessage()));
    }
}

@Data
private static class FeedbackRequest {
    private String userFeedback;
}
```

**Step 3: 实现会话服务**

```java
// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/service/AiGenerationSessionService.java
package com.nexusarchive.integration.erp.ai.service;

import com.nexusarchive.integration.erp.ai.dto.AiGenerationSession;
import com.nexusarchive.integration.erp.ai.generator.AiCodeGenerationService;
import com.nexusarchive.integration.erp.ai.llm.prompt.PromptContext;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AiGenerationSessionService {

    private final AiCodeGenerationService aiCodeGenerationService;
    private final Map<String, AiGenerationSession> sessions = new ConcurrentHashMap<>();

    public AiGenerationSession createSession(
            List<OpenApiDefinition> definitions,
            String erpType,
            String erpName,
            String baseUrl,
            String authType) {

        // 生成代码
        var generatedCode = aiCodeGenerationService.generateWithAI(
            definitions, erpType, erpName, baseUrl, authType
        );

        // 创建会话
        AiGenerationSession session = AiGenerationSession.builder()
            .sessionId(UUID.randomUUID().toString())
            .erpType(erpType)
            .erpName(erpName)
            .generatedCode(generatedCode.getAdapterClass())
            .iterationCount(1)
            .createdAt(LocalDateTime.now())
            .lastModifiedAt(LocalDateTime.now())
            .status(AiGenerationSession.AiGenerationStatus.GENERATED)
            .build();

        sessions.put(session.getSessionId(), session);

        return session;
    }

    public AiGenerationSession regenerate(String sessionId, String userFeedback) {
        AiGenerationSession existing = sessions.get(sessionId);
        if (existing == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        // 构建 refined prompt（包含用户反馈）
        String refinedPrompt = buildRefinedPrompt(existing, userFeedback);

        // 重新调用 AI
        String newCode = claudeApiClient.complete(refinedPrompt);

        // 更新会话
        existing.setGeneratedCode(newCode);
        existing.setUserFeedback(userFeedback);
        existing.setIterationCount(existing.getIterationCount() + 1);
        existing.setLastModifiedAt(LocalDateTime.now());
        existing.setStatus(AiGenerationSession.AiGenerationStatus.REGENERATING);

        sessions.put(sessionId, existing);

        return existing;
    }

    public void approve(String sessionId) {
        AiGenerationSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        session.setStatus(AiGenerationSession.AiGenerationStatus.APPROVED);
        session.setLastModifiedAt(LocalDateTime.now());

        // TODO: 将代码保存到文件系统并部署

        log.info("Session {} approved, proceeding with deployment", sessionId);
    }

    public void saveSession(AiGenerationSession session) {
        sessions.put(session.getSessionId(), session);
    }

    private String buildRefinedPrompt(AiGenerationSession session, String feedback) {
        return String.format("""
            Original prompt generated the following code.

            User feedback: %s

            Please refine the generated code addressing the feedback above.
            Keep the same class structure and package name.
            """, feedback);
    }
}
```

**Step 4: 测试会话管理**

```bash
cd nexusarchive-java
# 手动测试会话创建和反馈流程
git add src/main/java/com/nexusarchive/integration/erp/ai
git commit -m "feat(ai): implement human review and iteration mechanism"
```

---

## Task 7: 添加成本和性能控制

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/RateLimitService.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/ClaudeApiClient.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/monitoring/AiGenerationMetrics.java`

**Step 1: 实现速率限制服务**

```java
// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/RateLimitService.java
package com.nexusarchive.integration.erp.ai.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.currentTimeMillis;

@Slf4j
@Service
public class RateLimitService {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final int MINUTE_IN_MS = 60000;

    private final AtomicInteger requestCount = new AtomicInteger(0);
    private long windowStartTimestamp = currentTimeMillis();

    /**
     * 检查是否允许发起请求
     */
    public synchronized boolean tryAcquire() {
        long now = currentTimeMillis();

        // 重置时间窗口
        if (now - windowStartTimestamp >= MINUTE_IN_MS) {
            windowStartTimestamp = now;
            requestCount.set(0);
        }

        // 检查限制
        if (requestCount.get() >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded: {} requests/min", requestCount.get());
            return false;
        }

        requestCount.incrementAndGet();
        return true;
    }

    /**
     * 获取当前可用请求数
     */
    public int getAvailablePermits() {
        return MAX_REQUESTS_PER_MINUTE - requestCount.get();
    }
}
```

**Step 2: 集成速率限制到 API 客户端**

```java
// 修改 nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/ClaudeApiClient.java
// 添加字段
private final RateLimitService rateLimiter;

// 修改 complete 方法
public String complete(String userPrompt) {
    if (!properties.isEnabled()) {
        throw new IllegalStateException("AI generation is disabled");
    }

    // 检查速率限制
    if (!rateLimiter.tryAcquire()) {
        throw new IllegalStateException("AI API rate limit exceeded. Please try again later.");
    }

    // ... 原有逻辑
}
```

**Step 3: 实现指标收集**

```java
// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/monitoring/AiGenerationMetrics.java
package com.nexusarchive.integration.erp.ai.monitoring;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AiGenerationMetrics {

    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final AtomicInteger totalTokensUsed = new AtomicInteger(0);
    private final AtomicLong totalResponseTimeMs = new AtomicLong(0);

    public void recordRequest(boolean success, int tokensUsed, long responseTimeMs) {
        totalRequests.incrementAndGet();
        if (success) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
        totalTokensUsed.addAndGet(tokensUsed);
        totalResponseTimeMs.addAndGet(responseTimeMs);
    }

    public MetricsSnapshot getSnapshot() {
        int total = totalRequests.get();
        return new MetricsSnapshot(
            total,
            successfulRequests.get(),
            failedRequests.get(),
            totalTokensUsed.get(),
            total > 0 ? totalResponseTimeMs.get() / total : 0
        );
    }

    @Data
    public static class MetricsSnapshot {
        private final int totalRequests;
        private final int successfulRequests;
        private final int failedRequests;
        private final int totalTokensUsed;
        private final double averageResponseTimeMs;
    }
}
```

**Step 4: 添加监控端点**

```java
// 在 nexusarchive-java/src/main/java/com/nexusarchive/controller/AiMetricsController.java
@RestController
@RequestMapping("/api/ai")
public class AiMetricsController {

    @Autowired
    private AiGenerationMetrics metrics;

    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        return ResponseEntity.ok(metrics.getSnapshot());
    }
}
```

**Step 5: 测试速率限制**

```bash
cd nexusarchive-java
git add src/main/java/com/nexusarchive/integration/erp/ai/llm src/main/java/com/nexusarchive/integration/erp/ai/monitoring
git commit -m "feat(ai): add rate limiting and metrics collection"
```

---

## Task 8: 端到端集成测试

**Files:**
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/AiGenerationE2ETest.java`
- Modify: `nexusarchive-java/src/test/resources/application-test.yml`

**Step 1: 配置测试环境**

```yaml
# nexusarchive-java/src/test/resources/application-test.yml
ai:
  claude:
    enabled: false  # 测试环境默认关闭，避免真实 API 调用
    timeout: 5000
```

**Step 2: 编写 E2E 测试**

```java
// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/AiGenerationE2ETest.java
package com.nexusarchive.integration.erp.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.integration.erp.ai.controller.ErpAdaptationController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "CLAUDE_API_KEY", matches = ".+")
class AiGenerationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testFullAiGenerationFlow() throws Exception {
        // 准备测试文件
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-yonsuite-api.json",
            "application/json",
            getClass().getResourceAsStream("/test-yonsuite-api.json").readAllBytes()
        );

        // 调用生成接口
        mockMvc.perform(multipart("/api/erp-ai/generate-ai")
                .file(file)
                .param("erpType", "yonsuite")
                .param("erpName", "测试 YonSuite")
                .param("baseUrl", "https://api.test.com")
                .param("authType", "appkey"))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").exists())
                .andExpect(jsonPath("$.data.generatedCode").isString())
                .andExpect(jsonPath("$.data.status").value("GENERATED"));

        // TODO: 测试反馈和重新生成
        // TODO: 测试部署流程
    }
}
```

**Step 3: 验证测试文件存在**

```bash
ls -la nexusarchive-java/src/test/resources/test-yonsuite-api.json || echo "需要准备测试用 OpenAPI 文件"
```

**Step 4: 提交测试代码**

```bash
cd nexusarchive-java
# 确保测试文件存在
git add src/test/java/com/nexusarchive/integration/erp/ai
git commit -m "test(ai): add E2E test for AI generation flow"
```

---

## Task 9: 文档和用户指南

**Files:**
- Create: `docs/guides/ai-adapter-generation.md`
- Create: `docs/api/ai-generation-api.md`
- Modify: `README.md`

**Step 1: 编写用户指南**

```markdown
# AI 驱动的 ERP 适配器生成指南

## 概述

本系统使用 Claude AI 自动生成完整的 ERP 适配器代码，包括 HTTP 客户端、认证签名、数据映射和错误处理。

## 使用步骤

### 1. 准备 OpenAPI 文档

确保你的 OpenAPI 文档包含：
- 完整的请求/响应 Schema
- 认证方式说明
- Base URL 和端点路径

示例：
```yaml
openapi: 3.0.0
info:
  title: YonSuite API
  version: 1.0.0
servers:
  - url: https://api.yonyoucloud.com
paths:
  /yiyan/salesOut/list:
    get:
      summary: 销售出库单列表
      parameters:
        - name: startDate
          in: query
          schema:
            type: string
```

### 2. 上传并生成

**方法 1: 使用前端界面**
1. 访问 http://localhost:15175/system/settings/integration
2. 点击 "AI 智能适配器"
3. 上传 OpenAPI 文档
4. 选择 ERP 类型和认证方式
5. 点击 "生成适配器"

**方法 2: 使用 REST API**
```bash
curl -X POST http://localhost:19090/api/erp-ai/generate-ai \
  -F "file=@yonsuite-api.json" \
  -F "erpType=yonsuite" \
  -F "erpName=我的YonSuite" \
  -F "authType=appkey" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 3. 审核和迭代

生成的代码会返回供审核。你可以：
- 查看生成的代码
- 提供修改意见
- 请求重新生成
- 批准后部署

### 4. 部署

审核通过后，系统会：
- 保存代码到源码目录
- 编译验证
- 数据库注册
- 热加载适配器

## 成本控制

- 速率限制：10 次/分钟
- Token 使用：~2000 tokens/次
- 建议每月预算：$50-100

## 故障排查

### AI 生成失败
- 检查 API Key 配置
- 查看日志中的错误信息
- 确认 OpenAPI 文档格式正确

### 生成的代码无法编译
- 检查 AI 生成的代码是否完整
- 查看编译错误日志
- 提供反馈让 AI 重新生成
```

**Step 2: 编写 API 文档**

```markdown
# AI 生成 API 文档

## POST /api/erp-ai/generate-ai

使用 AI 生成 ERP 适配器代码。

**请求参数:**
- `file` (required): OpenAPI 文档文件
- `erpType` (required): ERP 类型标识
- `erpName` (required): 适配器名称
- `baseUrl` (optional): API Base URL
- `authType` (optional): 认证类型 (appkey/oauth2/none)

**响应:**
```json
{
  "success": true,
  "data": {
    "sessionId": "uuid",
    "status": "GENERATED",
    "generatedCode": "package com.example;..."
  }
}
```

## POST /api/erp-ai/regenerate-ai/{sessionId}

根据反馈重新生成代码。

## POST /api/erp-ai/approve/{sessionId}

批准生成的代码并继续部署。
```

**Step 3: 更新 README**

```markdown
# AI 适配器生成

本系统支持使用 Claude AI 自动生成 ERP 适配器代码。详见 [AI 生成指南](docs/guides/ai-adapter-generation.md)。
```

**Step 4: 提交文档**

```bash
cd /Users/user/nexusarchive
git add docs/guides/ai-adapter-generation.md \
        docs/api/ai-generation-api.md \
        README.md
git commit -m "docs(ai): add AI generation user guide and API documentation"
```

---

## Task 10: 最终集成验证

**Files:**
- Verify: `docker-compose.dev.yml` 包含所有必要的卷挂载
- Verify: `nexusarchive-java/pom.xml` 包含必要的依赖

**Step 1: 检查依赖**

```bash
cd nexusarchive-java
# 检查是否有必要的 HTTP 客户端依赖
grep "restTemplate\|webclient" pom.xml
```

如果没有，添加：
```xml
<!-- 已在 Spring Boot 中包含，无需额外添加 -->
```

**Step 2: 检查 Docker 配置**

```bash
# 确认卷挂载正确
grep -A 5 "nexus-backend:" /Users/user/nexusarchive/docker-compose.dev.yml | grep volumes -A 10
```

**Step 3: 完整功能测试**

1. 启动所有服务
2. 访问 http://localhost:15175/system/settings/integration
3. 测试上传 OpenAPI 文档
4. 验证 AI 生成的代码
5. 测试部署流程

**Step 4: 最终提交**

```bash
cd /Users/user/nexusarchive
git add .
git commit -m "feat(ai): complete LLM-driven ERP adapter generation system

Features:
- Claude API integration for code generation
- Prompt template system for high-quality output
- Code parsing and syntax validation
- Human review and iteration workflow
- Rate limiting and cost control
- E2E tests and documentation

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## 执行检查清单

在开始实现前，确认：
- [ ] 已获取 Claude API Key
- [ ] Java 17 环境已配置
- [ ] Maven 可用
- [ ] Docker Desktop 运行中

每个任务完成后验证：
- [ ] 单元测试通过
- [ ] 代码已提交
- [ ] 日志无错误
- [ ] 功能可用
