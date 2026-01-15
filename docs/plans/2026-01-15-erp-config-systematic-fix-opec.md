# ErpConfig 模块系统性修复 OPEC

**日期**: 2026-01-15
**问题类别**: 敏感数据泄露 + 架构设计缺陷
**严重程度**: P0（敏感信息可能通过 API 泄露）
**状态**: 已完成

---

## 一、问题背景

### 1.1 初始问题

用户在测试 YonSuite 连接器时发现：
- API 响应中包含加密后的 `appSecret`
- `testConnection()` 方法无法正常工作
- ErpConfig 实体缺少部分 getter 方法

### 1.2 根本原因分析

通过洋葱模型排查，发现是**多层架构问题叠加**：

```
第一层：Entity 设计问题
   └─ configJson 存储敏感数据，但缺少对应 getter 方法

第二层：Service 层职责混乱
   └─ findById() 和 getByIdForInternalUse() 区分不明显

第三层：类型不匹配
   └─ entity.ErpConfig ≠ dto.ErpConfig（两个同名类）
   └─ Adapter 期望 dto.ErpConfig，但 Controller 传入 entity.ErpConfig

第四层：缺少编译时验证
   └─ 添加新字段时没有强制检查对应 getter 方法
```

---

## 二、解决方案

### 2.1 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                        API 层                                │
│  Controller → 返回 ErpConfigDto（已清理敏感信息）              │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                       Service 层                             │
│  ┌─────────────────────┐    ┌─────────────────────┐        │
│  │   API 方法          │    │   内部方法           │        │
│  │ - getConfigs()      │    │ - getByIdForInternalUse() │  │
│  │ - getConfig(id)     │    │ - getAllConfigs()    │        │
│  │  返回 ErpConfigDto   │    │  返回 ErpConfig      │        │
│  └─────────────────────┘    └─────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                       Entity 层                              │
│  ErpConfig (数据库实体) + ErpConfigDto (API 响应)            │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 关键设计原则

| 原则 | 实现 |
|------|------|
| **类型分离** | API 返回 DTO，内部使用 Entity |
| **自动清理** | ErpConfigApiDtoBuilder 自动过滤敏感字段 |
| **命名约定** | `getXxx()` = API 方法，`getXxxForInternalUse()` = 内部方法 |
| **编译时验证** | ArchUnit 测试确保架构规则 |

---

## 三、实施步骤

### Step 1: 创建 DTO 层

**文件**: `dto/ErpConfigDto.java`

\`\`\`java
@Data
@Builder
public class ErpConfigDto {
    private Long id;
    private String name;
    private String erpType;
    private Map<String, Object> sanitizedConfig;  // 已清理敏感信息
    private Integer isActive;
    // ... 其他字段
}
\`\`\`

**文件**: `dto/ErpConfigApiDtoBuilder.java`

\`\`\`java
public final class ErpConfigApiDtoBuilder {
    private static final String[] SENSITIVE_FIELDS = {
        "appSecret", "clientSecret", "appSecret_encrypted",
        "clientSecret_encrypted", "password", "token", "privateKey"
    };

    public static ErpConfigDto toDto(ErpConfig entity) {
        Map<String, Object> sanitized = sanitizeConfigJson(entity.getConfigJson());
        return ErpConfigDto.builder()
            .sanitizedConfig(sanitized)
            // ... 复制其他字段
            .build();
    }
}
\`\`\`

### Step 2: 更新 Service 接口

**文件**: `service/ErpConfigService.java`

\`\`\`java
public interface ErpConfigService {
    // ========== API 方法（返回 DTO） ==========
    List<ErpConfigDto> getConfigs();
    List<ErpConfigDto> getConfigsByErpType(String erpType);
    ErpConfigDto getConfig(Long configId);

    // ========== 内部方法（返回 Entity） ==========
    @Deprecated
    List<ErpConfig> getAllConfigs();
    ErpConfig getByIdForInternalUse(Long configId);
    // ...
}
\`\`\`

### Step 3: 更新 Service 实现

**文件**: `service/impl/ErpConfigServiceImpl.java`

\`\`\`java
@Override
public List<ErpConfigDto> getConfigs() {
    List<ErpConfig> configs = erpConfigMapper.selectList(null);
    return configs.stream()
        .map(ErpConfigApiDtoBuilder::toDto)
        .collect(Collectors.toList());
}
\`\`\`

### Step 4: 更新 Controller

**文件**: `controller/ErpConfigController.java`

\`\`\`java
// API 响应使用 DTO
@GetMapping
public Result<List<ErpConfigDto>> list() {
    return Result.success(erpConfigService.getConfigs());
}

// testConnection 使用 DTO 转换
@PostMapping("/{id}/test")
public Result<Map<String, Object>> testConnection(@PathVariable Long id) {
    ErpConfig config = erpConfigService.getByIdForInternalUse(id);

    // 转换为适配器 DTO（包含解密后的敏感信息）
    com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig =
        erpConfigDtoBuilder.buildDtoConfig(config);

    ConnectionTestResult result = adapter.testConnection(dtoConfig);
    // ...
}
\`\`\`

### Step 5: 添加 ArchUnit 测试

**文件**: `architecture/ErpConfigConsistencyTest.java`

\`\`\`java
@Tag("architecture")
public class ErpConfigConsistencyTest {

    private static final Set<String> REQUIRED_CONFIG_FIELDS = Set.of(
        "baseUrl", "appKey", "appSecret", "clientSecret",
        "accbookCode", "extraConfig"
    );

    @Test
    void erpConfigMustProvideGettersForAllFields() {
        // 验证每个字段都有对应的 getter 方法
    }

    @Test
    void erpConfigServiceMustSeparateApiAndInternalMethods() {
        // 验证 Service 层区分 API 和内部方法
    }
}
\`\`\`

---

## 四、经验教训

### 4.1 核心原则

1. **单一事实来源 (SSOT)**
   - 敏感数据只能通过专门的内部方法获取
   - API 响应必须经过清理

2. **类型安全优于文档约定**
   - 使用不同的类型（DTO vs Entity）强制区分用途
   - 而不是依赖文档或命名约定

3. **编译时验证优于运行时错误**
   - ArchUnit 测试在编译时发现问题
   - 而不是等到运行时才发现 NullPointerException

### 4.2 命名约定

| 用途 | 方法命名 | 返回类型 |
|------|----------|----------|
| API 响应 | `getConfigs()` | `List<ErpConfigDto>` |
| 内部使用 | `getByIdForInternalUse()` | `ErpConfig` |
| 已废弃 | `getAllConfigs()` | `List<ErpConfig>` |

### 4.3 防止未来错误的清单

- [ ] 添加新 configJson 字段时，同时添加 getter 方法
- [ ] 新增 API 方法时，返回 DTO 而非 Entity
- [ ] 敏感操作（如连接测试）使用内部方法
- [ ] 运行 ArchUnit 测试验证架构规则

---

## 五、测试验证

\`\`\`bash
# 运行架构测试
mvn test -Dtest=ErpConfigConsistencyTest

# 结果
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
\`\`\`

---

## 六、相关文件清单

### 新增文件
- `dto/ErpConfigDto.java`
- `dto/ErpConfigApiDtoBuilder.java`
- `architecture/ErpConfigConsistencyTest.java`

### 修改文件
- `entity/ErpConfig.java` - 添加 getExtraConfig()
- `service/ErpConfigService.java` - 新增 DTO 方法
- `service/impl/ErpConfigServiceImpl.java` - 实现 DTO 方法
- `controller/ErpConfigController.java` - 使用 DTO 响应

### 相关文件（未修改）
- `service/erp/ErpConfigDtoBuilder.java` - 现有的适配器 DTO 转换器
- `integration/erp/dto/ErpConfig.java` - 适配器使用的 DTO

---

## 七、后续建议

1. **逐步迁移**
   - 将其他使用 `getAllConfigs()` 的地方迁移到 `getConfigs()`
   - 最终可以移除旧的 Entity 返回方法

2. **扩展 ArchUnit 规则**
   - 为其他类似模块（如 FondsConfig）添加相同的测试
   - 创建通用的敏感数据保护规则

3. **文档更新**
   - 更新 API 文档，明确说明哪些字段会被清理
   - 在开发者文档中强调 DTO vs Entity 的使用场景
