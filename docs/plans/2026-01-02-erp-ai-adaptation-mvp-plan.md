# ERP AI 自适应系统实施计划 (MVP)

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 构建一个 AI 驱动的 ERP 接口自动适配系统，用户上传 OpenAPI JSON 文件，系统自动生成并部署适配器代码。

**架构:** 采用 MCP Server + Agent Skills 混合架构。Agent 负责理解文档和生成代码，MCP Server 提供标准化的运行时接口。与现有的 ErpAdapterFactory 和 ErpAdapter 接口完全兼容。

**Tech Stack:** Java 17, Spring Boot 3.1.6, Claude API, LangChain, Swagger Parser, Lombok, JUnit 5, ArchUnit

**MVP 范围:** 仅支持 OpenAPI JSON 文件格式，仅处理记账凭证同步场景。

---

## Task 1: 创建项目基础结构

**目标:** 搭建 AI 适配模块的基础项目结构

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/agent/`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/parser/`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/generator/`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/mapper/`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/config/`
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/`
- Create: `nexusarchive-java/src/main/resources/ai-prompts/`

**Step 1: 创建目录结构**

Run:
```bash
cd nexusarchive-java
mkdir -p src/main/java/com/nexusarchive/integration/erp/ai/agent
mkdir -p src/main/java/com/nexusarchive/integration/erp/ai/parser
mkdir -p src/main/java/com/nexusarchive/integration/erp/ai/generator
mkdir -p src/main/java/com/nexusarchive/integration/erp/ai/mapper
mkdir -p src/main/java/com/nexusarchive/integration/erp/ai/config
mkdir -p src/test/java/com/nexusarchive/integration/erp/ai
mkdir -p src/main/resources/ai-prompts
```

**Step 2: 验证目录创建**

Run:
```bash
ls -la src/main/java/com/nexusarchive/integration/erp/ai/
```

Expected: 显示 agent, parser, generator, mapper, config 子目录

**Step 3: 创建包说明文件**

Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/README.md`

```markdown
# ERP AI 自适应模块

本模块负责 AI 驱动的 ERP 接口自动适配。

## 子模块

- `agent/` - Agent 编排和协调
- `parser/` - 接口文档解析器
- `generator/` - 代码生成器
- `mapper/` - 业务语义映射器
- `config/` - AI 配置和提示词模板

## MVP 范围

- 支持 OpenAPI JSON 格式
- 处理记账凭证同步场景
- 生成适配器 Java 代码
```

**Step 4: 提交基础结构**

Run:
```bash
git add src/main/java/com/nexusarchive/integration/erp/ai/ \
        src/test/java/com/nexusarchive/integration/erp/ai/ \
        src/main/resources/ai-prompts/
git commit -m "feat(erp-ai): 创建 AI 适配模块基础结构"
```

---

## Task 2: 实现 OpenAPI 文档解析器

**目标:** 解析 OpenAPI JSON 文件，提取 API 定义

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/parser/OpenApiDefinition.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/parser/OpenApiDocumentParser.java`
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/parser/OpenApiDocumentParserTest.java`
- Modify: `nexusarchive-java/pom.xml` (添加 swagger-parser 依赖)

**Step 1: 添加 Maven 依赖**

Modify: `nexusarchive-java/pom.xml`

在 `<dependencies>` 部分添加：

```xml
<!-- Swagger/OpenAPI Parser -->
<dependency>
    <groupId>io.swagger.parser.v3</groupId>
    <artifactId>swagger-parser</artifactId>
    <version>2.1.18</version>
</dependency>
```

**Step 2: 创建 API 定义 DTO**

Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/parser/OpenApiDefinition.java`

```java
package com.nexusarchive.integration.erp.ai.parser;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI 接口定义
 */
@Data
@Builder
public class OpenApiDefinition {

    /**
     * 接口路径
     */
    private String path;

    /**
     * HTTP 方法
     */
    private String method;

    /**
     * 操作 ID
     */
    private String operationId;

    /**
     * 接口摘要
     */
    private String summary;

    /**
     * 接口描述
     */
    private String description;

    /**
     * 请求参数
     */
    private List<ParameterDefinition> parameters;

    /**
     * 请求体定义
     */
    private RequestBodyDefinition requestBody;

    /**
     * 响应定义
     */
    private Map<String, ResponseDefinition> responses;

    /**
     * 标签
     */
    private List<String> tags;

    @Data
    @Builder
    public static class ParameterDefinition {
        private String name;
        private String in; // query, path, header, cookie
        private String description;
        private boolean required;
        private String type;
        private String format;
        private Object defaultValue;
    }

    @Data
    @Builder
    public static class RequestBodyDefinition {
        private String description;
        private boolean required;
        private MediaTypeSchema content;
    }

    @Data
    @Builder
    public static class MediaTypeSchema {
        private String schemaType;
        private Map<String, Object> properties;
        private String ref; // $schema reference
    }

    @Data
    @Builder
    public static class ResponseDefinition {
        private String description;
        private MediaTypeSchema content;
    }
}
```

**Step 3: 实现文档解析器**

Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/parser/OpenApiDocumentParser.java`

```java
package com.nexusarchive.integration.erp.ai.parser;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI 文档解析器
 * <p>
 * 解析 OpenAPI JSON/YAML 文件，提取接口定义
 * </p>
 */
@Slf4j
@Component
public class OpenApiDocumentParser {

    private final OpenAPIV3Parser parser = new OpenAPIV3Parser();

    /**
     * 解析上传的 OpenAPI 文件
     *
     * @param file 上传的文件
     * @return 解析结果
     * @throws IOException 读取文件失败
     */
    public ParseResult parse(MultipartFile file) throws IOException {
        // 1. 保存到临时文件
        Path tempFile = Files.createTempFile("openapi-", ".json");
        file.transferTo(tempFile.toFile());

        try {
            // 2. 解析 OpenAPI 文档
            SwaggerParseResult result = parser.readContents(tempFile.toString(), null);

            if (result.hasErrors()) {
                return ParseResult.failure(result.getMessages().toString());
            }

            OpenAPI openAPI = result.getOpenAPI();

            // 3. 提取所有接口定义
            List<OpenApiDefinition> definitions = extractDefinitions(openAPI);

            return ParseResult.success(definitions);

        } finally {
            // 4. 清理临时文件
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * 从 OpenAPI 对象提取接口定义
     */
    private List<OpenApiDefinition> extractDefinitions(OpenAPI openAPI) {
        List<OpenApiDefinition> definitions = new ArrayList<>();

        Map<String, PathItem> paths = openAPI.getPaths();
        if (paths == null) {
            return definitions;
        }

        for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
            String path = entry.getKey();
            PathItem pathItem = entry.getValue();

            // 遍历所有 HTTP 方法
            pathItem.readOperationsMap().forEach((method, operation) -> {
                OpenApiDefinition definition = convertToDefinition(path, method, operation);
                definitions.add(definition);
            });
        }

        return definitions;
    }

    /**
     * 转换为 OpenApiDefinition
     */
    private OpenApiDefinition convertToDefinition(String path,
                                                  PathItem.HttpMethod method,
                                                  Operation operation) {
        return OpenApiDefinition.builder()
            .path(path)
            .method(method.name())
            .operationId(operation.getOperationId())
            .summary(operation.getSummary())
            .description(operation.getDescription())
            .tags(operation.getTags() != null ? new ArrayList<>(operation.getTags()) : List.of())
            .build();
    }

    /**
     * 解析结果
     */
    @lombok.Data
    @lombok.Builder
    public static class ParseResult {
        private boolean success;
        private List<OpenApiDefinition> definitions;
        private String errorMessage;

        public static ParseResult success(List<OpenApiDefinition> definitions) {
            return ParseResult.builder()
                .success(true)
                .definitions(definitions)
                .build();
        }

        public static ParseResult failure(String errorMessage) {
            return ParseResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
        }
    }
}
```

**Step 4: 编写测试**

Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/parser/OpenApiDocumentParserTest.java`

```java
package com.nexusarchive.integration.erp.ai.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiDocumentParserTest {

    @Test
    void shouldParseValidOpenApiJson() throws IOException {
        // Given
        OpenApiDocumentParser parser = new OpenApiDocumentParser();

        String openApiJson = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Test API",
                "version": "1.0.0"
              },
              "paths": {
                "/api/vouchers": {
                  "get": {
                    "operationId": "listVouchers",
                    "summary": "获取凭证列表",
                    "tags": ["vouchers"]
                  }
                }
              }
            }
            """;

        Path tempFile = Files.createTempFile("test-", ".json");
        Files.writeString(tempFile, openApiJson);

        MultipartFile multipartFile = new MockMultipartFile(
            "openapi.json",
            "openapi.json",
            "application/json",
            Files.readAllBytes(tempFile)
        );

        // When
        var result = parser.parse(multipartFile);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getDefinitions());
        assertEquals(1, result.getDefinitions().size());
        assertEquals("listVouchers", result.getDefinitions().get(0).getOperationId());

        Files.deleteIfExists(tempFile);
    }

    @Test
    void shouldReturnFailureForInvalidJson() throws IOException {
        // Given
        OpenApiDocumentParser parser = new OpenApiDocumentParser();

        MultipartFile invalidFile = new MockMultipartFile(
            "invalid.json",
            "invalid.json",
            "application/json",
            "{ invalid json".getBytes()
        );

        // When
        var result = parser.parse(invalidFile);

        // Then
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }
}
```

**Step 5: 运行测试验证**

Run:
```bash
cd nexusarchive-java
mvn test -Dtest=OpenApiDocumentParserTest
```

Expected: 测试通过，显示 "Tests run: 2, Failures: 0"

**Step 6: 提交解析器实现**

Run:
```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/parser/ \
        nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/parser/ \
        nexusarchive-java/pom.xml
git commit -m "feat(erp-ai): 实现 OpenAPI 文档解析器

- 添加 swagger-parser 依赖
- 实现 OpenApiDocumentParser 解析 OpenAPI JSON
- 支持 MultipartFile 文件上传
- 提取接口定义、路径、方法、参数
- 单元测试验证解析功能"
```

---

## Task 3: 实现业务语义映射器

**目标:** 将 API 定义映射到系统的标准业务场景

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/mapper/ApiIntent.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/mapper/BusinessSemanticMapper.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/mapper/StandardScenario.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/mapper/ScenarioMapping.java`
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/mapper/BusinessSemanticMapperTest.java`

**Step 1: 定义 API 意图枚举**

Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/mapper/ApiIntent.java`

```java
package com.nexusarchive.integration.erp.ai.mapper;

import lombok.Builder;
import lombok.Data;

/**
 * API 接口意图
 */
@Data
@Builder
public class ApiIntent {

    /**
     * 操作类型
     */
    private OperationType operationType;

    /**
     * 业务对象
     */
    private BusinessObject businessObject;

    /**
     * 触发时机
     */
    private TriggerTiming triggerTiming;

    /**
     * 数据流向
     */
    private DataFlowDirection dataFlowDirection;

    public enum OperationType {
        QUERY,      // 查询
        SYNC,       // 同步
        SUBMIT,     // 提交
        CALLBACK,   // 回调
        NOTIFY      // 通知
    }

    public enum BusinessObject {
        ACCOUNTING_VOUCHER,  // 记账凭证
        INVOICE,             // 发票
        RECEIPT,             // 收据
        CONTRACT,            // 合同
        ATTACHMENT,          // 附件
        ACCOUNT_BALANCE,     // 科目余额
        UNKNOWN
    }

    public enum TriggerTiming {
        SCHEDULED,   // 定时触发
        EVENT_BASED, // 事件触发
        REALTIME,    // 实时查询
        ON_DEMAND    // 按需
    }

    public enum DataFlowDirection {
        ERP_TO_SYSTEM,  // ERP → 系统
        SYSTEM_TO_ERP,  // 系统 → ERP
        BIDIRECTIONAL   // 双向
    }
}
```

**Step 2: 定义标准场景**

Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/mapper/StandardScenario.java`

```java
package com.nexusarchive.integration.erp.ai.mapper;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 * 系统标准业务场景
 */
@Getter
public enum StandardScenario {

    VOUCHER_SYNC("voucherSync", "记账凭证同步",
                 ApiIntent.OperationType.SYNC,
                 ApiIntent.BusinessObject.ACCOUNTING_VOUCHER),

    INVOICE_SYNC("invoiceSync", "发票同步",
                 ApiIntent.OperationType.SYNC,
                 ApiIntent.BusinessObject.INVOICE),

    RECEIPT_SYNC("receiptSync", "收据同步",
                 ApiIntent.OperationType.SYNC,
                 ApiIntent.BusinessObject.RECEIPT),

    ATTACHMENT_SYNC("attachmentSync", "附件同步",
                     ApiIntent.OperationType.SYNC,
                     ApiIntent.BusinessObject.ATTACHMENT),

    VOUCHER_WEBHOOK("voucherWebhook", "凭证推送",
                     ApiIntent.OperationType.CALLBACK,
                     ApiIntent.BusinessObject.ACCOUNTING_VOUCHER),

    ARCHIVAL_FEEDBACK("archivalFeedback", "归档状态回写",
                       ApiIntent.OperationType.SUBMIT,
                       ApiIntent.BusinessObject.ACCOUNTING_VOUCHER),

    ACCOUNT_QUERY("accountQuery", "科目查询",
                   ApiIntent.OperationType.QUERY,
                   ApiIntent.BusinessObject.ACCOUNT_BALANCE),

    UNKNOWN("unknown", "未知场景",
            ApiIntent.OperationType.QUERY,
            ApiIntent.BusinessObject.UNKNOWN);

    private final String code;
    private final String description;
    private final ApiIntent.OperationType operationType;
    private final ApiIntent.BusinessObject businessObject;

    StandardScenario(String code, String description,
                    ApiIntent.OperationType operationType,
                    ApiIntent.BusinessObject businessObject) {
        this.code = code;
        this.description = description;
        this.operationType = operationType;
        this.businessObject = businessObject;
    }

    /**
     * 根据 API 意图匹配标准场景
     */
    public static StandardScenario fromIntent(ApiIntent intent) {
        // 简单的规则匹配（MVP 版本）

        for (StandardScenario scenario : values()) {
            if (scenario == UNKNOWN) continue;

            if (scenario.operationType == intent.operationType()
                && scenario.businessObject == intent.businessObject()) {
                return scenario;
            }
        }

        return UNKNOWN;
    }
}
```

**Step 3: 实现语义映射器**

Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/mapper/BusinessSemanticMapper.java`

```java
package com.nexusarchive.integration.erp.ai.mapper;

import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import com.nexusarchive.integration.erp.ai.prompt.PromptTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 业务语义映射器
 * <p>
 * 核心 AI 组件：理解接口的业务意图，映射到系统标准场景
 * </p>
 */
@Slf4j
@Component
public class BusinessSemanticMapper {

    /**
     * 将 API 定义映射到标准场景
     *
     * @param definition API 定义
     * @return 场景映射结果
     */
    public ScenarioMapping mapToScenario(OpenApiDefinition definition) {
        // Step 1: 理解接口意图（AI）
        ApiIntent intent = understandIntent(definition);

        // Step 2: 匹配标准场景
        StandardScenario scenario = StandardScenario.fromIntent(intent);

        // Step 3: 生成映射配置
        MappingConfig config = generateMappingConfig(definition, scenario);

        log.info("API {} → 场景 {}", definition.getOperationId(), scenario.getCode());

        return ScenarioMapping.builder()
            .apiDefinition(definition)
            .intent(intent)
            .scenario(scenario)
            .config(config)
            .build();
    }

    /**
     * 理解接口意图（使用 LLM）
     * MVP 版本：使用基于规则的分析
     * 未来版本：调用 Claude API 进行智能分析
     */
    private ApiIntent understandIntent(OpenApiDefinition definition) {
        // MVP: 使用基于规则的关键词匹配

        String path = definition.getPath().toLowerCase();
        String operationId = definition.getOperationId().toLowerCase();
        String summary = definition.getSummary() != null ? definition.getSummary().toLowerCase() : "";
        String tags = String.join(" ", definition.getTags()).toLowerCase();

        // 识别操作类型
        ApiIntent.OperationType operationType = detectOperationType(operationId, summary);

        // 识别业务对象
        ApiIntent.BusinessObject businessObject = detectBusinessObject(path, operationId, summary, tags);

        // 识别触发时机
        ApiIntent.TriggerTiming triggerTiming = detectTriggerTiming(definition.getParameters());

        // 识别数据流向
        ApiIntent.DataFlowDirection dataFlow = ApiIntent.DataFlowDirection.ERP_TO_SYSTEM;

        return ApiIntent.builder()
            .operationType(operationType)
            .businessObject(businessObject)
            .triggerTiming(triggerTiming)
            .dataFlowDirection(dataFlow)
            .build();
    }

    private ApiIntent.OperationType detectOperationType(String operationId, String summary) {
        if (operationId.contains("list") || operationId.contains("query") || operationId.contains("get")) {
            return ApiIntent.OperationType.QUERY;
        } else if (operationId.contains("sync") || summary.contains("同步")) {
            return ApiIntent.OperationType.SYNC;
        } else if (operationId.contains("submit") || operationId.contains("create")) {
            return ApiIntent.OperationType.SUBMIT;
        } else if (operationId.contains("webhook") || operationId.contains("callback")) {
            return ApiIntent.OperationType.CALLBACK;
        }
        return ApiIntent.OperationType.QUERY; // 默认
    }

    private ApiIntent.BusinessObject detectBusinessObject(String path, String operationId,
                                                           String summary, String tags) {
        String combined = (path + " " + operationId + " " + summary + " " + tags).toLowerCase();

        if (combined.contains("voucher") || combined.contains("凭证")) {
            return ApiIntent.BusinessObject.ACCOUNTING_VOUCHER;
        } else if (combined.contains("invoice") || combined.contains("发票")) {
            return ApiIntent.BusinessObject.INVOICE;
        } else if (combined.contains("receipt") || combined.contains("收据")) {
            return ApiIntent.BusinessObject.RECEIPT;
        } else if (combined.contains("contract") || combined.contains("合同")) {
            return ApiIntent.BusinessObject.CONTRACT;
        } else if (combined.contains("attachment") || combined.contains("附件")) {
            return ApiIntent.BusinessObject.ATTACHMENT;
        } else if (combined.contains("account") || combined.contains("科目") || combined.contains("余额")) {
            return ApiIntent.BusinessObject.ACCOUNT_BALANCE;
        }
        return ApiIntent.BusinessObject.UNKNOWN;
    }

    private ApiIntent.TriggerTiming detectTriggerTiming(java.util.List<OpenApiDefinition.ParameterDefinition> parameters) {
        boolean hasDateRange = parameters.stream()
            .anyMatch(p -> p.getName().toLowerCase().contains("date") || p.getName().toLowerCase().contains("time"));

        return hasDateRange ? ApiIntent.TriggerTiming.SCHEDULED : ApiIntent.TriggerTiming.ON_DEMAND;
    }

    /**
     * 生成映射配置
     */
    private MappingConfig generateMappingConfig(OpenApiDefinition definition,
                                                 StandardScenario scenario) {
        return MappingConfig.builder()
            .scenarioCode(scenario.getCode())
            .sourcePath(definition.getPath())
            .httpMethod(definition.getMethod())
            .needsDataMapping(true)
            .needsFileDownload(false) // MVP 暂不支持
            .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class ScenarioMapping {
        private OpenApiDefinition apiDefinition;
        private ApiIntent intent;
        private StandardScenario scenario;
        private MappingConfig config;
    }

    @lombok.Data
    @lombok.Builder
    public static class MappingConfig {
        private String scenarioCode;
        private String sourcePath;
        private String httpMethod;
        private boolean needsDataMapping;
        private boolean needsFileDownload;
    }
}
```

**Step 4: 编写测试**

Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/mapper/BusinessSemanticMapperTest.java`

```java
package com.nexusarchive.integration.erp.ai.mapper;

import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BusinessSemanticMapperTest {

    private BusinessSemanticMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new BusinessSemanticMapper();
    }

    @Test
    void shouldMapVoucherListApiToVoucherSync() {
        // Given
        OpenApiDefinition definition = OpenApiDefinition.builder()
            .path("/api/v1/vouchers")
            .method("get")
            .operationId("listVouchers")
            .summary("获取凭证列表")
            .tags(List.of("vouchers"))
            .build();

        // When
        var mapping = mapper.mapToScenario(definition);

        // Then
        assertEquals(StandardScenario.VOUCHER_SYNC, mapping.getScenario());
        assertEquals(ApiIntent.OperationType.QUERY, mapping.getIntent().getOperationType());
        assertEquals(ApiIntent.BusinessObject.ACCOUNTING_VOUCHER, mapping.getIntent().getBusinessObject());
    }

    @Test
    void shouldMapInvoiceApiToInvoiceSync() {
        // Given
        OpenApiDefinition definition = OpenApiDefinition.builder()
            .path("/api/v1/invoices")
            .method("get")
            .operationId("syncInvoices")
            .summary("同步发票数据")
            .tags(List.of("invoice"))
            .build();

        // When
        var mapping = mapper.mapToScenario(definition);

        // Then
        assertEquals(StandardScenario.INVOICE_SYNC, mapping.getScenario());
        assertEquals(ApiIntent.BusinessObject.INVOICE, mapping.getIntent().getBusinessObject());
    }
}
```

**Step 5: 运行测试验证**

Run:
```bash
cd nexusarchive-java
mvn test -Dtest=BusinessSemanticMapperTest
```

Expected: 测试通过

**Step 6: 提交映射器实现**

Run:
```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/mapper/
git commit -m "feat(erp-ai): 实现业务语义映射器

- 添加 ApiIntent、StandardScenario 枚举
- 实现 BusinessSemanticMapper 映射业务意图
- 支持凭证、发票等场景识别
- MVP 使用规则匹配，未来可扩展为 LLM 分析"
```

---

## Task 4: 实现代码生成器

**目标:** 根据场景映射生成适配器 Java 代码

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/generator/ErpAdapterCodeGenerator.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/generator/GeneratedCode.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/generator/CodeTemplate.java`
- Create: `nexusarchive-java/src/main/resources/ai-prompts/adapter-template.java`
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/generator/CodeGeneratorTest.java`

**Step 1: 定义生成代码结果**

Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/generator/GeneratedCode.java`

```java
package com.nexusarchive.integration.erp.ai.generator;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 生成的代码
 */
@Data
@Builder
public class GeneratedCode {

    /**
     * 适配器主类
     */
    private String adapterClass;

    /**
     * 类名
     */
    private String className;

    /**
     * 包名
     */
    private String packageName;

    /**
     * DTO 类列表
     */
    private List<DtoClass> dtoClasses;

    /**
     * 单元测试类
     */
    private String testClass;

    /**
     * 集成配置 SQL
     */
    private String sqlConfig;

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    public static class DtoClass {
        private String className;
        private String code;
    }
}
```

**Step 2: 实现代码生成器**

Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/generator/ErpAdapterCodeGenerator.java`

```java
package com.nexusarchive.integration.erp.ai.generator;

import com.nexusarchive.integration.erp.ai.mapper.BusinessSemanticMapper;
import com.nexusarchive.integration.erp.ai.mapper.ScenarioMapping;
import com.nexusarchive.integration.erp.ai.mapper.StandardScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ERP 适配器代码生成器
 * <p>
 * 根据场景映射生成适配器 Java 代码
 * </p>
 */
@Slf4j
@Component
public class ErpAdapterCodeGenerator {

    /**
     * 生成完整的适配器代码
     *
     * @param mappings 场景映射列表
     * @param erpType ERP 类型标识
     * @param erpName ERP 名称
     * @return 生成的代码
     */
    public GeneratedCode generate(List<ScenarioMapping> mappings,
                                  String erpType,
                                  String erpName) {
        // 生成类名（标准化）
        String className = generateClassName(erpType);
        String packageName = "com.nexusarchive.integration.erp.adapter.impl";

        // 生成主适配器类
        String adapterClass = generateAdapterClass(mappings, className, packageName, erpName);

        // 生成 DTO 类
        List<GeneratedCode.DtoClass> dtoClasses = generateDtoClasses(mappings, packageName);

        // 生成测试类
        String testClass = generateTestClass(className, packageName);

        return GeneratedCode.builder()
            .adapterClass(adapterClass)
            .className(className)
            .packageName(packageName)
            .dtoClasses(dtoClasses)
            .testClass(testClass)
            .build();
    }

    /**
     * 生成主适配器类
     */
    private String generateAdapterClass(List<ScenarioMapping> mappings,
                                         String className,
                                         String packageName,
                                         String erpName) {
        StringBuilder code = new StringBuilder();

        // 包声明
        code.append("package ").append(packageName).append(";\n\n");

        // 导入
        code.append(generateImports());

        // 类注解
        code.append(generateClassAnnotation(className, erpName, mappings));

        // 类声明
        code.append("@Component\n");
        code.append("@Slf4j\n");
        code.append("public class ").append(className).append(" implements ErpAdapter {\n\n");

        // 字段
        code.append(generateFields());

        // 基本方法
        code.append(generateBasicMethods(className, erpName));

        // 业务方法（根据映射生成）
        code.append(generateBusinessMethods(mappings));

        // 辅助方法
        code.append(generateHelperMethods(mappings));

        code.append("}\n");

        return code.toString();
    }

    private String generateImports() {
        return """
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.dto.*;
import com.nexusarchive.integration.erp.dto.ConnectionTestResult;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import com.nexusarchive.entity.ErpConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

""";
    }

    private String generateClassAnnotation(String className, String erpName, List<ScenarioMapping> mappings) {
        // 提取支持的场景
        List<String> scenarios = mappings.stream()
            .map(m -> m.getScenario().getCode())
            .distinct()
            .toList();

        String scenariosArray = scenarios.stream()
            .map(s -> "\"" + s + "\"")
            .reduce((a, b) -> a + ", " + b)
            .orElse("");

        return """
@ErpAdapter(
    identifier = "<IDENTIFIER>",
    name = "<NAME>",
    description = "<DESCRIPTION>",
    version = "1.0.0",
    erpType = "<ERPTYPE>",
    supportedScenarios = {<SCENARIOS>}
)
""".replace("<IDENTIFIER>", className.replace("ErpAdapter", "").toLowerCase())
    .replace("<NAME>", erpName)
    .replace("<DESCRIPTION>", erpName + " ERP 集成适配器")
    .replace("<ERPTYPE>", className.replace("ErpAdapter", "").toUpperCase())
    .replace("<SCENARIOS>", scenariosArray);
    }

    private String generateFields() {
        return """
    @Autowired
    private RestTemplate restTemplate;

""";
    }

    private String generateBasicMethods(String className, String erpName) {
        return """
    @Override
    public String getIdentifier() {
        return "<IDENTIFIER>";
    }

    @Override
    public String getName() {
        return "<NAME>";
    }

    @Override
    public String getDescription() {
        return "<NAME> ERP 集成适配器";
    }

    @Override
    public ConnectionTestResult testConnection(ErpConfig config) {
        try {
            // TODO: 实现连接测试
            return ConnectionTestResult.success("连接成功");
        } catch (Exception e) {
            return ConnectionTestResult.failure("连接失败: " + e.getMessage());
        }
    }

""".replace("<IDENTIFIER>", className.replace("ErpAdapter", "").toLowerCase())
    .replace("<NAME>", erpName);
    }

    private String generateBusinessMethods(List<ScenarioMapping> mappings) {
        StringBuilder code = new StringBuilder();

        for (ScenarioMapping mapping : mappings) {
            StandardScenario scenario = mapping.getScenario();

            switch (scenario) {
                case VOUCHER_SYNC:
                    code.append(generateVoucherSyncMethod(mapping));
                    break;
                case INVOICE_SYNC:
                    code.append(generateInvoiceSyncMethod(mapping));
                    break;
                // TODO: 添加其他场景
                default:
                    log.debug("暂不支持生成场景: {}", scenario);
            }
        }

        return code.toString();
    }

    private String generateVoucherSyncMethod(ScenarioMapping mapping) {
        return """
    @Override
    public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        String url = config.getBaseUrl() + "<PATH>";

        // TODO: 调用 ERP 接口
        // SomeErpVoucherResponse response = restTemplate.getForObject(
        //     url + "?startDate={start}&endDate={end}",
        //     SomeErpVoucherResponse.class,
        //     startDate, endDate
        // );

        // TODO: 数据映射
        // return response.getData().stream()
        //     .map(this::mapToVoucherDTO)
        //     .collect(Collectors.toList());

        log.info("同步凭证: startDate={}, endDate={}", startDate, endDate);
        return new ArrayList<>(); // 返回空列表作为占位符
    }

    private VoucherDTO mapToVoucherDTO(Object item) {
        // TODO: 实现 ERP 对象到 VoucherDTO 的映射
        return VoucherDTO.builder().build();
    }

""".replace("<PATH>", mapping.getConfig().getSourcePath());
    }

    private String generateInvoiceSyncMethod(ScenarioMapping mapping) {
        return """
    @Override
    public List<InvoiceDTO> syncInvoices(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        // TODO: 实现发票同步逻辑
        log.info("同步发票: startDate={}, endDate={}", startDate, endDate);
        return new ArrayList<>();
    }

    @Override
    public List<ErpScenario> getAvailableScenarios() {
        List<ErpScenario> scenarios = new ArrayList<>();
        // TODO: 返回支持的场景列表
        return scenarios;
    }

""";
    }

    private String generateHelperMethods(List<ScenarioMapping> mappings) {
        return """
    // TODO: 添加辅助方法
    // - 数据转换工具
    // - 错误处理
    // - 日志记录
""";
    }

    /**
     * 生成类名
     */
    private String generateClassName(String erpType) {
        // 转换为 PascalCase
        String normalized = erpType.toLowerCase().replaceAll("[^a-z0-9]", "");
        return normalized.substring(0, 1).toUpperCase() +
               (normalized.length() > 1 ? normalized.substring(1) : "") +
               "ErpAdapter";
    }

    /**
     * 生成 DTO 类
     */
    private List<GeneratedCode.DtoClass> generateDtoClasses(List<ScenarioMapping> mappings,
                                                           String packageName) {
        // MVP: 只生成基础 DTO，实际应该根据响应结构生成
        List<GeneratedCode.DtoClass> dtoClasses = new ArrayList<>();

        // 生成占位符 DTO
        String placeholderDto = """
package <PACKAGE>;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PlaceholderResponse {
    private String message;
}
""".replace("<PACKAGE>", packageName);

        dtoClasses.add(GeneratedCode.DtoClass.builder()
            .className("PlaceholderResponse")
            .code(placeholderDto)
            .build());

        return dtoClasses;
    }

    /**
     * 生成测试类
     */
    private String generateTestClass(String className, String packageName) {
        String testClassName = className + "Test";
        String testPackage = packageName.replace(".adapter.impl", ".adapter");

        return """
package <PACKAGE>;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.entity.ErpConfig;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class <TEST_CLASS_NAME> {

    @Autowired
    private ErpAdapterFactory factory;

    @Test
    void shouldSyncVouchersSuccessfully() {
        // Given
        ErpConfig config = createTestConfig();
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        // When
        var adapter = factory.getAdapter("<IDENTIFIER>");
        List<VoucherDTO> vouchers = adapter.syncVouchers(config, start, end);

        // Then
        assertNotNull(vouchers);
        // TODO: 添加更多断言
    }

    private ErpConfig createTestConfig() {
        ErpConfig config = new ErpConfig();
        // TODO: 配置测试参数
        return config;
    }
}
""".replace("<PACKAGE>", testPackage)
  .replace("<TEST_CLASS_NAME>", testClassName)
  .replace("<IDENTIFIER>", className.replace("ErpAdapter", "").toLowerCase());
    }
}
```

**Step 3: 编写测试**

Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/generator/CodeGeneratorTest.java`

```java
package com.nexusarchive.integration.erp.ai.generator;

import com.nexusarchive.integration.erp.ai.mapper.BusinessSemanticMapper;
import com.nexusarchive.integration.erp.ai.mapper.ScenarioMapping;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodeGeneratorTest {

    private ErpAdapterCodeGenerator generator;
    private BusinessSemanticMapper mapper;

    @BeforeEach
    void setUp() {
        generator = new ErpAdapterCodeGenerator();
        mapper = new BusinessSemanticMapper();
    }

    @Test
    void shouldGenerateAdapterClass() {
        // Given
        OpenApiDefinition definition = OpenApiDefinition.builder()
            .path("/api/v1/vouchers")
            .method("get")
            .operationId("listVouchers")
            .summary("获取凭证列表")
            .tags(List.of("vouchers"))
            .build();

        ScenarioMapping mapping = mapper.mapToScenario(definition);

        // When
        GeneratedCode code = generator.generate(
            List.of(mapping),
            "testerp",
            "测试ERP"
        );

        // Then
        assertNotNull(code);
        assertNotNull(code.getAdapterClass());
        assertTrue(code.getAdapterClass().contains("public class TesterpErpAdapter"));
        assertTrue(code.getAdapterClass().contains("syncVouchers"));
        assertEquals("TesterpErpAdapter", code.getClassName());
    }

    @Test
    void shouldGenerateTestClass() {
        // Given
        OpenApiDefinition definition = OpenApiDefinition.builder()
            .path("/api/v1/vouchers")
            .operationId("listVouchers")
            .build();

        ScenarioMapping mapping = mapper.mapToScenario(definition);

        // When
        GeneratedCode code = generator.generate(
            List.of(mapping),
            "myerp",
            "MyERP"
        );

        // Then
        assertNotNull(code.getTestClass());
        assertTrue(code.getTestClass().contains("MyerpErpAdapterTest"));
    }
}
```

**Step 4: 运行测试验证**

Run:
```bash
cd nexusarchive-java
mvn test -Dtest=CodeGeneratorTest
```

Expected: 测试通过

**Step 5: 提交代码生成器**

Run:
```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/generator/
git commit -m "feat(erp-ai): 实现代码生成器

- 添加 ErpAdapterCodeGenerator 生成适配器类
- 支持根据场景映射生成 Java 代码
- 生成基本方法、业务方法、测试类
- MVP 版本生成框架代码，TODO 标记待完善部分"
```

---

## Task 5: 实现 Agent 编排器

**目标:** 编排整个 AI 适配流程（解析→映射→生成）

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/agent/ErpAdaptationOrchestrator.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/agent/AdaptationResult.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/agent/AdaptationRequest.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/controller/ErpAdaptationController.java`
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/agent/OrchestratorTest.java`

**Step 1: 定义请求和结果**

Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/agent/AdaptationRequest.java`

```java
package com.nexusarchive.integration.erp.ai.agent;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * ERP 适配请求
 */
@Data
@Builder
public class AdaptationRequest {

    /**
     * ERP 类型标识（用于生成类名）
     */
    private String erpType;

    /**
     * ERP 名称
     */
    private String erpName;

    /**
     * ERP 描述
     */
    private String description;

    /**
     * 接口文档文件
     */
    private MultipartFile[] apiFiles;

    /**
     * 联系人邮箱（用于通知）
     */
    private String contactEmail;
}
```

Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/agent/AdaptationResult.java`

```java
package com.nexusarchive.integration.erp.ai.agent;

import com.nexusarchive.integration.erp.ai.generator.GeneratedCode;
import com.nexusarchive.integration.erp.ai.mapper.ScenarioMapping;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * ERP 适配结果
 */
@Data
@Builder
public class AdaptationResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 生成的代码
     */
    private GeneratedCode generatedCode;

    /**
     * 场景映射列表
     */
    private List<ScenarioMapping> mappings;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;

    /**
     * 警告信息
     */
    private List<String> warnings;

    /**
     * 适配器 ID
     */
    private String adapterId;

    public static AdaptationResult success(GeneratedCode code,
                                           List<ScenarioMapping> mappings,
                                           String adapterId) {
        return AdaptationResult.builder()
            .success(true)
            .generatedCode(code)
            .mappings(mappings)
            .adapterId(adapterId)
            .build();
    }

    public static AdaptationResult failure(String errorMessage) {
        return AdaptationResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}
```

**Step 2: 实现编排器**

Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/agent/ErpAdaptationOrchestrator.java`

```java
package com.nexusarchive.integration.erp.ai.agent;

import com.nexusarchive.integration.erp.ai.generator.ErpAdapterCodeGenerator;
import com.nexusarchive.integration.erp.ai.generator.GeneratedCode;
import com.nexusarchive.integration.erp.ai.mapper.BusinessSemanticMapper;
import com.nexusarchive.integration.erp.ai.mapper.ScenarioMapping;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDocumentParser;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDocumentParser.ParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ERP 适配编排器
 * <p>
 * 编排整个 AI 适配流程：
 * 1. 解析文档
 * 2. 业务语义映射
 * 3. 代码生成
 * </p>
 */
@Slf4j
@Service
public class ErpAdaptationOrchestrator {

    private final OpenApiDocumentParser parser;
    private final BusinessSemanticMapper mapper;
    private final ErpAdapterCodeGenerator codeGenerator;

    public ErpAdaptationOrchestrator(OpenApiDocumentParser parser,
                                     BusinessSemanticMapper mapper,
                                     ErpAdapterCodeGenerator codeGenerator) {
        this.parser = parser;
        this.mapper = mapper;
        this.codeGenerator = codeGenerator;
    }

    /**
     * 执行完整的适配流程
     *
     * @param request 适配请求
     * @return 适配结果
     */
    public AdaptationResult adapt(AdaptationRequest request) {
        try {
            log.info("开始 ERP 适配: erpType={}, erpName={}",
                request.getErpType(), request.getErpName());

            // Step 1: 解析接口文档
            log.info("Step 1: 解析接口文档");
            List<OpenApiDefinition> definitions = parseApiDocuments(request.getApiFiles());

            // Step 2: 业务语义映射
            log.info("Step 2: 业务语义映射 ({} 个接口)", definitions.size());
            List<ScenarioMapping> mappings = mapToScenarios(definitions);

            // Step 3: 生成代码
            log.info("Step 3: 生成适配器代码");
            GeneratedCode code = codeGenerator.generate(
                mappings,
                request.getErpType(),
                request.getErpName()
            );

            // Step 4: 构建结果
            String adapterId = request.getErpType().toLowerCase();

            log.info("ERP 适配完成: adapterId={}, scenarios={}",
                adapterId, mappings.size());

            return AdaptationResult.success(code, mappings, adapterId);

        } catch (Exception e) {
            log.error("ERP 适配失败", e);
            return AdaptationResult.failure("适配失败: " + e.getMessage());
        }
    }

    /**
     * 解析接口文档
     */
    private List<OpenApiDefinition> parseApiDocuments(MultipartFile[] files) {
        List<OpenApiDefinition> allDefinitions = new ArrayList<>();

        for (MultipartFile file : files) {
            ParseResult result = parser.parse(file);

            if (!result.isSuccess()) {
                log.warn("文件解析失败: {}, 错误: {}", file.getOriginalFilename(),
                    result.getErrorMessage());
                continue;
            }

            allDefinitions.addAll(result.getDefinitions());
        }

        if (allDefinitions.isEmpty()) {
            throw new RuntimeException("未能解析出任何接口定义");
        }

        return allDefinitions;
    }

    /**
     * 业务语义映射
     */
    private List<ScenarioMapping> mapToScenarios(List<OpenApiDefinition> definitions) {
        List<ScenarioMapping> mappings = new ArrayList<>();

        for (OpenApiDefinition definition : definitions) {
            ScenarioMapping mapping = mapper.mapToScenario(definition);

            // 只保留已识别的场景
            if (mapping.getScenario() !=
                com.nexusarchive.integration.erp.ai.mapper.StandardScenario.UNKNOWN) {
                mappings.add(mapping);
            }
        }

        return mappings;
    }
}
```

**Step 3: 创建 REST API**

Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/controller/ErpAdaptationController.java`

```java
package com.nexusarchive.integration.erp.ai.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.integration.erp.ai.agent.AdaptationRequest;
import com.nexusarchive.integration.erp.ai.agent.AdaptationResult;
import com.nexusarchive.integration.erp.ai.agent.ErpAdaptationOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * ERP 适配 API
 * <p>
 * 提供 AI 驱动的 ERP 接口自动适配功能
 * </p>
 */
@Tag(name = "ERP AI 适配", description = "AI 驱动的 ERP 接口自动适配")
@RestController
@RequestMapping("/api/erp-ai")
@RequiredArgsConstructor
public class ErpAdaptationController {

    private final ErpAdaptationOrchestrator orchestrator;

    /**
     * 上传接口文件并生成适配器
     */
    @Operation(summary = "上传接口文件", description = "上传 ERP 接口文档，AI 自动生成适配器代码")
    @PostMapping("/adapt")
    public Result<AdaptationResult> adaptErp(@ModelAttribute AdaptationRequest request) {
        AdaptationResult result = orchestrator.adapt(request);

        if (result.isSuccess()) {
            return Result.success(result);
        } else {
            return Result.error(result.getErrorMessage());
        }
    }

    /**
     * 预览生成的代码
     */
    @Operation(summary = "预览代码", description = "预览生成的适配器代码")
    @GetMapping("/preview/{adapterId}")
    public Result<String> previewCode(@PathVariable String adapterId) {
        // TODO: 实现代码预览
        return Result.success("代码预览功能待实现");
    }

    /**
     * 部署适配器
     */
    @Operation(summary = "部署适配器", description = "确认并部署生成的适配器")
    @PostMapping("/deploy/{adapterId}")
    public Result<String> deployAdapter(@PathVariable String adapterId) {
        // TODO: 实现自动部署
        return Result.success("部署功能待实现");
    }
}
```

**Step 4: 编写测试**

Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/agent/OrchestratorTest.java`

```java
package com.nexusarchive.integration.erp.ai.agent;

import com.nexusarchive.integration.erp.ai.parser.OpenApiDocumentParser;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrchestratorTest {

    @Test
    void shouldOrchestrateFullAdaptationFlow() throws Exception {
        // Given
        var parser = new OpenApiDocumentParser();
        var mapper = new com.nexusarchive.integration.erp.ai.mapper.BusinessSemanticMapper();
        var generator = new com.nexusarchive.integration.erp.ai.generator.ErpAdapterCodeGenerator();
        var orchestrator = new ErpAdaptationOrchestrator(parser, mapper, generator);

        // 准备测试文件
        String openApiJson = """
            {
              "openapi": "3.0.0",
              "info": {"title": "Test ERP", "version": "1.0"},
              "paths": {
                "/api/vouchers": {
                  "get": {
                    "operationId": "listVouchers",
                    "summary": "获取凭证列表",
                    "tags": ["vouchers"]
                  }
                }
              }
            }
            """;

        Path tempFile = Files.createTempFile("test-", ".json");
        Files.writeString(tempFile, openApiJson);

        MultipartFile file = new MockMultipartFile(
            "openapi.json",
            "openapi.json",
            "application/json",
            Files.readAllBytes(tempFile)
        );

        AdaptationRequest request = AdaptationRequest.builder()
            .erpType("testerp")
            .erpName("测试ERP")
            .description("测试")
            .apiFiles(new MultipartFile[]{file})
            .build();

        // When
        AdaptationResult result = orchestrator.adapt(request);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getGeneratedCode());
        assertNotNull(result.getMappings());
        assertEquals("testerp", result.getAdapterId());

        Files.deleteIfExists(tempFile);
    }
}
```

**Step 5: 运行测试验证**

Run:
```bash
cd nexusarchive-java
mvn test -Dtest=OrchestratorTest
```

Expected: 测试通过

**Step 6: 提交编排器实现**

Run:
```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/agent/ \
        nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/controller/
git commit -m "feat(erp-ai): 实现 Agent 编排器和 REST API

- 添加 ErpAdaptationOrchestrator 编排适配流程
- 创建 AdaptationRequest/Result DTO
- 实现 ErpAdaptationController REST API
- 支持 POST /api/erp-ai/adapt 上传文件并生成适配器
- 集成文档解析、语义映射、代码生成三大模块"
```

---

## Task 6: 实现代码预览和确认功能

**目标:** 用户可以预览生成的代码并确认后部署

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/service/CodePreviewService.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/controller/ErpAdaptationController.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/store/AdaptationSessionStore.java`

**Step 1: 创建会话存储**

Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/store/AdaptationSessionStore.java`

```java
package com.nexusarchive.integration.erp.ai.store;

import com.nexusarchive.integration.erp.ai.agent.AdaptationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 适配会话存储
 * <p>
 * 存储生成的代码，供预览和确认
 * </p>
 */
@Slf4j
@Component
public class AdaptationSessionStore {

    private final Map<String, AdaptationSession> sessions = new ConcurrentHashMap<>();

    /**
     * 保存适配结果
     */
    public String save(AdaptationResult result) {
        String sessionId = UUID.randomUUID().toString();

        AdaptationSession session = AdaptationSession.builder()
            .sessionId(sessionId)
            .result(result)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(24)) // 24小时过期
            .build();

        sessions.put(sessionId, session);

        log.info("保存适配会话: sessionId={}, adapterId={}",
            sessionId, result.getAdapterId());

        return sessionId;
    }

    /**
     * 获取会话
     */
    public AdaptationSession get(String sessionId) {
        AdaptationSession session = sessions.get(sessionId);

        if (session == null) {
            return null;
        }

        // 检查是否过期
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            sessions.remove(sessionId);
            return null;
        }

        return session;
    }

    /**
     * 删除会话
     */
    public void delete(String sessionId) {
        sessions.remove(sessionId);
        log.info("删除适配会话: sessionId={}", sessionId);
    }

    @lombok.Data
    @lombok.Builder
    public static class AdaptationSession {
        private String sessionId;
        private AdaptationResult result;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private boolean confirmed;
    }
}
```

**Step 2: 实现代码预览服务**

Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/service/CodePreviewService.java`

```java
package com.nexusarchive.integration.erp.ai.service;

import com.nexusarchive.integration.erp.ai.agent.AdaptationResult;
import com.nexusarchive.integration.erp.ai.store.AdaptationSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 代码预览服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodePreviewService {

    private final AdaptationSessionStore sessionStore;

    /**
     * 保存生成的代码到会话
     */
    public String saveForPreview(AdaptationResult result) {
        return sessionStore.save(result);
    }

    /**
     * 获取代码预览
     */
    public CodePreview getCodePreview(String sessionId) {
        var session = sessionStore.get(sessionId);

        if (session == null) {
            return CodePreview.error("会话不存在或已过期");
        }

        AdaptationResult result = session.getResult();

        return CodePreview.builder()
            .success(true)
            .sessionId(sessionId)
            .adapterId(result.getAdapterId())
            .adapterCode(result.getGeneratedCode().getAdapterClass())
            .dtoCodes(result.getGeneratedCode().getDtoClasses())
            .testCode(result.getGeneratedCode().getTestClass())
            .scenarioCount(result.getMappings().size())
            .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class CodePreview {
        private boolean success;
        private String error;
        private String sessionId;
        private String adapterId;
        private String adapterCode;
        private java.util.List<com.nexusarchive.integration.erp.ai.generator.GeneratedCode.DtoClass> dtoCodes;
        private String testCode;
        private int scenarioCount;

        public static CodePreview error(String error) {
            return CodePreview.builder()
                .success(false)
                .error(error)
                .build();
        }
    }
}
```

**Step 3: 更新控制器集成预览功能**

Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/controller/ErpAdaptationController.java`

```java
package com.nexusarchive.integration.erp.ai.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.integration.erp.ai.agent.AdaptationRequest;
import com.nexusarchive.integration.erp.ai.agent.AdaptationResult;
import com.nexusarchive.integration.erp.ai.agent.ErpAdaptationOrchestrator;
import com.nexusarchive.integration.erp.ai.service.CodePreviewService;
import com.nexusarchive.integration.erp.ai.service.CodePreviewService.CodePreview;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * ERP 适配 API
 */
@Tag(name = "ERP AI 适配", description = "AI 驱动的 ERP 接口自动适配")
@RestController
@RequestMapping("/api/erp-ai")
@RequiredArgsConstructor
public class ErpAdaptationController {

    private final ErpAdaptationOrchestrator orchestrator;
    private final CodePreviewService previewService;

    @Operation(summary = "上传接口文件", description = "上传 ERP 接口文档，AI 自动生成适配器代码")
    @PostMapping("/adapt")
    public Result<String> adaptErp(@ModelAttribute AdaptationRequest request) {
        AdaptationResult result = orchestrator.adapt(request);

        if (!result.isSuccess()) {
            return Result.error(result.getErrorMessage());
        }

        // 保存到会话供预览
        String sessionId = previewService.saveForPreview(result);

        return Result.success("适配成功，sessionId=" + sessionId);
    }

    @Operation(summary = "预览代码", description = "预览生成的适配器代码")
    @GetMapping("/preview/{sessionId}")
    public Result<CodePreview> previewCode(@PathVariable String sessionId) {
        CodePreview preview = previewService.getCodePreview(sessionId);

        if (preview.isSuccess()) {
            return Result.success(preview);
        } else {
            return Result.error(preview.getError());
        }
    }

    @Operation(summary = "确认并部署", description = "确认生成的代码并部署到系统")
    @PostMapping("/deploy/{sessionId}")
    public Result<String> deployAdapter(@PathVariable String sessionId) {
        // TODO: 实现自动部署
        // 1. 编译代码
        // 2. 写入文件系统
        // 3. 触发热加载
        // 4. 更新数据库配置

        return Result.success("部署功能待实现 (MVP 阶段)");
    }
}
```

**Step 4: 提交预览功能**

Run:
```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/
git commit -m "feat(erp-ai): 实现代码预览和确认功能

- 添加 AdaptationSessionStore 存储适配会话
- 实现 CodePreviewService 代码预览服务
- 更新 ErpAdaptationController 添加预览和部署端点
- 支持 GET /api/erp-ai/preview/{sessionId} 预览生成的代码"
```

---

## Task 7: 添加集成测试

**目标:** 端到端验证整个 AI 适配流程

**Files:**
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/ErpAiAdaptationIntegrationTest.java`

**Step 1: 编写集成测试**

Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/ErpAiAdaptationIntegrationTest.java`

```java
package com.nexusarchive.integration.erp.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.integration.erp.ai.agent.AdaptationRequest;
import com.nexusarchive.integration.erp.ai.controller.ErpAdaptationController;
import com.nexusarchive.integration.erp.ai.service.CodePreviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ERP AI 适配集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
class ErpAiAdaptationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @Test
    void shouldCompleteFullAdaptationFlow() throws Exception {
        // Given: 准备 OpenAPI JSON 文件
        String openApiJson = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "MyERP API",
                "version": "1.0.0",
                "description": "MyERP 系统接口"
              },
              "paths": {
                "/api/v1/vouchers": {
                  "get": {
                    "operationId": "listVouchers",
                    "summary": "获取凭证列表",
                    "description": "根据日期范围查询记账凭证",
                    "tags": ["vouchers"],
                    "parameters": [
                      {
                        "name": "startDate",
                        "in": "query",
                        "required": true,
                        "schema": {"type": "string", "format": "date"}
                      },
                      {
                        "name": "endDate",
                        "in": "query",
                        "required": true,
                        "schema": {"type": "string", "format": "date"}
                      }
                    ],
                    "responses": {
                      "200": {
                        "description": "成功",
                        "content": {
                          "application/json": {
                            "schema": {
                              "type": "array",
                              "items": {"$ref": "#/components/schemas/Voucher"}
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

        Path apiFile = tempDir.resolve("openapi.json");
        Files.writeString(apiFile, openApiJson);

        MockMultipartFile file = new MockMultipartFile(
            "files",
            "openapi.json",
            "application/json",
            Files.readAllBytes(apiFile)
        );

        // When & Then: 调用适配 API
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/erp-ai/adapt")
                .file("files", file)
                .param("erpType", "myerp")
                .param("erpName", "MyERP系统")
                .param("description", "MyERP 接口文档")
                .param("contactEmail", "test@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").exists());

        // TODO: 测试预览 API
        // TODO: 测试部署 API
    }
}
```

**Step 2: 运行集成测试**

Run:
```bash
cd nexusarchive-java
mvn test -Dtest=ErpAiAdaptationIntegrationTest
```

Expected: 集成测试通过

**Step 3: 提交集成测试**

Run:
```bash
git add nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/
git commit -m "test(erp-ai): 添加端到端集成测试

- 测试完整的文件上传 → 解析 → 映射 → 生成流程
- 验证 REST API 端点
- 使用 MockMvc 模拟 HTTP 请求"
```

---

## Task 8: 更新模块清单和文档

**目标:** 在模块清单中记录新的 AI 适配模块

**Files:**
- Modify: `docs/architecture/module-manifest.md`
- Create: `docs/architecture/erp-ai-adaptation-guide.md`

**Step 1: 更新模块清单**

Modify: `docs/architecture/module-manifest.md`

在 `## Backend Modules` 部分的末尾添加：

```markdown
### AI 驱动模块 (v2.5)

| 模块 ID | 名称 | 范围 | 职责一句话 | 允许依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
| BE.AI.PARSER | 文档解析器 | `integration.erp.ai.parser` | 解析 OpenAPI 文档 | 通用库 | ✅ 活跃 v2.5 |
| BE.AI.MAPPER | 语义映射器 | `integration.erp.ai.mapper` | 业务意图识别和场景映射 | `BE.AI.PARSER` | ✅ 活跃 v2.5 |
| BE.AI.GENERATOR | 代码生成器 | `integration.erp.ai.generator` | 生成适配器 Java 代码 | `BE.AI.PARSER`, `BE.AI.MAPPER` | ✅ 活跃 v2.5 |
| BE.AI.AGENT | AI 编排器 | `integration.erp.ai.agent` | 编排适配流程 | Spring MVC, `BE.AI.*` | ✅ 活跃 v2.5 |
| BE.AI.API | AI 适配 API | `integration.erp.ai.controller` | REST API 端点 | Spring Web | ✅ 活跃 v2.5 |
```

同时更新版本号：

```markdown
> **版本**: 2.5.0
> **更新日期**: 2026-01-02
```

添加更新日志：

```markdown
- **2026-01-02 v2.5.0**: 新增 ERP AI 自适应模块，支持 OpenAPI 文档自动生成适配器
```

**Step 2: 创建使用指南**

Create: `docs/architecture/erp-ai-adaptation-guide.md`

```markdown
# ERP AI 自适应系统使用指南

## 概述

ERP AI 自适应系统允许你通过上传 ERP 接口文档，自动生成适配器代码，大大降低新 ERP 接入成本。

## MVP 能力

- ✅ 支持 OpenAPI JSON/YAML 格式
- ✅ 识别记账凭证同步场景
- ✅ 自动生成适配器 Java 代码
- ✅ 生成单元测试框架
- ✅ 代码预览和确认

## 使用流程

### 1. 准备接口文档

从 ERP 厂商获取接口文档，推荐格式：
- **最佳**: OpenAPI 3.0 规范的 JSON/YAML 文件
- **次选**: 包含清晰接口定义的 Markdown 文档

### 2. 上传文档

```bash
curl -X POST http://localhost:19090/api/erp-ai/adapt \\
  -F "files=@openapi.json" \\
  -F "erpType=myerp" \\
  -F "erpName=MyERP系统" \\
  -F "description=MyERP 接口文档"
```

### 3. 预览生成的代码

系统会返回 `sessionId`，用于预览代码：

```bash
curl http://localhost:19090/api/erp-ai/preview/{sessionId}
```

返回包含：
- 适配器主类代码
- DTO 类代码
- 测试类代码
- 识别的场景列表

### 4. 确认并部署

MVP 阶段：人工审核代码后手动部署

未来版本：支持一键自动部署

## 生成的适配器

适配器会自动注册到 `ErpAdapterFactory`，可以像使用其他适配器一样使用：

```java
@Autowired
private ErpAdapterFactory factory;

var adapter = factory.getAdapter("myerp");
List<VoucherDTO> vouchers = adapter.syncVouchers(config, start, end);
```

## 限制和注意事项

1. **文档格式**: MVP 仅支持 OpenAPI JSON，其他格式待实现
2. **场景支持**: MVP 仅支持记账凭证同步，原始凭证待扩展
3. **代码质量**: 生成的代码需要人工审核和测试
4. **部署方式**: MVP 需要手动编译部署，自动化待实现

## 故障排查

### 问题：无法解析接口文档

**原因**: 文档格式不符合 OpenAPI 规范

**解决**:
1. 使用 Swagger Editor 验证文档格式
2. 确保 `openapi` 版本为 3.0
3. 检查 `paths` 和 `operations` 定义完整

### 问题：无法识别业务场景

**原因**: 接口命名或描述不符合预期模式

**解决**:
1. 确保接口名称包含关键词（voucher, invoice, 等）
2. 检查 `summary` 或 `description` 描述清晰
3. 使用 `tags` 标注业务模块

### 问题：生成的代码无法编译

**原因**: 生成的代码可能有语法错误

**解决**:
1. 检查生成的代码
2. 修复 TODO 标记的部分
3. 运行 `mvn compile` 验证
```

**Step 3: 提交文档更新**

Run:
```bash
git add docs/architecture/module-manifest.md \
        docs/architecture/erp-ai-adaptation-guide.md
git commit -m "docs(erp-ai): 更新模块清单和使用指南

- 添加 AI 驱动模块到 v2.5.0
- 记录 5 个新模块：PARSER, MAPPER, GENERATOR, AGENT, API
- 创建 ERP AI 自适应系统使用指南
- 说明 MVP 能力和使用流程"
```

---

## Task 9: 运行完整测试套件

**目标:** 验证所有组件正常工作

**Step 1: 运行所有新增测试**

Run:
```bash
cd nexusarchive-java
mvn test -Dtest=*Ai*
```

Expected: 所有 AI 相关测试通过

**Step 2: 运行架构测试**

Run:
```bash
mvn test -Dtest=ArchitectureTest
```

Expected: 架构测试通过（无违规）

**Step 3: 编译验证**

Run:
```bash
mvn clean compile
```

Expected: 编译成功，无错误

**Step 4: 提交验证标记**

Run:
```bash
git commit --allow-empty -m "test(erp-ai): MVP 完整测试验证通过

- 单元测试: 全部通过
- 集成测试: 通过
- 架构测试: 通过
- 编译: 成功
- MVP 阶段完成"
```

---

## Task 10: 推送到远程仓库

**目标:** 将 MVP 代码推送到远程分支

**Step 1: 确认当前分支**

Run:
```bash
git branch
```

Expected: 显示 `* feature/erp-modularization-refactor`

**Step 2: 推送到远程**

Run:
```bash
git push origin feature/erp-modularization-refactor
```

Expected: 推送成功

**Step 3: 创建 Pull Request**

Run:
```bash
gh pr create \\
  --title "feat(erp-ai): 实现 ERP AI 自适应系统 (MVP)" \\
  --body "## 概述

实现 AI 驱动的 ERP 接口自动适配系统，用户上传 OpenAPI 文档即可自动生成适配器代码。

## 主要变更

### 核心功能
- ✅ OpenAPI 文档解析器
- ✅ 业务语义智能映射器
- ✅ 适配器代码生成器
- ✅ Agent 编排器
- ✅ REST API (/api/erp-ai/*)
- ✅ 代码预览和确认

### 新增模块
- `integration.erp.ai.parser` - 文档解析
- `integration.erp.ai.mapper` - 语义映射
- `integration.erp.ai.generator` - 代码生成
- `integration.erp.ai.agent` - 编排器
- `integration.erp.ai.controller` - REST API

### MVP 范围
- 支持 OpenAPI JSON/YAML 格式
- 处理记账凭证同步场景
- 生成适配器框架代码（TODO 标记待完善部分）

## 测试

- [x] 单元测试全部通过
- [x] 集成测试通过
- [x] 架构测试通过
- [x] 编译验证成功

## 后续工作

MVP 完成后，可扩展：
1. 支持更多文件格式（PDF、Markdown）
2. 扩展到原始凭证场景
3. 实现自动部署功能
4. 添加学习优化机制

## 相关文档

- 设计文档: docs/plans/2026-01-02-erp-ai-adaptation-design.md
- 使用指南: docs/architecture/erp-ai-adaptation-guide.md
- 模块清单: docs/architecture/module-manifest.md (v2.5.0)
"
```

---

## 完成检查清单

- [ ] Task 1: 创建项目基础结构
- [ ] Task 2: 实现 OpenAPI 文档解析器
- [ ] Task 3: 实现业务语义映射器
- [ ] Task 4: 实现代码生成器
- [ ] Task 5: 实现 Agent 编排器
- [ ] Task 6: 实现代码预览和确认功能
- [ ] Task 7: 添加集成测试
- [ ] Task 8: 更新模块清单和文档
- [ ] Task 9: 运行完整测试套件
- [ ] Task 10: 推送到远程仓库

---

## 实施说明

### 开发环境

- Java 17
- Spring Boot 3.1.6
- Maven 3.x
- IDE: IntelliJ IDEA (推荐)

### 依赖添加

所有新依赖已添加到 `pom.xml`：
- swagger-parser: 2.1.18

### 代码规范

- 遵循现有代码风格
- 使用 Lombok 简化代码
- TODO 标记标识待实现功能
- 日志使用 Slf4j

### 测试策略

- 单元测试：每个组件独立测试
- 集成测试：端到端流程测试
- TDD 原则：先写测试，再实现功能

### 提交规范

- 每个任务完成后提交
- Commit message 格式：`type(scope): description`
- Type: feat, test, docs, refactor, fix

---

## 参考

- 设计文档: `docs/plans/2026-01-02-erp-ai-adaptation-design.md`
- 现有适配器: `integration/erp/adapter/`
- 架构测试: `ArchitectureTest.java`
- 模块清单: `docs/architecture/module-manifest.md`
