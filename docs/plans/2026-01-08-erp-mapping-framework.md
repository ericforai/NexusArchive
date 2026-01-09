# ERP 差异隔离框架设计

> **模块化抽象的核心：隔离变化源头**
> 
> 创建日期: 2026-01-08
> 状态: 设计阶段
> 优先级: 高

---

## 问题背景

当前系统接入多个 ERP（YonSuite、金蝶、浪潮等），每个 ERP 的数据结构不同，字段映射逻辑散落在各个适配器中。新增一个 ERP 需要编写大量转换代码，维护成本高。

### 核心问题

```
YonSuite API → YonSuiteVoucherClient.convertToVoucherDTO() → VoucherDTO
金蝶 API → KingdeeAdapter.syncVouchers() → VoucherDTO (TODO)
浪潮 API → WeaverAdapter.syncVouchers() → VoucherDTO (TODO)
```

**问题**：
1. 字段映射逻辑散落各处，没有统一规则
2. VoucherDTO 字段名反映 YonSuite 术语，不是真正的 ERP 中立模型
3. 新增 ERP 需要手写大量转换代码

### 变化源头分析

| 变化维度 | 稳定性 |
|---------|-------|
| ERP 数据结构 | **经常变化**（核心问题） |
| 四性检测规则 | 基本不变 |
| 审批流程 | 基本不变 |
| AIP 封装格式 | 基本不变 |

**结论**：只需隔离 ERP 数据结构的差异。

---

## 设计方案

### 核心原则

1. **复用现有模型** - 使用 `AccountingSipDto` 作为统一的待归档凭证模型，不引入新概念
2. **配置化映射** - 用 YAML + Groovy 脚本声明字段映射，避免手写转换代码
3. **AI 自动生成** - AI 分析 OpenAPI 文档自动生成映射配置
4. **真实数据验证** - 用真实 API 数据自动测试，确保配置正确

### 架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        目标数据流                                    │
└─────────────────────────────────────────────────────────────────────┘

YonSuite API    金蝶 API    浪潮 API    未来 ERP...
      ↓              ↓           ↓           ↓
【AI 分析 OpenAPI → 生成 mapping.yml】
      ↓              ↓           ↓           ↓
 yonsuite.yml   kingdee.yml  weaver.yml   new-erp.yml
      ↓              ↓           ↓           ↓
        【统一映射框架 ErpMapper】
                  ↓
        AccountingSipDto (复用现有模型)
                  ↓
           现有流程（四性检测、审批、归档）
```

---

## 映射配置格式

### YAML 配置结构

```yaml
# kingdee-mapping.yml
sourceSystem: kingdee
targetModel: AccountingSipDto
version: 1.0.0

# 简单字段映射
mappings:
  header.voucherNumber:
    field: FNumber
  
  header.accountPeriod:
    script: |
      groovy:
        return ctx.FYear + '-' + String.format('%02d', ctx.FMonth as Integer)
  
  header.attachmentCount:
    field: FAttachments
    script: "groovy:return ctx.FAttachments?.size() ?: 0"

# 复杂对象转换
entries:
  source: FDetails
  item:
    lineNo:
      field: FLineNo
    summary:
      field: FExplanation
    accountCode:
      field: FAccountCode
    debit:
      field: FDebitAmount
      script: "groovy:return ctx.FDebitAmount ?: 0"
    credit:
      field: FCreditAmount
      script: "groovy:return ctx.FCreditAmount ?: 0"

attachments:
  source: FAttachments
  item:
    attachmentId:
      field: FID
    fileName:
      field: FFileName
    fileSize:
      field: FFileSize
    downloadUrl:
      script: |
        groovy:
          def baseUrl = config.getBaseUrl()
          return baseUrl + '/download?id=' + ctx.FID
```

### Groovy 脚本上下文

每个脚本可用的上下文变量：

| 变量 | 说明 |
|------|------|
| `ctx` | 当前 ERP 响应对象 |
| `config` | ErpConfig 配置对象 |
| `utils` | 工具类（日期格式化、字符串处理等） |

---

## 组件设计

### ErpMapper 接口

```java
public interface ErpMapper {
    /**
     * 将 ERP 响应转换为 AccountingSipDto
     * 
     * @param erpResponse ERP 原始响应
     * @param sourceSystem ERP 标识（kingdee/yonsuite/weaver）
     * @param config ERP 配置
     * @return 标准化的 SIP DTO
     */
    AccountingSipDto mapToSipDto(Object erpResponse, String sourceSystem, ErpConfig config);
    
    /**
     * 加载并验证映射配置
     */
    MappingConfig loadMapping(String sourceSystem);
}
```

### MappingConfig

```java
@Data
public class MappingConfig {
    private String sourceSystem;
    private String version;
    private Map<String, FieldMapping> mappings;
    private ObjectMapping entries;
    private ObjectMapping attachments;
}

@Data
public class FieldMapping {
    private String field;           // 源字段名
    private String script;          // Groovy 脚本
    private String type;            // 类型转换
    private String format;          // 格式化
}
```

### GroovyMappingEngine

```java
@Component
public class GroovyMappingEngine {
    
    private final GroovyShell shell;
    
    public Object executeScript(String script, Map<String, Object> context) {
        Binding binding = new Binding(context);
        return shell.evaluate(script, binding);
    }
}
```

---

## AI 生成流程

### 输入

- ERP OpenAPI/Swagger 文档 URL
- 示例响应数据（可选）

### 输出

- `erp-mapping-<system>.yml` 配置文件
- 对应的单元测试代码

### 生成策略

1. 分析 OpenAPI 文档，提取字段定义
2. 对比目标模型 `AccountingSipDto` 的字段
3. 按名称相似度匹配字段
4. 无法匹配的复杂字段生成 Groovy 脚本模板
5. 标记需要人工确认的部分

---

## 验证机制

### 自动测试

```java
@SpringBootTest
public class ErpMappingIntegrationTest {
    
    @Autowired
    private ErpMapper erpMapper;
    
    @Autowired
    private ErpAdapter erpAdapter;
    
    @Test
    void testKingdeeMapping() {
        // 1. 从真实 API 获取数据
        ErpConfig config = getTestConfig();
        List<Object> rawResponses = erpAdapter.fetchRawData(config);
        
        // 2. 执行映射
        List<AccountingSipDto> result = rawResponses.stream()
            .map(r -> erpMapper.mapToSipDto(r, "kingdee", config))
            .toList();
        
        // 3. 验证结果
        assertThat(result).isNotEmpty();
        result.forEach(dto -> {
            assertThat(dto.getHeader().getVoucherNumber()).isNotEmpty();
            assertThat(dto.getHeader().getAccountPeriod()).matches("\\d{4}-\\d{2}");
            assertThat(dto.getEntries()).isNotEmpty();
        });
    }
}
```

### CI/CD 集成

```
1. AI 生成/更新 mapping.yml
2. 自动运行测试
3. 测试通过 → 合并 PR
4. 测试失败 → 通知开发者
```

---

## 实施计划

### Phase 1: 框架基础

- [ ] 实现 `ErpMapper` 接口
- [ ] 实现 `GroovyMappingEngine`
- [ ] 实现 `MappingConfig` 加载器
- [ ] 编写单元测试

### Phase 2: YonSuite 迁移

- [ ] 将现有 `YonSuiteVoucherClient` 的转换逻辑提取为 `yonsuite-mapping.yml`
- [ ] 验证迁移后结果一致
- [ ] 移除硬编码转换逻辑

### Phase 3: 金蝶实现

- [ ] AI 生成 `kingdee-mapping.yml`
- [ ] 实现 `KingdeeAdapter` 的 `fetchRawData` 方法
- [ ] 用真实数据验证映射

### Phase 4: 浪潮及其他

- [ ] 重复金蝶的流程

### Phase 5: AI 生成器

- [ ] 实现 OpenAPI 文档解析
- [ ] 实现字段匹配算法
- [ ] 实现 Groovy 脚本生成
- [ ] 实现配置验证

---

## 文件清单

### 新增文件

```
nexusarchive-java/src/main/java/
├── com/nexusarchive/integration/erp/mapping/
│   ├── ErpMapper.java                  # 映射器接口
│   ├── GroovyMappingEngine.java        # 脚本执行引擎
│   ├── MappingConfig.java              # 配置模型
│   └── MappingConfigLoader.java        # 配置加载器
└── resources/erp-mapping/
    ├── yonsuite-mapping.yml            # YonSuite 映射配置
    ├── kingdee-mapping.yml             # 金蝶映射配置
    └── weaver-mapping.yml              # 浪潮映射配置

nexusarchive-java/src/test/java/
└── com/nexusarchive/integration/erp/mapping/
    └── ErpMappingIntegrationTest.java  # 集成测试
```

### 修改文件

```
# 现有适配器简化，只负责 API 调用
YonSuiteVoucherClient.java  → 移除 convertToVoucherDTO()
KingdeeAdapter.java          → 实现 fetchRawData() 方法
WeaverAdapter.java           → 实现 fetchRawData() 方法
```

---

## 风险与缓解

| 风险 | 缓解措施 |
|------|----------|
| Groovy 脚本安全性 | 使用沙箱限制可访问的类和方法 |
| 脚本调试困难 | 提供详细的错误信息和脚本执行日志 |
| AI 生成不准确 | 人工审核 + 自动测试双重验证 |
| 性能问题 | 缓存编译后的脚本，避免重复编译 |

---

## 附录：关键决策记录

### Q1: 为什么复用 AccountingSipDto 而不是新建 PendingVoucher？

**答**: 务实原则，少改动。AccountingSipDto 已有完整结构，职责清晰，无需引入新概念。

### Q2: 为什么选择 Groovy 脚本而不是纯表达式？

**答**: 灵活性。复杂转换（如字符串拼接、条件判断）用脚本更容易表达。

### Q3: 为什么用真实数据验证而不是 Sample 数据？

**答**: 真实性。真实数据最能发现问题，Sample 数据可能过时或不完整。

### Q4: 映射配置放在代码里还是外部？

**答**: 代码里（resources/erp-mapping/）。好处是版本控制、CI/CD 友好、便于测试。

---

## 变更历史

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2026-01-08 | 1.0.0 | 初始设计 |
