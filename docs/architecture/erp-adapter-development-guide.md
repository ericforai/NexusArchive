# ERP 适配器开发指南

## 概述

本文档描述如何开发新的 ERP 适配器，接入 NexusArchive 系统。

## 架构说明

### 核心组件

- **ErpAdapter 接口**: 所有适配器必须实现的核心接口
- **@ErpAdapterAnnotation 注解**: 声明式元数据定义
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
│   └── ErpAdapterAnnotation.java    # 元数据注解
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
import com.nexusarchive.integration.erp.annotation.ErpAdapterAnnotation;
import com.nexusarchive.integration.erp.dto.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@ErpAdapterAnnotation(
    identifier = "myerp",
    name = "我的ERP系统",
    description = "自定义 ERP 系统适配器",
    version = "1.0.0",
    erpType = "CUSTOM",
    supportedScenarios = {"VOUCHER_SYNC"},
    supportsWebhook = false,
    priority = 50
)
@Service("myerp")
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
    public List<com.nexusarchive.entity.ErpScenario> getAvailableScenarios() {
        // 返回支持的业务场景
        return List.of(
            com.nexusarchive.entity.ErpScenario.builder()
                .scenarioKey("VOUCHER_SYNC")
                .name("凭证同步")
                .syncStrategy("MANUAL")
                .isActive(true)
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
        ErpAdapterAnnotation annotation = MyErpAdapter.class
            .getAnnotation(ErpAdapterAnnotation.class);

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

### @ErpAdapterAnnotation 参数

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
@Service("myerp")
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
public ConnectionTestResult testConnection(ErpConfig config) {
    List<String> errors = new ArrayList<>();

    if (config.getUrl() == null || config.getUrl().isBlank()) {
        errors.add("ERP URL 不能为空");
    }

    if (config.getAppKey() == null) {
        errors.add("AppKey 不能为空");
    }

    return errors.isEmpty()
        ? ConnectionTestResult.success("连接成功")
        : ConnectionTestResult.failure("配置验证失败: " + String.join(", ", errors));
}
```

## 架构测试

ArchUnit 会自动验证以下规则：

1. ✅ 所有适配器必须有 `@ErpAdapterAnnotation` 注解
2. ✅ `@ErpAdapterAnnotation.identifier()` 必须唯一
3. ✅ 适配器层不能直接依赖服务层
4. ✅ 元数据注册中心只依赖注解和 DTO

违反这些规则会导致 CI/CD 失败。

## 参考实现

- `YonSuiteErpAdapter.java`: 用友 YonSuite 完整实现
- `KingdeeAdapter.java`: 金蝶云星空实现
- `GenericErpAdapter.java`: 通用适配器实现

## 故障排查

### 问题: 适配器未被发现

**检查:**
1. 是否添加了 `@Service` 注解且指定了 bean 名称？
2. 是否实现了 `ErpAdapter` 接口？
3. 是否添加了 `@ErpAdapterAnnotation` 注解？
4. `identifier` 是否唯一？

### 问题: ArchUnit 测试失败

**检查:**
1. 是否违反了模块边界规则？
2. 是否有循环依赖？
3. 运行 `mvn test -Dtest=ArchitectureTest` 查看详细错误

### 问题: 元数据未注册

**检查:**
1. 查看 `ErpMetadataRegistry` 日志
2. 确认 `@ErpAdapterAnnotation` 注解配置正确
3. 重启应用触发扫描

## 相关文档

- [模块清单](../module-manifest.md)
- [架构防御指南](../architecture-defense-guide.md)
