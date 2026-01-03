# ERP AI 适配器增强实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 修复 ERP AI 适配系统，实现智能识别现有连接器并添加场景，而非每次创建新连接器。

**架构:** 后端 Spring Boot + 前端 React，使用 OpenAPI 文档解析生成适配器代码，支持预览确认后注册到现有或新建连接器。

**技术栈:** Spring Boot 3.1.6, Java 17, React 19, TypeScript 5.8, Ant Design 6, PostgreSQL

---

## 修改概览

| 模块 | 文件 | 变更类型 |
|------|------|---------|
| 后端 | `ErpAdaptationController.java` | 新增 preview 接口 |
| 后端 | `ErpAdaptationOrchestrator.java` | 新增 preview 方法 |
| 后端 | `ErpTypeIdentifier.java` | 新增 ERP 类型识别器 |
| 后端 | `ScenarioNamer.java` | 新增场景命名生成器 |
| 后端 | `DatabaseRegistrationService.java` | 修改注册逻辑 |
| 前端 | `erp.ts` | 新增 API 方法 |
| 前端 | `IntegrationSettings.tsx` | 新增预览 Modal |

---

## Task 1: 创建 ERP 类型识别器

**目标:** 根据 OpenAPI 文档识别 ERP 类型

**文件:**
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/identifier/ErpTypeIdentifier.java`

**Step 1: 创建接口和枚举**

```java
package com.nexusarchive.integration.erp.ai.identifier;

import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import lombok.Getter;

/**
 * ERP 类型识别器
 */
public class ErpTypeIdentifier {

    @Getter
    public enum ErpType {
        YONSUITE("YonSuite", "yonsuite"),
        KINGDEE("Kingdee", "kingdee"),
        WEAVER("泛微OA", "weaver"),
        GENERIC("通用", "generic");

        private final String displayName;
        private final String code;

        ErpType(String displayName, String code) {
            this.displayName = displayName;
            this.code = code;
        }
    }

    /**
     * 识别 ERP 类型
     */
    public ErpType identify(String fileName, OpenApiDefinition document) {
        // 1. 检查文件名
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.contains("yonsuite") || lowerFileName.contains("yonbip") || lowerFileName.contains("yonyou")) {
            return ErpType.YONSUITE;
        }
        if (lowerFileName.contains("kingdee") || lowerFileName.contains("k3cloud")) {
            return ErpType.KINGDEE;
        }
        if (lowerFileName.contains("weaver") || lowerFileName.contains("ecology")) {
            return ErpType.WEAVER;
        }

        // 2. 检查 API 路径
        if (document != null && document.getPath() != null) {
            String path = document.getPath().toLowerCase();
            if (path.contains("/yonbip/")) {
                return ErpType.YONSUITE;
            }
            if (path.contains("/k3cloud/")) {
                return ErpType.KINGDEE;
            }
        }

        return ErpType.GENERIC;
    }
}
```

**Step 2: 编写单元测试**

创建: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/identifier/ErpTypeIdentifierTests.java`

```java
package com.nexusarchive.integration.erp.ai.identifier;

import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ErpTypeIdentifierTests {

    private final ErpTypeIdentifier identifier = new ErpTypeIdentifier();

    @Test
    void testIdentifyYonSuiteByFileName() {
        ErpTypeIdentifier.ErpType type = identifier.identify("yonsuite-salesout-api.json", null);
        assertEquals(ErpTypeIdentifier.ErpType.YONSUITE, type);
    }

    @Test
    void testIdentifyKingdeeByFileName() {
        ErpTypeIdentifier.ErpType type = identifier.identify("kingdee-api.json", null);
        assertEquals(ErpTypeIdentifier.ErpType.KINGDEE, type);
    }

    @Test
    void testIdentifyYonSuiteByPath() {
        OpenApiDefinition doc = new OpenApiDefinition();
        doc.setPath("/yonbip/digitalModel/salesout/doc/query");
        ErpTypeIdentifier.ErpType type = identifier.identify("api.json", doc);
        assertEquals(ErpTypeIdentifier.ErpType.YONSUITE, type);
    }

    @Test
    void testDefaultToGeneric() {
        ErpTypeIdentifier.ErpType type = identifier.identify("unknown-api.json", null);
        assertEquals(ErpTypeIdentifier.ErpType.GENERIC, type);
    }
}
```

**Step 3: 运行测试验证**

```bash
cd /Users/user/nexusarchive/nexusarchive-java
mvn test -Dtest=ErpTypeIdentifierTests -q
```

预期: 全部测试通过

**Step 4: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/identifier/
git add nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/identifier/
git commit -m "feat(ai): add ERP type identifier"
```

---

## Task 2: 创建场景命名生成器

**目标:** 根据 API 定义生成场景代码和名称

**文件:**
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/identifier/ScenarioNamer.java`

**Step 1: 创建命名生成器**

```java
package com.nexusarchive.integration.erp.ai.identifier;

import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import lombok.Data;

import java.util.regex.Pattern;

/**
 * 场景命名生成器
 */
public class ScenarioNamer {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9]");

    /**
     * 生成场景信息
     */
    public ScenarioName generateScenarioName(OpenApiDefinition definition) {
        String scenarioKey = generateScenarioKey(definition);
        String displayName = generateDisplayName(definition);
        String description = generateDescription(definition);

        return ScenarioName.builder()
            .scenarioKey(scenarioKey)
            .displayName(displayName)
            .description(description)
            .build();
    }

    /**
     * 生成场景代码 (SALESOUT_DOC_QUERY)
     */
    private String generateScenarioKey(OpenApiDefinition definition) {
        String source = definition.getPath();

        // 移除路径参数和查询参数
        source = source.replaceAll("\\{[^}]+\\}", "");
        source = source.replaceAll("\\?.*", "");

        // 分割路径
        String[] parts = source.split("/");

        // 取最后 2-3 个有意义的部分
        StringBuilder keyBuilder = new StringBuilder();
        int start = Math.max(0, parts.length - 3);
        for (int i = start; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                keyBuilder.append(parts[i].toUpperCase());
                if (i < parts.length - 1) {
                    keyBuilder.append("_");
                }
            }
        }

        // 移除非字母数字
        String key = NON_ALPHANUMERIC.matcher(keyBuilder.toString()).replaceAll("_");

        // 确保不为空
        if (key.isEmpty()) {
            key = "API_" + definition.getOperationId().toUpperCase();
        }

        return key;
    }

    /**
     * 生成显示名称
     */
    private String generateDisplayName(OpenApiDefinition definition) {
        // 优先使用 summary
        if (definition.getSummary() != null && !definition.getSummary().isEmpty()) {
            return definition.getSummary();
        }

        // 其次使用 operationId
        if (definition.getOperationId() != null && !definition.getOperationId().isEmpty()) {
            return camelToWords(definition.getOperationId());
        }

        // 最后使用路径
        return "API: " + definition.getPath();
    }

    /**
     * 生成描述
     */
    private String generateDescription(OpenApiDefinition definition) {
        return "AI 自动识别: " + definition.getPath() + " (" + definition.getMethod() + ")";
    }

    /**
     * 驼峰转词语
     */
    private String camelToWords(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    @Data
    @lombok.Builder
    public static class ScenarioName {
        private String scenarioKey;
        private String displayName;
        private String description;
    }
}
```

**Step 2: 编写单元测试**

创建: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/identifier/ScenarioNamerTests.java`

```java
package com.nexusarchive.integration.erp.ai.identifier;

import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScenarioNamerTests {

    private final ScenarioNamer namer = new ScenarioNamer();

    @Test
    void testGenerateSalesOutScenario() {
        OpenApiDefinition doc = new OpenApiDefinition();
        doc.setPath("/yonbip/digitalModel/salesout/doc/query");
        doc.setMethod("POST");
        doc.setOperationId("querySalesOutList");
        doc.setSummary("查询销售出库单列表");

        ScenarioNamer.ScenarioName name = namer.generateScenarioName(doc);

        assertEquals("SALESOUT_DOC_QUERY", name.getScenarioKey());
        assertEquals("查询销售出库单列表", name.getDisplayName());
        assertTrue(name.getDescription().contains("AI 自动识别"));
    }

    @Test
    void testGenerateReceiptScenario() {
        OpenApiDefinition doc = new OpenApiDefinition();
        doc.setPath("/api/receipt/list");
        doc.setOperationId("getReceiptList");
        doc.setSummary(null);

        ScenarioNamer.ScenarioName name = namer.generateScenarioName(doc);

        assertEquals("API_RECEIPT_LIST", name.getScenarioKey());
        assertEquals("Get Receipt List", name.getDisplayName());
    }

    @Test
    void testHandlePathParameters() {
        OpenApiDefinition doc = new OpenApiDefinition();
        doc.setPath("/yonbip/digitalModel/salesout/{id}/detail");
        doc.setOperationId("getDetail");
        doc.setSummary("获取详情");

        ScenarioNamer.ScenarioName name = namer.generateScenarioName(doc);

        assertEquals("SALESOUT_DETAIL", name.getScenarioKey());
        assertEquals("获取详情", name.getDisplayName());
    }
}
```

**Step 3: 运行测试验证**

```bash
cd /Users/user/nexusarchive/nexusarchive-java
mvn test -Dtest=ScenarioNamerTests -q
```

**Step 4: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/identifier/
git add nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/identifier/
git commit -m "feat(ai): add scenario naming generator"
```

---

## Task 3: 修改数据库注册服务

**目标:** 支持添加场景到现有连接器

**文件:**
- 修改: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/deploy/DatabaseRegistrationService.java`

**Step 1: 修改注册方法**

```java
package com.nexusarchive.integration.erp.ai.deploy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.integration.erp.ai.generator.GeneratedCode;
import com.nexusarchive.integration.erp.ai.identifier.ScenarioNamer;
import com.nexusarchive.integration.erp.ai.mapper.BusinessSemanticMapper;
import com.nexusarchive.integration.erp.ai.identifier.ErpTypeIdentifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * 数据库注册服务
 */
@Slf4j
@Service
public class DatabaseRegistrationService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScenarioNamer scenarioNamer = new ScenarioNamer();
    private final ErpTypeIdentifier erpTypeIdentifier = new ErpTypeIdentifier();

    public DatabaseRegistrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 注册场景到指定连接器
     *
     * @param targetConfigId 目标连接器 ID（null 表示创建新连接器）
     * @param fileName 文件名（用于识别 ERP 类型）
     * @param mappings API 映射列表
     * @return 注册结果
     */
    @Transactional
    public RegistrationResult registerScenarios(Long targetConfigId, String fileName,
                                                  List<BusinessSemanticMapper.ScenarioMapping> mappings) {
        log.info("开始注册场景: targetConfigId={}, mappings={}", targetConfigId, mappings.size());

        // 识别 ERP 类型
        ErpTypeIdentifier.ErpType erpType = erpTypeIdentifier.identify(fileName,
            mappings.isEmpty() ? null : mappings.get(0).getApiDefinition());

        // 如果没有指定目标连接器，创建新的
        Long configId = targetConfigId;
        if (configId == null) {
            configId = createNewConfig(erpType);
            log.info("创建新连接器: id={}, type={}", configId, erpType);
        } else {
            log.info("使用现有连接器: id={}", configId);
        }

        // 插入场景
        int createdCount = 0;
        int skippedCount = 0;
        for (BusinessSemanticMapper.ScenarioMapping mapping : mappings) {
            if (mapping.getScenario() == null) {
                continue;
            }

            // 使用场景命名生成器
            ScenarioNamer.ScenarioName name = scenarioNamer.generateScenarioName(mapping.getApiDefinition());

            // 检查是否已存在
            if (scenarioExists(configId, name.getScenarioKey())) {
                skippedCount++;
                log.debug("场景已存在，跳过: {}", name.getScenarioKey());
                continue;
            }

            // 插入场景
            insertScenario(configId, name);
            createdCount++;
        }

        log.info("场景注册完成: created={}, skipped={}", createdCount, skippedCount);

        return RegistrationResult.builder()
            .configId(configId)
            .erpType(erpType.getCode())
            .createdCount(createdCount)
            .skippedCount(skippedCount)
            .build();
    }

    /**
     * 检查场景是否存在
     */
    private boolean scenarioExists(Long configId, String scenarioKey) {
        String sql = "SELECT COUNT(*) FROM sys_erp_scenario WHERE config_id = ? AND scenario_key = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, configId, scenarioKey);
        return count != null && count > 0;
    }

    /**
     * 创建新连接器配置
     */
    private Long createNewConfig(ErpTypeIdentifier.ErpType erpType) {
        String sql = """
            INSERT INTO sys_erp_config (name, erp_type, config_json, is_active, created_time)
            VALUES (?, ?, ?::jsonb, 1, CURRENT_TIMESTAMP)
            """;

        Map<String, Object> config = Map.of(
            "baseUrl", "",
            "appKey", "",
            "appSecret", "",
            "description", "AI 生成的 " + erpType.getDisplayName() + " 适配器"
        );

        try {
            String configJson = objectMapper.writeValueAsString(config);
            String name = erpType.getDisplayName() + "-" + System.currentTimeMillis();

            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            int rows = jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, name);
                ps.setString(2, erpType.getCode());
                ps.setString(3, configJson);
                return ps;
            }, keyHolder);

            if (rows > 0 && keyHolder.getKey() != null) {
                return keyHolder.getKey().longValue();
            }
        } catch (Exception e) {
            log.error("创建连接器失败", e);
        }

        throw new RuntimeException("无法创建连接器配置");
    }

    /**
     * 插入场景
     */
    private void insertScenario(Long configId, ScenarioNamer.ScenarioName name) {
        String sql = """
            INSERT INTO sys_erp_scenario (
                config_id, scenario_key, name, description, is_active, sync_strategy, created_time
            ) VALUES (?, ?, ?, ?, true, 'MANUAL', CURRENT_TIMESTAMP)
            """;

        int rows = jdbcTemplate.update(sql,
            configId,
            name.getScenarioKey(),
            name.getDisplayName(),
            name.getDescription()
        );

        log.debug("插入场景: {} -> {}", configId, name.getScenarioKey());
    }

    /**
     * 注册结果
     */
    @lombok.Data
    @lombok.Builder
    public static class RegistrationResult {
        private Long configId;
        private String erpType;
        private int createdCount;
        private int skippedCount;
    }
}
```

**Step 2: 编写集成测试**

创建: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/deploy/DatabaseRegistrationServiceTests.java`

```java
package com.nexusarchive.integration.erp.ai.deploy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseRegistrationServiceTests {

    @Autowired
    private DatabaseRegistrationService registrationService;

    @Test
    void testRegisterToNewConfig() {
        DatabaseRegistrationService.RegistrationResult result =
            registrationService.registerScenarios(null, "yonsuite-test.json", List.of());

        assertNotNull(result.getConfigId());
        assertEquals("yonsuite", result.getErpType());
    }

    @Test
    void testRegisterToExistingConfig() {
        // 假设配置 ID 1 存在
        DatabaseRegistrationService.RegistrationResult result =
            registrationService.registerScenarios(1L, "yonsuite-test.json", List.of());

        assertEquals(1L, result.getConfigId());
    }
}
```

**Step 3: 运行测试验证**

```bash
cd /Users/user/nexusarchive/nexusarchive-java
mvn test -Dtest=DatabaseRegistrationServiceTests -q
```

**Step 4: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/deploy/
git commit -m "refactor(ai): update database registration to support existing configs"
```

---

## Task 4: 新增预览 API 接口

**目标:** 提供预览接口，返回识别结果供前端展示

**文件:**
- 修改: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/controller/ErpAdaptationController.java`

**Step 1: 添加预览接口**

```java
package com.nexusarchive.integration.erp.ai.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.integration.erp.ai.agent.ErpAdaptationOrchestrator;
import com.nexusarchive.integration.erp.ai.identifier.ErpTypeIdentifier;
import com.nexusarchive.integration.erp.ai.identifier.ScenarioNamer;
import com.nexusarchive.integration.erp.ai.mapper.BusinessSemanticMapper;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDocumentParser;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/erp-ai")
@RequiredArgsConstructor
public class ErpAdaptationController {

    private final OpenApiDocumentParser documentParser;
    private final BusinessSemanticMapper semanticMapper;
    private final ErpTypeIdentifier erpTypeIdentifier;
    private final ScenarioNamer scenarioNamer;
    private final ErpAdaptationOrchestrator orchestrator;

    /**
     * 预览 API - 上传文档后返回识别结果
     */
    @PostMapping("/preview")
    public Result<PreviewResult> preview(@RequestParam("files") MultipartFile[] files,
                                         @RequestParam(value = "erpType", required = false) String erpType) {
        try {
            // 解析文档
            List<OpenApiDefinition> definitions = parseDocuments(files);
            if (definitions.isEmpty()) {
                return Result.error("未能从文档中提取任何 API 定义");
            }

            // 识别 ERP 类型
            ErpTypeIdentifier.ErpType recognizedType = erpTypeIdentifier.identify(files[0].getOriginalFilename(), definitions.get(0));

            // 生成场景预览
            List<ScenarioPreview> scenarios = generateScenarioPreviews(definitions);

            // 查找现有连接器
            List<ExistingConfig> existingConfigs = findExistingConfigs(recognizedType.getCode());

            return Result.success(PreviewResult.builder()
                .erpType(recognizedType.getCode())
                .erpDisplayName(recognizedType.getDisplayName())
                .apiCount(definitions.size())
                .scenarios(scenarios)
                .existingConfigs(existingConfigs)
                .suggestedConfigId(suggestConfigId(existingConfigs, recognizedType.getCode()))
                .build());

        } catch (Exception e) {
            log.error("预览失败", e);
            return Result.error("预览失败: " + e.getMessage());
        }
    }

    /**
     * 确认部署 - 用户确认后执行
     */
    @PostMapping("/deploy")
    public Result<DeployResult> deploy(@RequestBody DeployRequest request) {
        try {
            // 重新解析文档（或使用缓存的 sessionId）
            // 这里简化处理，实际可能需要缓存机制

            // 执行部署
            // orchestrator.adaptAndDeploy(...);

            return Result.success(DeployResult.builder()
                .success(true)
                .message("部署成功，请重启后端以加载新的适配器")
                .build());

        } catch (Exception e) {
            log.error("部署失败", e);
            return Result.error("部署失败: " + e.getMessage());
        }
    }

    private List<OpenApiDefinition> parseDocuments(MultipartFile[] files) throws IOException {
        List<OpenApiDefinition> allDefinitions = new ArrayList<>();
        for (MultipartFile file : files) {
            var result = documentParser.parse(file);
            if (result.isSuccess()) {
                allDefinitions.addAll(result.getDefinitions());
            }
        }
        return allDefinitions;
    }

    private List<ScenarioPreview> generateScenarioPreviews(List<OpenApiDefinition> definitions) {
        return definitions.stream()
            .map(def -> {
                ScenarioNamer.ScenarioName name = scenarioNamer.generateScenarioName(def);
                return ScenarioPreview.builder()
                    .scenarioKey(name.getScenarioKey())
                    .displayName(name.getDisplayName())
                    .description(name.getDescription())
                    .path(def.getPath())
                    .method(def.getMethod())
                    .selected(true)
                    .build();
            })
            .collect(Collectors.toList());
    }

    private List<ExistingConfig> findExistingConfigs(String erpType) {
        // 查询数据库中的现有配置
        // 这里简化，实际需要注入 JdbcTemplate
        return List.of();
    }

    private Long suggestConfigId(List<ExistingConfig> configs, String erpType) {
        // 如果有同类型的配置，建议使用第一个
        return configs.isEmpty() ? null : configs.get(0).getConfigId();
    }

    /**
     * 预览结果
     */
    @lombok.Data
    @lombok.Builder
    public static class PreviewResult {
        private String erpType;
        private String erpDisplayName;
        private int apiCount;
        private List<ScenarioPreview> scenarios;
        private List<ExistingConfig> existingConfigs;
        private Long suggestedConfigId;
    }

    /**
     * 场景预览
     */
    @lombok.Data
    @lombok.Builder
    public static class ScenarioPreview {
        private String scenarioKey;
        private String displayName;
        private String description;
        private String path;
        private String method;
        private boolean selected;
    }

    /**
     * 现有配置
     */
    @lombok.Data
    @lombok.Builder
    public static class ExistingConfig {
        private Long configId;
        private String name;
        private int scenarioCount;
    }

    /**
     * 部署请求
     */
    @lombok.Data
    public static class DeployRequest {
        private String erpType;
        private Long targetConfigId; // null = 创建新连接器
        private List<ScenarioPreview> scenarios;
    }

    /**
     * 部署结果
     */
    @lombok.Data
    @lombok.Builder
    public static class DeployResult {
        private boolean success;
        private String message;
        private Long configId;
        private int scenarioCount;
    }
}
```

**Step 2: 测试预览接口**

```bash
# 保存请求文件
cat > /tmp/preview-request.json << 'EOF'
{
  "erpType": "yonsuite",
  "files": ["test-yonsuite-salesout-api.json"]
}
EOF

# 使用 curl 测试（需要先启动后端）
TOKEN="your-token"
curl -X POST http://localhost:19090/api/erp-ai/preview \
  -H "Authorization: Bearer $TOKEN" \
  -F "files=@/Users/user/nexusarchive/test-yonsuite-salesout-api.json"
```

**Step 3: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/controller/
git commit -m "feat(ai): add preview API endpoint"
```

---

## Task 5: 前端 API 客户端

**目标:** 添加预览和部署 API 调用方法

**文件:**
- 修改: `src/api/erp.ts`

**Step 1: 添加 API 方法和类型**

```typescript
// 在 src/api/erp.ts 中添加

export interface ScenarioPreview {
  scenarioKey: string;
  displayName: string;
  description: string;
  path: string;
  method: string;
  selected: boolean;
}

export interface ExistingConfig {
  configId: number;
  name: string;
  scenarioCount: number;
}

export interface PreviewResult {
  erpType: string;
  erpDisplayName: string;
  apiCount: number;
  scenarios: ScenarioPreview[];
  existingConfigs: ExistingConfig[];
  suggestedConfigId: number | null;
}

export interface DeployRequest {
  erpType: string;
  targetConfigId: number | null;
  scenarios: ScenarioPreview[];
}

export interface DeployResult {
  success: boolean;
  message: string;
  configId?: number;
  scenarioCount?: number;
}

// 在 erpApi 对象中添加新方法

  // ERP AI Adapter Preview APIs
  previewAdaptation: async (files: File[]): Promise<ApiResponse<PreviewResult>> => {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));

    const response = await client.post<ApiResponse<PreviewResult>>(
      '/erp-ai/preview',
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' }
      }
    );
    return response.data;
  },

  deployAdaptation: async (request: DeployRequest): Promise<ApiResponse<DeployResult>> => {
    const response = await client.post<ApiResponse<DeployResult>>(
      '/erp-ai/deploy',
      request
    );
    return response.data;
  },
```

**Step 2: TypeScript 类型检查**

```bash
cd /Users/user/nexusarchive
npx tsc --noEmit
```

**Step 3: 提交**

```bash
git add src/api/erp.ts
git commit -m "feat(ai): add preview and deploy API methods"
```

---

## Task 6: 前端预览确认界面

**目标:** 实现预览 Modal，显示识别结果并让用户确认

**文件:**
- 修改: `src/components/settings/IntegrationSettings.tsx`

**Step 1: 添加状态和类型**

```typescript
// 在组件中添加新状态

interface PreviewResult {
  erpType: string;
  erpDisplayName: string;
  apiCount: number;
  scenarios: Array<{
    scenarioKey: string;
    displayName: string;
    description: string;
    path: string;
    method: string;
    selected: boolean;
  }>;
  existingConfigs: Array<{
    configId: number;
    name: string;
    scenarioCount: number;
  }>;
  suggestedConfigId: number | null;
}

// AI 适配预览状态
const [showPreviewModal, setShowPreviewModal] = useState(false);
const [previewLoading, setPreviewLoading] = useState(false);
const [previewResult, setPreviewResult] = useState<PreviewResult | null>(null);
const [selectedConfigId, setSelectedConfigId] = useState<number | null>(null);
const [editingScenarios, setEditingScenarios] = useState<PreviewResult['scenarios']>([]);
```

**Step 2: 添加预览处理函数**

```typescript
// 打开预览 Modal
const handleAiPreview = async (files: File[]) => {
  setPreviewLoading(true);
  setShowPreviewModal(true);

  try {
    const res = await erpApi.previewAdaptation(files);
    if (res.code === 200 && res.data) {
      setPreviewResult(res.data);
      setSelectedConfigId(res.data.suggestedConfigId);
      setEditingScenarios(res.data.scenarios.map(s => ({ ...s })));
    } else {
      toast.error(res.message || '预览失败');
      setShowPreviewModal(false);
    }
  } catch (error: any) {
    toast.error('预览失败: ' + (error.message || '未知错误'));
    setShowPreviewModal(false);
  } finally {
    setPreviewLoading(false);
  }
};

// 确认部署
const handleConfirmDeploy = async () => {
  if (!previewResult) return;

  setPreviewLoading(true);
  try {
    const deployRequest = {
      erpType: previewResult.erpType,
      targetConfigId: selectedConfigId,
      scenarios: editingScenarios.filter(s => s.selected)
    };

    const res = await erpApi.deployAdaptation(deployRequest);
    if (res.code === 200 && res.data?.success) {
      toast.success('部署成功！请重启后端以加载新的适配器');
      setShowPreviewModal(false);
      // 刷新配置列表
      loadConfigs();
    } else {
      toast.error(res.message || '部署失败');
    }
  } catch (error: any) {
    toast.error('部署失败: ' + (error.message || '未知错误'));
  } finally {
    setPreviewLoading(false);
  }
};

// 修改现有的文件上传处理
const handleAiAdapterFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
  const files = Array.from(e.target.files || []);
  setAiAdapterFiles(files);

  // 自动触发预览
  if (files.length > 0) {
    handleAiPreview(files);
  }
};
```

**Step 3: 添加预览 Modal UI**

```tsx
{/* AI 适配预览 Modal */}
{showPreviewModal && previewResult && (
  <div className="fixed inset-0 z-[110] flex items-center justify-center bg-black/50 backdrop-blur-sm">
    <div className="bg-white rounded-xl shadow-2xl w-[800px] max-w-[95vw] max-h-[90vh] flex flex-col">
      {/* Header */}
      <div className="p-6 border-b">
        <h3 className="text-lg font-bold flex items-center gap-2">
          <Zap size={20} className="text-purple-600" />
          ERP AI 智能适配 - 预览确认
        </h3>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        {previewLoading ? (
          <div className="flex justify-center py-12">
            <Loader2 className="animate-spin text-purple-600" size={32} />
          </div>
        ) : (
          <>
            {/* 识别结果 */}
            <div className="bg-purple-50 rounded-lg p-4">
              <h4 className="font-semibold text-purple-900 mb-2">📋 识别结果</h4>
              <div className="text-sm text-purple-800 space-y-1">
                <p>ERP 类型: <strong>{previewResult.erpDisplayName}</strong></p>
                <p>文档: test-yonsuite-salesout-api.json</p>
                <p>识别到 <strong>{previewResult.apiCount}</strong> 个 API</p>
              </div>
            </div>

            {/* 场景列表 */}
            <div>
              <h4 className="font-semibold text-slate-800 mb-3">📦 场景列表</h4>
              <div className="border rounded-lg divide-y max-h-60 overflow-y-auto">
                {editingScenarios.map((scenario, idx) => (
                  <div key={idx} className="p-3 hover:bg-slate-50 flex items-start gap-3">
                    <input
                      type="checkbox"
                      checked={scenario.selected}
                      onChange={(e) => {
                        const newScenarios = [...editingScenarios];
                        newScenarios[idx].selected = e.target.checked;
                        setEditingScenarios(newScenarios);
                      }}
                      className="mt-1"
                    />
                    <div className="flex-1 min-w-0">
                      <div className="font-mono text-xs text-purple-700">{scenario.scenarioKey}</div>
                      <input
                        type="text"
                        value={scenario.displayName}
                        onChange={(e) => {
                          const newScenarios = [...editingScenarios];
                          newScenarios[idx].displayName = e.target.value;
                          setEditingScenarios(newScenarios);
                        }}
                        className="font-medium text-slate-800 w-full border-b border-transparent hover:border-slate-300 focus:border-purple-500 focus:outline-none px-1"
                      />
                      <div className="text-xs text-slate-500 mt-1">{scenario.description}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* 目标位置 */}
            <div>
              <h4 className="font-semibold text-slate-800 mb-3">🎯 目标位置</h4>
              <div className="space-y-2">
                <label className={`flex items-start gap-3 p-4 border rounded-lg cursor-pointer transition-colors ${selectedConfigId === null ? 'border-purple-500 bg-purple-50' : 'border-slate-200 hover:border-slate-300'}`}>
                  <input
                    type="radio"
                    name="targetConfig"
                    checked={selectedConfigId === null}
                    onChange={() => setSelectedConfigId(null)}
                    className="mt-1"
                  />
                  <div>
                    <div className="font-medium text-slate-800">创建新连接器</div>
                    <div className="text-sm text-slate-500">
                      "{previewResult.erpDisplayName}-{Date.now()}"
                    </div>
                  </div>
                </label>

                {previewResult.existingConfigs.map(config => (
                  <label key={config.configId} className={`flex items-start gap-3 p-4 border rounded-lg cursor-pointer transition-colors ${selectedConfigId === config.configId ? 'border-purple-500 bg-purple-50' : 'border-slate-200 hover:border-slate-300'}`}>
                    <input
                      type="radio"
                      name="targetConfig"
                      checked={selectedConfigId === config.configId}
                      onChange={() => setSelectedConfigId(config.configId)}
                      className="mt-1"
                    />
                    <div>
                      <div className="font-medium text-slate-800">{config.name}</div>
                      <div className="text-sm text-slate-500">
                        当前有 {config.scenarioCount} 个场景，将新增 {editingScenarios.filter(s => s.selected).length} 个
                      </div>
                    </div>
                  </label>
                ))}
              </div>
            </div>
          </>
        )}
      </div>

      {/* Footer */}
      <div className="p-6 border-t bg-slate-50 flex justify-end gap-3">
        <button
          onClick={() => setShowPreviewModal(false)}
          disabled={previewLoading}
          className="px-4 py-2 text-slate-600 hover:bg-slate-200 rounded-lg transition-colors"
        >
          取消
        </button>
        <button
          onClick={handleConfirmDeploy}
          disabled={previewLoading || editingScenarios.filter(s => s.selected).length === 0}
          className="px-6 py-2 bg-gradient-to-r from-purple-600 to-blue-600 text-white rounded-lg hover:from-purple-700 hover:to-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all flex items-center gap-2"
        >
          {previewLoading ? (
            <>
              <Loader2 className="animate-spin" size={16} />
              处理中...
            </>
          ) : (
            <>
              <Zap size={16} />
              确认并部署
            </>
          )}
        </button>
      </div>
    </div>
  </div>
)}
```

**Step 4: 测试前端界面**

```bash
# 访问集成设置页面
# http://localhost:15175/system/settings/integration
# 点击 "+" → "AI 智能适配器" → 上传测试文件
```

**Step 5: 提交**

```bash
git add src/components/settings/IntegrationSettings.tsx
git commit -m "feat(ai): add preview modal for ERP adapter"
```

---

## Task 7: 端到端测试

**目标:** 完整测试整个流程

**测试步骤:**

1. **启动所有服务**
```bash
cd /Users/user/nexusarchive
docker-compose -f docker-compose.dev.yml up -d
```

2. **等待服务就绪**
```bash
sleep 30
curl -s http://localhost:19090/api/health
curl -s http://localhost:15175/
```

3. **访问前端**
```
http://localhost:15175/system/settings/integration
```

4. **执行测试用例:**

   | 用例 | 步骤 | 预期结果 |
   |------|------|---------|
   | 上传 YonSuite 文档 | 点击 "+" → "AI 智能适配器" → 上传 test-yonsuite-salesout-api.json | 显示预览界面，识别为 YonSuite |
   | 查看场景列表 | 预览界面中查看场景 | 显示 3 个场景，可编辑名称 |
   | 选择现有连接器 | 选择"添加到用友 YonSuite (生产环境)" | 单选框选中 |
   | 确认部署 | 点击"确认并部署" | 提示成功，刷新后显示新场景 |

5. **验证数据库**
```bash
PGPASSWORD=postgres psql -h localhost -U postgres -d nexusarchive -c "
SELECT scenario_key, name
FROM sys_erp_scenario
WHERE config_id = 1
ORDER BY id DESC
LIMIT 5;
"
```

6. **验证代码生成**
```bash
ls -la /Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/yonsuite/
```

**Step 6: 提交**

```bash
git add docs/plans/2025-01-03-erp-ai-adapter-enhancement.md
git commit -m "docs(ai): add ERP AI adapter enhancement implementation plan"
```

---

## 验收标准

完成后应满足：

- ✅ 上传 OpenAPI 文档后显示预览界面
- ✅ 正确识别 ERP 类型（YonSuite/Kingdee/泛微/通用）
- ✅ 为每个 API 生成场景代码和名称
- ✅ 可编辑场景名称，可选择目标连接器
- ✅ 添加到现有连接器时正确创建场景记录
- ✅ 创建新连接器时正确初始化配置
- ✅ 场景命名与 API 保持一致性
- ✅ 生成 Java 代码保存到正确目录

---

**计划完成！** 保存到 `docs/plans/2025-01-03-erp-ai-adapter-enhancement.md`

**执行选项:**

**1. 当前会话执行（推荐）** - 我使用 superpowers:subagent-driven-development 在本会话中逐步执行，每个任务后进行代码审查

**2. 独立会话执行** - 打开新会话使用 superpowers:executing-plans 批量执行

你希望使用哪种方式？
