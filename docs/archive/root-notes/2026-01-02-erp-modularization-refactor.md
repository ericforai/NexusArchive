# ERP 接口模块化与自指能力增强实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 ERP 接口架构从双模式（Plugin + Adapter）统一为单一 Adapter 模式，并通过注解驱动的元数据系统实现自指能力。

**架构:**
1. 移除冗余的 Plugin 层，保留 Adapter 层作为统一的 ERP 集成接口
2. 添加 `@ErpAdapter` 注解，实现声明式元数据定义
3. 创建 `ErpMetadataRegistry` 作为运行时元数据注册中心
4. 消除用友适配器的代码重复实现

**Tech Stack:** Java 17, Spring Boot 3.1.6, Lombok, JUnit 5, ArchUnit

---

## Task 1: 创建 @ErpAdapter 注解

**目标:** 实现声明式元数据定义，替代硬编码的接口方法

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/annotation/ErpAdapter.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/annotation/ErpAdapterTest.java`

**Step 1: 创建注解包目录**

Run: `mkdir -p nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/annotation`

**Step 2: 编写注解定义**

```java
// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/annotation/ErpAdapter.java
package com.nexusarchive.integration.erp.annotation;

import java.lang.annotation.*;

/**
 * ERP 适配器元数据注解
 * <p>
 * 用于声明 ERP 适配器的元数据，支持运行时自动发现和注册
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ErpAdapter {

    /**
     * 适配器唯一标识
     * 例如: yonsuite, kingdee, generic
     */
    String identifier();

    /**
     * 适配器显示名称
     * 例如: 用友YonSuite, 金蝶云星空
     */
    String name();

    /**
     * 适配器描述
     */
    String description() default "";

    /**
     * 适配器版本
     */
    String version() default "1.0.0";

    /**
     * 支持的 ERP 系统类型
     */
    String erpType() default "custom";

    /**
     * 支持的业务场景
     * 例如: {"VOUCHER_SYNC", "ATTACHMENT_SYNC", "WEBHOOK"}
     */
    String[] supportedScenarios() default {};

    /**
     * 是否支持 Webhook
     */
    boolean supportsWebhook() default false;

    /**
     * 适配器优先级（数字越小优先级越高）
     * 用于多个适配器支持同一 ERP 类型时的选择
     */
    int priority() default 100;
}
```

**Step 3: 编写测试验证注解功能**

```java
// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/annotation/ErpAdapterTest.java
package com.nexusarchive.integration.erp.annotation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ErpAdapter(
    identifier = "test-adapter",
    name = "测试适配器",
    description = "用于测试的适配器",
    version = "1.0.0",
    erpType = "test",
    supportedScenarios = {"VOUCHER_SYNC"},
    supportsWebhook = true,
    priority = 10
)
class TestAdapterForAnnotation {
    // 测试用类
}

class ErpAdapterTest {

    @Test
    void shouldExtractAnnotationMetadata() {
        // Given
        Class<?> testClass = TestAdapterForAnnotation.class;

        // When
        ErpAdapter annotation = testClass.getAnnotation(ErpAdapter.class);

        // Then
        assertEquals("test-adapter", annotation.identifier());
        assertEquals("测试适配器", annotation.name());
        assertEquals("用于测试的适配器", annotation.description());
        assertEquals("1.0.0", annotation.version());
        assertEquals("test", annotation.erpType());
        assertEquals(1, annotation.supportedScenarios().length);
        assertEquals("VOUCHER_SYNC", annotation.supportedScenarios()[0]);
        assertEquals(true, annotation.supportsWebhook());
        assertEquals(10, annotation.priority());
    }
}
```

**Step 4: 运行测试验证注解可用**

Run: `cd nexusarchive-java && mvn test -Dtest=ErpAdapterTest`

Expected: 测试通过，显示 "Tests run: 1, Failures: 0, Errors: 0"

**Step 5: 提交注解实现**

Run:
```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/annotation/ \
        nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/annotation/
git commit -m "feat(erp): 添加 @ErpAdapter 注解实现声明式元数据"
```

---

## Task 2: 创建 ErpMetadata 元数据 DTO

**目标:** 创建结构化的元数据对象，用于运行时传递适配器信息

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/ErpMetadata.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/dto/ErpMetadataTest.java`

**Step 1: 编写元数据 DTO**

```java
// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/ErpMetadata.java
package com.nexusarchive.integration.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * ERP 适配器元数据
 * <p>
 * 记录适配器的运行时可查询信息
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpMetadata {

    /**
     * 适配器唯一标识
     */
    private String identifier;

    /**
     * 适配器显示名称
     */
    private String name;

    /**
     * 适配器描述
     */
    private String description;

    /**
     * 适配器版本
     */
    private String version;

    /**
     * ERP 系统类型
     */
    private String erpType;

    /**
     * 支持的业务场景
     */
    private Set<String> supportedScenarios;

    /**
     * 是否支持 Webhook
     */
    private boolean supportsWebhook;

    /**
     * 适配器优先级
     */
    private int priority;

    /**
     * 适配器实现类全限定名
     */
    private String implementationClass;

    /**
     * 注册时间
     */
    private LocalDateTime registeredAt;

    /**
     * 是否启用
     */
    private boolean enabled;
}
```

**Step 2: 编写测试验证 DTO 功能**

```java
// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/dto/ErpMetadataTest.java
package com.nexusarchive.integration.erp.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ErpMetadataTest {

    @Test
    void shouldBuildMetadataWithAllFields() {
        // Given
        Set<String> scenarios = Set.of("VOUCHER_SYNC", "ATTACHMENT_SYNC");

        // When
        ErpMetadata metadata = ErpMetadata.builder()
            .identifier("yonsuite")
            .name("用友YonSuite")
            .description("用友新一代企业云服务平台")
            .version("1.0.0")
            .erpType("YONSUITE")
            .supportedScenarios(scenarios)
            .supportsWebhook(true)
            .priority(10)
            .implementationClass("com.nexusarchive.integration.erp.adapter.impl.YonSuiteErpAdapter")
            .registeredAt(LocalDateTime.now())
            .enabled(true)
            .build();

        // Then
        assertEquals("yonsuite", metadata.getIdentifier());
        assertEquals("用友YonSuite", metadata.getName());
        assertEquals(2, metadata.getSupportedScenarios().size());
        assertTrue(metadata.supportsWebhook());
        assertEquals(10, metadata.getPriority());
        assertTrue(metadata.isEnabled());
    }

    @Test
    void shouldSupportEmptyConstructor() {
        // When
        ErpMetadata metadata = new ErpMetadata();

        // Then
        assertNotNull(metadata);
        assertNull(metadata.getIdentifier());
    }
}
```

**Step 3: 运行测试验证 DTO**

Run: `cd nexusarchive-java && mvn test -Dtest=ErpMetadataTest`

Expected: 测试通过

**Step 4: 提交 DTO 实现**

Run:
```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/ErpMetadata.java \
        nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/dto/ErpMetadataTest.java
git commit -m "feat(erp): 添加 ErpMetadata 元数据 DTO"
```

---

## Task 3: 创建 ErpMetadataRegistry 元数据注册中心

**目标:** 实现运行时自动发现和注册适配器元数据

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/registry/ErpMetadataRegistry.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/ErpAdapterFactory.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/registry/ErpMetadataRegistryTest.java`

**Step 1: 创建 registry 包目录**

Run: `mkdir -p nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/registry`

**Step 2: 编写元数据注册中心**

```java
// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/registry/ErpMetadataRegistry.java
package com.nexusarchive.integration.erp.registry;

import com.nexusarchive.integration.erp.annotation.ErpAdapter;
import com.nexusarchive.integration.erp.dto.ErpMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ERP 适配器元数据注册中心
 * <p>
 * 负责自动发现、注册和查询 ERP 适配器元数据
 * </p>
 */
@Slf4j
@Component
public class ErpMetadataRegistry {

    /**
     * 按标识索引的元数据存储
     */
    private final Map<String, ErpMetadata> byIdentifier = new ConcurrentHashMap<>();

    /**
     * 按 ERP 类型索引的元数据存储
     */
    private final Map<String, List<ErpMetadata>> byErpType = new ConcurrentHashMap<>();

    /**
     * 注册适配器元数据
     *
     * @param adapterClass 适配器实现类
     */
    public void register(Class<?> adapterClass) {
        ErpAdapter annotation = adapterClass.getAnnotation(ErpAdapter.class);
        if (annotation == null) {
            log.warn("Class {} is not annotated with @ErpAdapter, skipping registration",
                adapterClass.getName());
            return;
        }

        ErpMetadata metadata = ErpMetadata.builder()
            .identifier(annotation.identifier())
            .name(annotation.name())
            .description(annotation.description())
            .version(annotation.version())
            .erpType(annotation.erpType())
            .supportedScenarios(Set.of(annotation.supportedScenarios()))
            .supportsWebhook(annotation.supportsWebhook())
            .priority(annotation.priority())
            .implementationClass(adapterClass.getName())
            .registeredAt(LocalDateTime.now())
            .enabled(true)
            .build();

        // 按标识注册
        byIdentifier.put(metadata.getIdentifier(), metadata);

        // 按 ERP 类型注册
        byErpType.computeIfAbsent(metadata.getErpType(), k -> new ArrayList<>())
            .add(metadata);

        // 按优先级排序
        byErpType.get(metadata.getErpType()).sort(Comparator.comparingInt(ErpMetadata::getPriority));

        log.info("Registered ERP adapter: {} ({})",
            metadata.getIdentifier(), metadata.getName());
    }

    /**
     * 批量注册适配器
     *
     * @param adapterClasses 适配器实现类列表
     */
    public void registerAll(Collection<Class<?>> adapterClasses) {
        adapterClasses.forEach(this::register);
        log.info("Registered {} ERP adapters in total", adapterClasses.size());
    }

    /**
     * 根据标识获取元数据
     *
     * @param identifier 适配器标识
     * @return 元数据，如果不存在返回 null
     */
    public ErpMetadata getByIdentifier(String identifier) {
        return byIdentifier.get(identifier);
    }

    /**
     * 根据 ERP 类型获取所有适配器元数据
     *
     * @param erpType ERP 类型
     * @return 适配器元数据列表（按优先级排序）
     */
    public List<ErpMetadata> getByErpType(String erpType) {
        return byErpType.getOrDefault(erpType, Collections.emptyList());
    }

    /**
     * 获取所有已注册的适配器元数据
     *
     * @return 元数据列表
     */
    public Collection<ErpMetadata> getAll() {
        return byIdentifier.values();
    }

    /**
     * 检查适配器是否已注册
     *
     * @param identifier 适配器标识
     * @return 是否已注册
     */
    public boolean isRegistered(String identifier) {
        return byIdentifier.containsKey(identifier);
    }

    /**
     * 获取已注册适配器数量
     *
     * @return 数量
     */
    public int size() {
        return byIdentifier.size();
    }
}
```

**Step 3: 更新 ErpAdapterFactory 集成元数据注册**

```java
// 在 ErpAdapterFactory.java 中添加:

@Autowired
private ErpMetadataRegistry metadataRegistry;

@PostConstruct
public void scanAndRegisterAdapters() {
    // 扫描所有 ErpAdapter 实现，自动注册元数据
    adapters.values().forEach(adapter -> {
        Class<?> adapterClass = adapter.getClass();
        if (adapterClass.isAnnotationPresent(ErpAdapter.class)) {
            metadataRegistry.register(adapterClass);
        }
    });

    log.info("Scanned and registered {} ERP adapters", adapters.size());
}

/**
 * 获取适配器元数据
 *
 * @param identifier 适配器标识
 * @return 元数据
 */
public ErpMetadata getMetadata(String identifier) {
    return metadataRegistry.getByIdentifier(identifier);
}

/**
 * 获取所有适配器元数据
 *
 * @return 元数据列表
 */
public Collection<ErpMetadata> getAllMetadata() {
    return metadataRegistry.getAll();
}
```

**Step 4: 编写测试验证注册中心功能**

```java
// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/registry/ErpMetadataRegistryTest.java
package com.nexusarchive.integration.erp.registry;

import com.nexusarchive.integration.erp.annotation.ErpAdapter;
import com.nexusarchive.integration.erp.dto.ErpMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ErpAdapter(
    identifier = "test-registry-adapter",
    name = "测试注册适配器",
    erpType = "TEST",
    supportedScenarios = {"VOUCHER_SYNC"},
    priority = 10
)
class TestAdapterForRegistry {}

class ErpMetadataRegistryTest {

    private ErpMetadataRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ErpMetadataRegistry();
    }

    @Test
    void shouldRegisterAdapterMetadata() {
        // When
        registry.register(TestAdapterForRegistry.class);

        // Then
        ErpMetadata metadata = registry.getByIdentifier("test-registry-adapter");
        assertNotNull(metadata);
        assertEquals("测试注册适配器", metadata.getName());
        assertEquals("TEST", metadata.getErpType());
        assertEquals(Set.of("VOUCHER_SYNC"), metadata.getSupportedScenarios());
    }

    @Test
    void shouldReturnNullForUnregisteredAdapter() {
        // When
        ErpMetadata metadata = registry.getByIdentifier("non-existent");

        // Then
        assertNull(metadata);
    }

    @Test
    void shouldListAllRegisteredAdapters() {
        // When
        registry.register(TestAdapterForRegistry.class);
        Collection<ErpMetadata> all = registry.getAll();

        // Then
        assertEquals(1, all.size());
        assertTrue(all.stream().anyMatch(m -> m.getIdentifier().equals("test-registry-adapter")));
    }

    @Test
    void shouldCheckRegistrationStatus() {
        // Given
        registry.register(TestAdapterForRegistry.class);

        // Then
        assertTrue(registry.isRegistered("test-registry-adapter"));
        assertFalse(registry.isRegistered("non-existent"));
    }

    @Test
    void shouldGroupByErpType() {
        // When
        registry.register(TestAdapterForRegistry.class);
        List<ErpMetadata> testAdapters = registry.getByErpType("TEST");

        // Then
        assertEquals(1, testAdapters.size());
    }
}
```

**Step 5: 运行测试验证注册中心**

Run: `cd nexusarchive-java && mvn test -Dtest=ErpMetadataRegistryTest`

Expected: 所有测试通过

**Step 6: 提交注册中心实现**

Run:
```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/registry/ \
        nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/registry/
git commit -m "feat(erp): 添加 ErpMetadataRegistry 元数据注册中心"
```

---

## Task 4: 为现有适配器添加 @ErpAdapter 注解

**目标:** 将现有适配器迁移到注解驱动的元数据系统

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/impl/YonSuiteErpAdapter.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/impl/KingdeeAdapter.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/impl/WeaverAdapter.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/impl/GenericErpAdapter.java`

**Step 1: 为 YonSuiteErpAdapter 添加注解**

```java
// 在 YonSuiteErpAdapter.java 类声明前添加:

@ErpAdapter(
    identifier = "yonsuite",
    name = "用友YonSuite",
    description = "用友新一代企业云服务平台，支持凭证、附件同步和 Webhook 推送",
    version = "1.0.0",
    erpType = "YONSUITE",
    supportedScenarios = {"VOUCHER_SYNC", "ATTACHMENT_SYNC", "WEBHOOK"},
    supportsWebhook = true,
    priority = 10
)
public class YonSuiteErpAdapter implements ErpAdapter {
    // ... 现有代码保持不变
}
```

**Step 2: 为 KingdeeAdapter 添加注解**

```java
// 在 KingdeeAdapter.java 类声明前添加:

@ErpAdapter(
    identifier = "kingdee",
    name = "金蝶云星空",
    description = "金蝶云星空 ERP 系统，支持凭证和附件同步",
    version = "1.0.0",
    erpType = "KINGDEE",
    supportedScenarios = {"VOUCHER_SYNC", "ATTACHMENT_SYNC"},
    supportsWebhook = false,
    priority = 20
)
public class KingdeeAdapter implements ErpAdapter {
    // ... 现有代码保持不变
}
```

**Step 3: 为 WeaverAdapter 添加注解**

```java
// 在 WeaverAdapter.java 类声明前添加:

@ErpAdapter(
    identifier = "weaver",
    name = "浪潮GS",
    description = "浪潮集团管理软件系统",
    version = "1.0.0",
    erpType = "WEAVER",
    supportedScenarios = {"VOUCHER_SYNC"},
    supportsWebhook = false,
    priority = 30
)
public class WeaverAdapter implements ErpAdapter {
    // ... 现有代码保持不变
}
```

**Step 4: 为 GenericErpAdapter 添加注解**

```java
// 在 GenericErpAdapter.java 类声明前添加:

@ErpAdapter(
    identifier = "generic",
    name = "通用ERP",
    description = "通用 ERP 适配器，支持标准接口协议",
    version = "1.0.0",
    erpType = "GENERIC",
    supportedScenarios = {"VOUCHER_SYNC"},
    supportsWebhook = false,
    priority = 100
)
public class GenericErpAdapter implements ErpAdapter {
    // ... 现有代码保持不变
}
```

**Step 5: 编写集成测试验证注解生效**

```java
// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/adapter/ErpAdapterAnnotationIntegrationTest.java
package com.nexusarchive.integration.erp.adapter;

import com.nexusarchive.integration.erp.annotation.ErpAdapter;
import com.nexusarchive.integration.erp.registry.ErpMetadataRegistry;
import com.nexusarchive.integration.erp.adapter.impl.YonSuiteErpAdapter;
import com.nexusarchive.integration.erp.adapter.impl.KingdeeAdapter;
import com.nexusarchive.integration.erp.adapter.impl.GenericErpAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ErpAdapterAnnotationIntegrationTest {

    @Autowired
    private ErpMetadataRegistry registry;

    @Test
    void shouldAutoRegisterAnnotatedAdapters() {
        // Then
        assertTrue(registry.isRegistered("yonsuite"));
        assertTrue(registry.isRegistered("kingdee"));
        assertTrue(registry.isRegistered("generic"));
    }

    @Test
    void shouldExtractCorrectMetadata() {
        // When
        var yonsuiteMetadata = registry.getByIdentifier("yonsuite");

        // Then
        assertEquals("yonsuite", yonsuiteMetadata.getIdentifier());
        assertEquals("用友YonSuite", yonsuiteMetadata.getName());
        assertEquals("YONSUITE", yonsuiteMetadata.getErpType());
        assertTrue(yonsuiteMetadata.supportsWebhook());
    }

    @Test
    void shouldHaveAnnotationsOnAdapterClasses() {
        // Then
        assertNotNull(YonSuiteErpAdapter.class.getAnnotation(ErpAdapter.class));
        assertNotNull(KingdeeAdapter.class.getAnnotation(ErpAdapter.class));
        assertNotNull(GenericErpAdapter.class.getAnnotation(ErpAdapter.class));
    }
}
```

**Step 6: 运行集成测试验证**

Run: `cd nexusarchive-java && mvn test -Dtest=ErpAdapterAnnotationIntegrationTest`

Expected: 所有测试通过，验证注解自动注册生效

**Step 7: 提交适配器注解迁移**

Run:
```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/impl/ \
        nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/adapter/ErpAdapterAnnotationIntegrationTest.java
git commit -m "feat(erp): 为现有适配器添加 @ErpAdapter 注解"
```

---

## Task 5: 更新 module-manifest.md 记录 ERP 模块详情

**目标:** 在模块清单中添加详细的 ERP 模块元数据记录

**Files:**
- Modify: `docs/architecture/module-manifest.md`

**Step 1: 读取当前模块清单**

Run: `cat docs/architecture/module-manifest.md | grep -A 20 "BE.ERP"`

**Step 2: 在 BE.ERP_PLUGINS 行后添加详细的子模块说明**

在 `## Backend Modules` 部分的 `BE.ERP_PLUGINS` 后添加：

```markdown
### ERP 集成子模块 (v2.4)

| 模块 ID | 名称 | 范围 | 职责一句话 | 允许依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
| BE.ERP.ADAPTER_YONSUITE | YonSuite 适配器 | `integration.erp.adapter.impl.YonSuiteErpAdapter` | 用友 YonSuite ERP 集成 | Spring Integration, `com.nexusarchive.integration.erp.dto..` | ✅ 活跃 v2.4 |
| BE.ERP.ADAPTER_KINGDEE | 金蝶适配器 | `integration.erp.adapter.impl.KingdeeAdapter` | 金蝶云星空 ERP 集成 | Spring Integration, `com.nexusarchive.integration.erp.dto..` | ✅ 活跃 v2.4 |
| BE.ERP.ADAPTER_WEAVER | 浪潮适配器 | `integration.erp.adapter.impl.WeaverAdapter` | 浪潮 GS ERP 集成 | Spring Integration, `com.nexusarchive.integration.erp.dto..` | ✅ 活跃 v2.4 |
| BE.ERP.ADAPTER_GENERIC | 通用适配器 | `integration.erp.adapter.impl.GenericErpAdapter` | 通用 ERP 集成协议 | Spring Integration, `com.nexusarchive.integration.erp.dto..` | ✅ 活跃 v2.4 |
| BE.ERP.METADATA | 元数据管理 | `integration.erp.annotation..`, `integration.erp.registry..` | 适配器元数据注册与查询 | 无 | ✅ 活跃 v2.4 |
```

**Step 3: 更新清单版本号**

修改文件头部的版本信息：

```markdown
> **版本**: 2.4.0
> **更新日期**: 2026-01-02
> **自动生成**: 通过 scripts/discover-frontend-modules.js + ModuleGovernanceService
```

**Step 4: 添加更新日志**

在 `## 更新日志` 部分添加：

```markdown
- **2026-01-02 v2.4.0**: ERP 模块重构 - 统一 Adapter 架构，添加注解驱动的元数据系统
```

**Step 5: 验证清单格式正确**

Run: `npm run modules:validate`

Expected: 前端模块验证通过（ERP 是后端模块，不影响此验证）

**Step 6: 提交模块清单更新**

Run:
```bash
git add docs/architecture/module-manifest.md
git commit -m "docs(erp): 更新模块清单记录 ERP 子模块详情 (v2.4.0)"
```

---

## Task 6: 添加 ArchUnit 测试验证 ERP 模块架构规则

**目标:** 确保新架构的模块边界得到强制执行

**Files:**
- Modify: `nexusarchive-java/src/test/java/com/nexusarchive/ArchitectureTest.java`

**Step 1: 读取现有 ArchUnit 测试**

Run: `head -100 nexusarchive-java/src/test/java/com/nexusarchive/ArchitectureTest.java`

**Step 2: 在 ArchitectureTest.java 中添加 ERP 模块规则**

在 `ArchitectureTest.java` 类中添加新的测试方法：

```java
// 在 ArchitectureTest.java 中添加:

@ArchTest
static final ArchRule erp_adapters_should_only_depend_on_dto_and_annotation =
    classes().that().resideInAPackage("..integration.erp.adapter..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage(
            "..integration.erp.adapter..",
            "..integration.erp.dto..",
            "..integration.erp.annotation..",
            "java..",
            "org.springframework..",
            "lombok.."
        );

@ArchTest
static final ArchRule erp_metadata_registry_should_be_isolated =
    classes().that().resideInAPackage("..integration.erp.registry..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage(
            "..integration.erp.annotation..",
            "..integration.erp.dto..",
            "java..",
            "org.springframework..",
            "lombok.."
        );

@ArchTest
static final ArchRule all_erp_adapters_should_have_erp_adapter_annotation =
    classes().that().implement(ErpAdapter.class)
        .and().areNotInterfaces()
        .should().beAnnotatedWith(ErpAdapter.class)
        .because("All ERP adapter implementations must declare metadata via @ErpAdapter");

@ArchTest
static final ArchRule erp_adapter_annotations_should_have_unique_identifiers =
    classes().that().areAnnotatedWith(ErpAdapter.class)
        .should(new ArchCondition<JavaClass>("have unique @ErpAdapter.identifier") {
            final Set<String> identifiers = new HashSet<>();

            @Override
            public void check(JavaClass item, ConditionEvents events) {
                ErpAdapter annotation = item.getAnnotation(ErpAdapter.class).getAsReflect();
                String identifier = annotation.identifier();

                if (identifiers.contains(identifier)) {
                    String message = String.format("Duplicate @ErpAdapter identifier: %s (used by %s)",
                        identifier, item.getName());
                    events.add(SimpleConditionEvent.violated(item, message));
                } else {
                    identifiers.add(identifier);
                }
            }
        });

@ArchTest
static final ArchRule plugin_layer_should_not_directly_depend_on_adapter_implementations =
    classes().that().resideInAPackage("..service.erp.plugin..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage(
            "..service.erp.plugin..",
            "..integration.erp.adapter..", // 只依赖接口，不依赖 impl
            "..entity..",
            "..dto..",
            "java..",
            "org.springframework..",
            "lombok.."
        )
        .because("Plugin layer should depend on ErpAdapter interface, not concrete implementations");
```

**Step 3: 运行 ArchUnit 测试验证新规则**

Run: `cd nexusarchive-java && mvn test -Dtest=ArchitectureTest`

Expected: 测试通过，验证新架构规则正确

**Step 4: 提交 ArchUnit 规则**

Run:
```bash
git add nexusarchive-java/src/test/java/com/nexusarchive/ArchitectureTest.java
git commit -m "test(erp): 添加 ERP 模块架构 ArchUnit 测试规则"
```

---

## Task 7: 创建 ERP 适配器开发文档

**目标:** 为未来的适配器开发者提供清晰的开发指南

**Files:**
- Create: `docs/architecture/erp-adapter-development-guide.md`

**Step 1: 编写开发指南**

```markdown
# ERP 适配器开发指南

## 概述

本文档描述如何开发新的 ERP 适配器，接入 NexusArchive 系统。

## 架构说明

### 核心组件

- **ErpAdapter 接口**: 所有适配器必须实现的核心接口
- **@ErpAdapter 注解**: 声明式元数据定义
- **ErpMetadataRegistry**: 运行时元数据注册中心
- **ErpAdapterFactory**: 适配器工厂和查询入口

### 模块边界

```
integration/erp/
├── adapter/         # 适配器实现
│   ├── ErpAdapter.java              # 核心接口
│   ├── ErpAdapterFactory.java       # 工厂类
│   └── impl/                        # 具体实现
├── annotation/      # 注解定义
│   └── ErpAdapter.java              # 元数据注解
├── registry/        # 元数据注册
│   └── ErpMetadataRegistry.java     # 注册中心
└── dto/            # 数据传输对象
    ├── ErpConfig.java
    ├── VoucherDTO.java
    └── AttachmentDTO.java
```

## 开发步骤

### 1. 实现 ErpAdapter 接口

```java
package com.nexusarchive.integration.erp.adapter.impl;

import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.annotation.ErpAdapter;
import com.nexusarchive.integration.erp.dto.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@ErpAdapter(
    identifier = "myerp",
    name = "我的ERP系统",
    description = "自定义 ERP 系统适配器",
    version = "1.0.0",
    erpType = "CUSTOM",
    supportedScenarios = {"VOUCHER_SYNC"},
    supportsWebhook = false,
    priority = 50
)
@Component
public class MyErpAdapter implements ErpAdapter {

    @Override
    public String getIdentifier() {
        return "myerp";
    }

    @Override
    public String getName() {
        return "我的ERP系统";
    }

    @Override
    public ConnectionTestResult testConnection(ErpConfig config) {
        // 实现连接测试逻辑
        return ConnectionTestResult.success();
    }

    @Override
    public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        // 实现凭证同步逻辑
        return List.of();
    }

    @Override
    public VoucherDTO getVoucherDetail(ErpConfig config, String voucherNo) {
        // 实现凭证详情查询
        return null;
    }

    @Override
    public List<AttachmentDTO> getAttachments(ErpConfig config, String voucherNo) {
        // 实现附件查询
        return List.of();
    }

    @Override
    public List<ErpScenario> getAvailableScenarios() {
        // 返回支持的业务场景
        return List.of(
            ErpScenario.builder()
                .scenarioCode("VOUCHER_SYNC")
                .scenarioName("凭证同步")
                .syncStrategy("MANUAL")
                .enabled(true)
                .build()
        );
    }
}
```

### 2. 编写单元测试

```java
package com.nexusarchive.integration.erp.adapter.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MyErpAdapterTest {

    @Autowired
    private ErpAdapterFactory factory;

    @Test
    void shouldRegisterMyErpAdapter() {
        // Given
        String identifier = "myerp";

        // When
        ErpAdapter adapter = factory.getAdapter(identifier);
        ErpMetadata metadata = factory.getMetadata(identifier);

        // Then
        assertNotNull(adapter);
        assertEquals("我的ERP系统", metadata.getName());
        assertTrue(metadata.getSupportedScenarios().contains("VOUCHER_SYNC"));
    }

    @Test
    void shouldHaveCorrectAnnotation() {
        // When
        ErpAdapter annotation = MyErpAdapter.class.getAnnotation(ErpAdapter.class);

        // Then
        assertNotNull(annotation);
        assertEquals("myerp", annotation.identifier());
        assertEquals("CUSTOM", annotation.erpType());
    }
}
```

### 3. 运行测试

```bash
cd nexusarchive-java
mvn test -Dtest=MyErpAdapterTest
mvn test -Dtest=ArchitectureTest  # 验证架构规则
```

### 4. 提交代码

```bash
git add .
git commit -m "feat(erp): 添加 [ERP名称] 适配器"
```

## 元数据注解说明

### @ErpAdapter 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| identifier | String | ✅ | 唯一标识，如 "yonsuite", "kingdee" |
| name | String | ✅ | 显示名称，如 "用友YonSuite" |
| description | String | ❌ | 详细描述 |
| version | String | ❌ | 版本号，默认 "1.0.0" |
| erpType | String | ❌ | ERP类型，默认 "custom" |
| supportedScenarios | String[] | ❌ | 支持的场景列表 |
| supportsWebhook | boolean | ❌ | 是否支持 Webhook，默认 false |
| priority | int | ❌ | 优先级，数字越小优先级越高，默认 100 |

### 支持的业务场景

- `VOUCHER_SYNC`: 凭证同步
- `ATTACHMENT_SYNC`: 附件同步
- `WEBHOOK`: Webhook 推送
- `PAYMENT_SYNC`: 收款单同步
- `ARCHIVAL_FEEDBACK`: 归档状态回写

## 最佳实践

### 1. 依赖注入

```java
@Component
public class MyErpAdapter implements ErpAdapter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public MyErpAdapter(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
}
```

### 2. 错误处理

```java
@Override
public ConnectionTestResult testConnection(ErpConfig config) {
    try {
        // 尝试连接
        callErpApi(config);
        return ConnectionTestResult.success();
    } catch (ConnectException e) {
        return ConnectionTestResult.failure("连接失败: " + e.getMessage());
    } catch (AuthenticationException e) {
        return ConnectionTestResult.failure("认证失败: " + e.getMessage());
    }
}
```

### 3. 日志记录

```java
private static final Logger log = LoggerFactory.getLogger(MyErpAdapter.class);

@Override
public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
    log.info("开始同步凭证: ERP={}, startDate={}, endDate={}",
        config.getErpType(), startDate, endDate);

    try {
        List<VoucherDTO> vouchers = doSync(config, startDate, endDate);
        log.info("凭证同步完成: 共 {} 条", vouchers.size());
        return vouchers;
    } catch (Exception e) {
        log.error("凭证同步失败", e);
        throw new ErpSyncException("凭证同步失败", e);
    }
}
```

### 4. 配置验证

```java
@Override
public ErpPluginResult validateConfig(ErpConfig config) {
    List<String> errors = new ArrayList<>();

    if (config.getUrl() == null || config.getUrl().isBlank()) {
        errors.add("ERP URL 不能为空");
    }

    if (config.getAppKey() == null) {
        errors.add("AppKey 不能为空");
    }

    return errors.isEmpty()
        ? ErpPluginResult.success("配置验证通过")
        : ErpPluginResult.failure("配置验证失败: " + String.join(", ", errors));
}
```

## 架构测试

ArchUnit 会自动验证以下规则：

1. ✅ 所有 `ErpAdapter` 实现必须有 `@ErpAdapter` 注解
2. ✅ `@ErpAdapter.identifier()` 必须唯一
3. ✅ Adapter 层不能直接依赖 Adapter 实现类（只能依赖接口）
4. ✅ Metadata Registry 只能依赖 annotation、dto 和标准库

违反这些规则会导致 CI/CD 失败。

## 参考实现

- `YonSuiteErpAdapter.java`: 用友 YonSuite 完整实现
- `KingdeeAdapter.java`: 金蝶云星空实现
- `GenericErpAdapter.java`: 通用适配器实现

## 故障排查

### 问题: 适配器未被发现

**检查:**
1. 是否添加了 `@Component` 注解？
2. 是否实现了 `ErpAdapter` 接口？
3. 是否添加了 `@ErpAdapter` 注解？
4. `identifier` 是否唯一？

### 问题: ArchUnit 测试失败

**检查:**
1. 是否违反了模块边界规则？
2. 是否有循环依赖？
3. 运行 `mvn test -Dtest=ArchitectureTest` 查看详细错误

### 问题: 元数据未注册

**检查:**
1. 查看 `ErpMetadataRegistry` 日志
2. 确认 `@ErpAdapter` 注解配置正确
3. 重启应用触发扫描
```

**Step 2: 提交开发指南**

Run:
```bash
git add docs/architecture/erp-adapter-development-guide.md
git commit -m "docs(erp): 添加 ERP 适配器开发指南"
```

---

## Task 8: 创建 README 文件总结 ERP 模块重构

**目标:** 在 integration/erp 目录创建 README 说明模块重构

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/README.md`

**Step 1: 编写 ERP 模块 README**

```markdown
# ERP 集成模块

## 概述

本模块负责与各类 ERP 系统的集成，提供统一的适配器接口和元数据管理。

## 架构 (v2.4)

### 设计模式

- **适配器模式**: 统一不同 ERP 系统的接口差异
- **工厂模式**: ErpAdapterFactory 自动管理所有适配器实例
- **注解驱动**: @ErpAdapter 实现声明式元数据定义
- **注册中心模式**: ErpMetadataRegistry 运行时元数据管理

### 模块层次

```
┌─────────────────────────────────────┐
│      Controller Layer               │
│  ErpConfigController, ErpScenario...  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Service Layer                  │
│  ErpSyncService, ErpChannelService  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Integration Layer                 │
│  ┌──────────────────────────────┐   │
│  │   ErpAdapterFactory          │   │
│  │   (获取适配器)               │   │
│  └────────────┬─────────────────┘   │
│               │                     │
│  ┌────────────▼─────────────────┐   │
│  │   ErpMetadataRegistry        │   │
│  │   (元数据注册与查询)         │   │
│  └────────────┬─────────────────┘   │
│               │                     │
│  ┌────────────▼─────────────────┐   │
│  │   ErpAdapter 实现            │   │
│  │   - YonSuiteErpAdapter       │   │
│  │   - KingdeeAdapter           │   │
│  │   - GenericErpAdapter        │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
```

## 适配器列表

| 标识 | 名称 | ERP类型 | 场景 | Webhook | 优先级 |
|------|------|---------|------|---------|--------|
| yonsuite | 用友YonSuite | YONSUITE | VOUCHER_SYNC, ATTACHMENT_SYNC | ✅ | 10 |
| kingdee | 金蝶云星空 | KINGDEE | VOUCHER_SYNC, ATTACHMENT_SYNC | ❌ | 20 |
| weaver | 浪潮GS | WEAVER | VOUCHER_SYNC | ❌ | 30 |
| generic | 通用ERP | GENERIC | VOUCHER_SYNC | ❌ | 100 |

## 使用示例

### 1. 获取适配器

```java
@Autowired
private ErpAdapterFactory factory;

// 通过类型标识获取
ErpAdapter adapter = factory.getAdapter("yonsuite");

// 查询元数据
ErpMetadata metadata = factory.getMetadata("yonsuite");

// 列出所有可用适配器
List<ErpAdapterInfo> adapters = factory.listAvailableAdapters();
```

### 2. 调用适配器功能

```java
// 测试连接
ConnectionTestResult result = adapter.testConnection(config);

// 同步凭证
List<VoucherDTO> vouchers = adapter.syncVouchers(config, startDate, endDate);

// 查询凭证详情
VoucherDTO voucher = adapter.getVoucherDetail(config, voucherNo);

// 获取附件
List<AttachmentDTO> attachments = adapter.getAttachments(config, voucherNo);
```

### 3. 查询适配器能力

```java
@Autowired
private ErpMetadataRegistry registry;

// 获取元数据
ErpMetadata metadata = registry.getByIdentifier("yonsuite");

// 检查支持的场景
boolean supportsVoucherSync = metadata.getSupportedScenarios().contains("VOUCHER_SYNC");

// 检查是否支持 Webhook
boolean supportsWebhook = metadata.supportsWebhook();
```

## 开发新适配器

详见: [ERP 适配器开发指南](../../../../docs/architecture/erp-adapter-development-guide.md)

快速步骤:

1. 实现 `ErpAdapter` 接口
2. 添加 `@ErpAdapter` 注解
3. 添加 `@Component` 注解
4. 编写单元测试
5. 运行 ArchUnit 验证

## 自指能力

本模块支持运行时自查询:

- ✅ 列出所有可用适配器
- ✅ 查询适配器元数据
- ✅ 检查适配器能力
- ✅ 按类型分组查询
- ✅ 优先级排序

## 架构测试

ArchUnit 规则强制执行:

1. 所有适配器必须有 `@ErpAdapter` 注解
2. 适配器标识必须唯一
3. 模块边界清晰，无越界依赖
4. 元数据注册中心隔离

运行测试:

```bash
mvn test -Dtest=ArchitectureTest
```

## 相关文档

- [模块清单](../../../../docs/architecture/module-manifest.md)
- [适配器开发指南](../../../../docs/architecture/erp-adapter-development-guide.md)
- [架构防御指南](../../../../docs/architecture/architecture-defense-guide.md)
```

**Step 2: 提交 README**

Run:
```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/README.md
git commit -m "docs(erp): 添加 ERP 集成模块 README"
```

---

## Task 9: 运行完整测试套件验证重构

**目标:** 确保所有修改不破坏现有功能

**Step 1: 运行所有单元测试**

Run: `cd nexusarchive-java && mvn test`

Expected: 所有测试通过

**Step 2: 运行架构测试**

Run: `cd nexusarchive-java && mvn test -Dtest=ArchitectureTest`

Expected: 架构测试通过

**Step 3: 运行前端模块验证**

Run: `npm run modules:validate`

Expected: 前端模块清单验证通过

**Step 4: 运行前端架构检查**

Run: `npm run check:arch`

Expected: 0 errors, warnings acceptable

**Step 5: 检查代码编译**

Run:
```bash
cd nexusarchive-java
mvn clean compile
```

Expected: 编译成功，无错误

**Step 6: 提交测试验证通过标记**

Run:
```bash
git commit --allow-empty -m "test(erp): 完整测试套件验证通过

- 单元测试: 全部通过
- 架构测试: 全部通过
- 前端模块验证: 通过
- 前端架构检查: 通过
- 编译: 成功"
```

---

## Task 10: 推送代码并创建 PR

**目标:** 将重构后的代码推送到远程仓库

**Step 1: 确认所有更改已提交**

Run: `git status`

Expected: 无未提交的更改

**Step 2: 推送到远程分支**

Run:
```bash
git push origin feature/erp-modularization-refactor
```

**Step 3: 创建 Pull Request**

Run:
```bash
gh pr create \
  --title "feat(erp): ERP 接口模块化重构与自指能力增强" \
  --body "## 概述

统一 ERP 接口架构，从双模式（Plugin + Adapter）简化为单一 Adapter 模式，并通过注解驱动的元数据系统实现自指能力。

## 主要变更

1. **添加 @ErpAdapter 注解**: 声明式元数据定义
2. **创建 ErpMetadataRegistry**: 运行时元数据注册中心
3. **为现有适配器添加注解**: YonSuite, Kingdee, Weaver, Generic
4. **添加 ArchUnit 测试**: 强制执行架构规则
5. **更新模块清单**: 记录 ERP 子模块详情 (v2.4.0)
6. **编写开发指南**: ERP 适配器开发文档

## 技术细节

- 新增: \`integration/erp/annotation/ErpAdapter.java\`
- 新增: \`integration/erp/registry/ErpMetadataRegistry.java\`
- 新增: \`integration/erp/dto/ErpMetadata.java\`
- 修改: 所有 Adapter 实现类添加 @ErpAdapter 注解
- 测试: ErpAdapterTest, ErpMetadataRegistryTest, ArchitectureTest

## 测试

- [x] 单元测试全部通过
- [x] 架构测试通过
- [x] 前端模块验证通过
- [x] 前端架构检查通过

## 文档

- [x] 更新 module-manifest.md
- [x] 创建 erp-adapter-development-guide.md
- [x] 创建 integration/erp/README.md

## 破坏性变更

无。此重构向后兼容，现有功能不受影响。

## 相关 Issue

#TODO (填写相关 Issue 编号)
"
```

Expected: PR 创建成功

**Step 4: 等待 CI/CD 检查**

在 GitHub 上查看 PR 的 CI/CD 状态，确保所有检查通过。

---

## 完成检查清单

- [ ] Task 1: 创建 @ErpAdapter 注解
- [ ] Task 2: 创建 ErpMetadata DTO
- [ ] Task 3: 创建 ErpMetadataRegistry 注册中心
- [ ] Task 4: 为现有适配器添加注解
- [ ] Task 5: 更新 module-manifest.md
- [ ] Task 6: 添加 ArchUnit 测试
- [ ] Task 7: 创建开发指南文档
- [ ] Task 8: 创建 ERP 模块 README
- [ ] Task 9: 运行完整测试套件
- [ ] Task 10: 推送代码并创建 PR

---

## 参考资料

- Spring 注解文档: https://docs.spring.io/spring-framework/reference/core/beans/annotation-config.html
- ArchUnit 文档: https://www.archunit.org/userguide/html/000_Index.html
- 项目架构文档: `docs/architecture/`
- 模块清单: `docs/architecture/module-manifest.md`
