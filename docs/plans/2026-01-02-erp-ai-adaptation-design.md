# ERP AI 自适应系统设计文档

> **设计日期**: 2026-01-02
> **设计目标**: AI 驱动的 ERP 接口自动适配系统
> **核心理念**: 用户准备接口文件，AI 理解并生成适配器，自动集成到系统

---

## 一、问题定义

### 1.1 背景分析

**当前痛点**：
- ERP 集成是电子会计档案系统的核心功能
- 接入新 ERP 需要人工开发适配器，耗时数天
- 不同 ERP 接口差异大（REST/WebService/SDK）
- API 文档格式多样（Swagger/PDF/Markdown/代码示例）
- 维护成本高，API 变化需要人工跟进

**用户需求**：
- 用户手动准备 ERP 接口文件（小文件，< 1MB）
- 系统自动理解接口并生成适配器
- 自动集成到现有系统功能
- 运行时持续优化调用策略

### 1.2 核心场景

**主要场景**：
- **数据同步场景** - 定期从 ERP 拉取数据到档案系统
- **标准化流程** - 拉取→验证→存储的固定流程
- **接口差异** - 调用方式多样，数据结构相似
- **智能需求** - AI 理解 API 文档 + 持续学习优化

---

## 二、整体架构

### 2.1 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      用户交互层                              │
│  1. 上传接口文件 (Swagger + Markdown + 代码示例)             │
│  2. 确认生成的适配器                                        │
│  3. 监控运行状态                                            │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                   AI Agent 核心层                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ 文档理解 Agent │  │ 代码生成 Agent │  │ 优化学习 Agent│      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                  MCP Server 运行层                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ YonSuite MCP  │  │ Kingdee MCP   │  │ 新ERP MCP    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    现有系统集成层                           │
│  ErpSyncService → ArchiveBatchItem → 归档流程                │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                      学习反馈层                              │
│  记录调用成功率、错误模式、性能指标 → 持续优化调用策略       │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 设计原则

1. **智能与执行分离** - Agent 负责理解，MCP Server 负责执行
2. **渐进式增强** - MVP 先支持简单场景，逐步扩展
3. **用户可控** - 生成代码需确认后才部署
4. **向后兼容** - 与现有 Adapter 架构完全兼容
5. **可观测性** - 每个步骤都有日志和指标

---

## 三、核心流程

### 3.1 端到端流程

```
1. 用户上传接口文件
   ↓
2. Agent 文档理解
   - 识别文件格式
   - 提取 API 定义
   ↓
3. 业务语义映射（核心）
   - 接口做什么？（查询凭证/同步发票/...）
   - 匹配标准场景（VOUCHER_SYNC/INVOICE_SYNC/...）
   - 数据如何映射？（ERP字段 → 系统实体）
   ↓
4. 代码生成
   - 适配器类
   - DTO 类
   - 配置类
   - 测试类
   - SQL 配置
   ↓
5. 用户确认
   - 预览生成的代码
   - 查看集成配置
   ↓
6. 自动部署
   - 编译验证
   - 注册到 ErpAdapterFactory
   - 写入数据库配置
   - 运行测试
   ↓
7. 运行时学习
   - 记录调用指标
   - 优化调用策略
   - 持续改进
```

### 3.2 数据集成示例

```java
// 用户上传接口文档后，AI 生成适配器
@ErpAdapter(
    identifier = "new-erp",
    name = "新ERP系统",
    supportedScenarios = {"VOUCHER_SYNC", "INVOICE_SYNC"}
)
public class NewErpAdapter implements ErpAdapter {
    // AI 生成的实现
}

// 现有服务直接调用，无需修改
@Autowired
private ErpAdapterFactory factory;

List<VoucherDTO> vouchers = factory.getAdapter("new-erp")
    .syncVouchers(config, startDate, endDate);

// 自动进入标准档案流程
erpSyncService.syncToArchive(vouchers);
```

---

## 四、业务场景映射

### 4.1 完整场景列表

| ERP 接口意图 | 系统标准场景 | 数据处理 | 优先级 |
|-------------|-------------|---------|-------|
| **记账凭证类** |||
| 查询记账凭证列表 | VOUCHER_SYNC | 同步到 ArchiveBatchItem | P0 |
| 接收凭证推送 | VOUCHER_WEBHOOK | 触发归档流程 | P1 |
| 回写归档状态 | ARCHIVAL_FEEDBACK | 更新 ERP 状态 | P1 |
| **原始凭证类** |||
| 查询发票列表 | INVOICE_SYNC | 同步到 OriginalVoucher | P0 |
| 查询收据/单据 | RECEIPT_SYNC | 同步到 OriginalVoucher | P0 |
| 查询报销单 | EXPENSE_SYNC | 关联到记账凭证 | P1 |
| 查询合同列表 | CONTRACT_SYNC | 单独归档管理 | P1 |
| 查询银行回单 | BANK_STATEMENT_SYNC | 对账使用 | P1 |
| **关联关系类** |||
| 查询凭证-原始凭证关系 | VOUCHER_ATTACHMENT_MAPPING | 建立关联索引 | P0 |
| 查询附件列表 | ATTACHMENT_SYNC | 下载并存储文件 | P0 |
| **辅助核算类** |||
| 查询科目余额 | ACCOUNT_QUERY | 三位一体核对 | P2 |
| 查询辅助核算项 | DIMENSION_QUERY | 部门/项目/人员 | P2 |
| **电子发票类** |||
| 查询电子发票真伪 | E_INVOICE_VERIFY | 验真并存储 | P1 |
| 接收发票推送通知 | E_INVOICE_WEBHOOK | 自动归档 | P1 |

### 4.2 数据实体映射

```java
// 记账凭证
ACCOUNTING_VOUCHER("记账凭证", "ArchiveBatchItem")

// 原始凭证
ORIGINAL_VOUCHER("原始凭证", "OriginalVoucher")
INVOICE("发票", "Invoice")
RECEIPT("收据", "Receipt")
CONTRACT("合同", "Contract")
EXPENSE_REPORT("报销单", "ExpenseReport")
BANK_STATEMENT("银行回单", "BankStatement")

// 关联关系
VOUCHER_ATTACHMENT("凭证附件关系", "VoucherAttachmentMapping")

// 辅助数据
ACCOUNT_BALANCE("科目余额", "AccountBalance")
DIMENSION("辅助核算", "Dimension")
```

---

## 五、AI Agent 组件设计

### 5.1 ErpAdaptationAgent

```java
@Service
public class ErpAdaptationAgent {

    public AdaptationResult adaptErp(
        MultipartFile[] files,
        ErpBasicInfo info
    ) {
        // Step 1: 文档理解
        DocumentUnderstandingResult docResult =
            documentUnderstander.analyze(files);

        // Step 2: API 提取
        List<ApiDefinition> apis =
            apiExtractor.extract(docResult);

        // Step 3: 业务语义映射
        BusinessMappingResult mapping =
            businessMapper.mapToStandardScenarios(apis, info);

        // Step 4: 代码生成
        GeneratedCode code =
            codeGenerator.generate(mapping);

        // Step 5: 配置生成
        IntegrationConfig config =
            configGenerator.generate(code, info);

        // Step 6: 测试验证
        ValidationResult validation =
            testingValidator.validate(code);

        return AdaptationResult.builder()
            .code(code)
            .config(config)
            .validation(validation)
            .build();
    }
}
```

### 5.2 BusinessSemanticMapper（核心智能）

```java
@Service
public class BusinessSemanticMapper {

    private ApiIntent understandApiIntent(ApiDefinition api) {
        // 使用 LLM 分析接口的业务意图
        return LlmAnalyzer.analyze(api, """
            分析这个 ERP 接口的业务意图：

            1. 它在做什么操作？（查询/同步/提交/回调）
            2. 操作的对象是什么？（凭证/发票/收据/合同/...）
            3. 调用时机？（定时拉取/事件触发/实时查询）
            4. 数据流向？（ERP → 系统 / 系统 → ERP）

            返回标准化的意图描述。
        """);
    }

    private StandardScenario matchScenario(ApiIntent intent) {
        // 匹配到系统标准场景
        return scenarioRegistry.match(intent);
    }
}
```

---

## 六、代码生成策略

### 6.1 生成内容

| 组件 | 说明 | AI 能力 |
|------|------|---------|
| Adapter 类 | 主适配器实现 | ✅ 代码生成 |
| DTO 类 | 数据传输对象 | ✅ 根据接口生成 |
| Config 类 | 配置类 | ✅ 模板填充 |
| Test 类 | 单元测试 | ✅ 测试生成 |
| SQL 配置 | 数据库配置 | ✅ 模板填充 |

### 6.2 代码示例

```java
@ErpAdapter(
    identifier = "some-erp",
    name = "某ERP系统",
    supportedScenarios = {"VOUCHER_SYNC", "INVOICE_SYNC"}
)
@Component
public class SomeErpAdapter implements ErpAdapter {

    // AI 根据接口文档生成实现
    @Override
    public List<VoucherDTO> syncVouchers(ErpConfig config,
                                         LocalDate startDate,
                                         LocalDate endDate) {
        // AI 生成的调用逻辑
        String url = config.getBaseUrl() + "/api/v1/vouchers";
        SomeErpVoucherResponse response = restTemplate.getForObject(
            url + "?startDate={start}&endDate={end}",
            SomeErpVoucherResponse.class,
            startDate, endDate
        );

        // AI 生成的数据映射
        return response.getData().stream()
            .map(this::mapToVoucherDTO)
            .collect(Collectors.toList());
    }
}
```

---

## 七、学习反馈机制

### 7.1 学习维度

| 学习目标 | 学习内容 | 应用 |
|---------|---------|------|
| **成功率优化** | 哪些时间段调用成功率最高？ | 动态调整同步时间窗口 |
| **性能优化** | 批量大小多少最快？ | 优化分页参数 |
| **重试策略** | 哪些错误可以重试？重试几次？ | 智能重试机制 |
| **超时设置** | 平均响应时间是多少？ | 动态调整超时 |
| **数据质量** | 哪些字段经常为空？ | 提前过滤无效数据 |

### 7.2 实现机制

```java
@Component
public class MetricsCollector {

    // 统计学习（从简单开始）
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);

    public void record(boolean success, long responseTimeMs) {
        if (success) successCount.incrementAndGet();
        else failureCount.incrementAndGet();
        totalResponseTime.addAndGet(responseTimeMs);
    }

    public double getSuccessRate() {
        long total = successCount.get() + failureCount.get();
        return total == 0 ? 0 : (double) successCount.get() / total;
    }
}
```

---

## 八、技术栈

### 8.1 核心技术

```
AI 能力层：
  - Claude API / OpenAI GPT-4 (文档理解、代码生成)
  - LangChain (Agent 编排)
  - Embedding (语义相似度匹配)

MCP Server 层：
  - MCP SDK (官方 Server 框架)
  - Spring Boot (现有技术栈)

文件处理层：
  - Swagger Parser (OpenAPI 解析)
  - Apache POI (Word/Excel 解析)
  - PDFBox (PDF 解析)
  - Jsoup (HTML 爬取)

数据持久层：
  - PostgreSQL (现有)
  - Redis (学习指标缓存)
  - SQLite (Agent 记忆存储)

学习优化层：
  - Micrometer (指标收集)
  - 自统计算引擎 (统计学习)
```

---

## 九、实施路径

### 9.1 阶段规划

**第一阶段：MVP（2-3周）**
- ✅ 支持最简单的场景：OpenAPI JSON 文件
- ✅ 只生成记账凭证同步代码
- ✅ 基础测试验证
- ✅ 人工审核后部署

**第二阶段：增强（1-2月）**
- ✅ 支持多种文件格式（PDF、Markdown、代码示例）
- ✅ 扩展到原始凭证（发票、收据等）
- ✅ 添加基础学习能力（成功率、响应时间）
- ✅ 自动化测试和部署

**第三阶段：智能化（3-6月）**
- ✅ 支持在线文档爬取
- ✅ 高级学习策略（优化调用时机）
- ✅ 自动发现 API 变化
- ✅ 异常检测和自动修复

### 9.2 MVP 功能清单

| 功能 | 优先级 | MVP |
|------|-------|-----|
| 上传 OpenAPI JSON | P0 | ✅ |
| 理解接口定义 | P0 | ✅ |
| 识别凭证同步场景 | P0 | ✅ |
| 生成适配器代码 | P0 | ✅ |
| 生成单元测试 | P1 | ✅ |
| 代码预览和确认 | P1 | ✅ |
| 自动编译验证 | P1 | ✅ |
| 部署到系统 | P2 | ✅ |
| 记录运行指标 | P2 | ⏳ |
| 基础学习优化 | P3 | ⏳ |

---

## 十、关键设计决策

### 10.1 为什么选择 MCP + Agent 混合架构？

**MCP Server 的优势**：
- ✅ 标准化的工具接口规范
- ✅ 易于集成和调用
- ✅ 支持多种传输协议
- ✅ 社区生态成熟

**Agent Skills 的优势**：
- ✅ 强大的理解和推理能力
- ✅ 灵活的代码生成
- ✅ 持续学习和优化
- ✅ 处理复杂业务逻辑

**结合的价值**：
- Agent 负责"理解"（文档解析、代码生成、策略优化）
- MCP Server 负责"执行"（标准化 API 调用、数据同步）
- 清晰的职责分离，各司其职

### 10.2 为什么从简单开始？

**设计原则**：
1. **YAGNI** - 不做当前不需要的功能
2. **快速验证** - MVP 尽快验证核心价值
3. **渐进增强** - 成功后再扩展功能
4. **风险控制** - 分阶段实施，降低失败风险

---

## 十一、成功指标

### 11.1 效率指标

| 指标 | 当前 | 目标 | 提升 |
|------|------|------|------|
| 新 ERP 接入时间 | 3-5 天 | 2-4 小时 | **90%+** |
| 代码开发工作量 | 100% | 10% | **90%** |
| 维护响应时间 | 1-2 天 | 自动 | **95%** |

### 11.2 质量指标

| 指标 | 目标 |
|------|------|
| 代码生成可用率 | > 80% |
| 测试覆盖率 | > 70% |
| 首次部署成功率 | > 60% |
| 学习优化效果 | 成功率提升 20%+ |

---

## 十二、风险与缓解

### 12.1 技术风险

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| AI 理解错误 | 生成错误代码 | 人工审核 + 测试验证 |
| API 文档不规范 | 无法准确提取 | 用户预处理 + 模板引导 |
| 生成代码质量低 | 运行时错误 | 静态检查 + 单元测试 |

### 12.2 业务风险

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 用户接受度低 | 不愿使用 | 渐进式推广 + 充分培训 |
| 维护成本高 | 难以维护 | 清晰架构 + 充分文档 |
| 性能问题 | 响应慢 | 性能测试 + 持续优化 |

---

## 十三、下一步

### 13.1 立即行动

1. ✅ 设计文档已确认
2. ⏳ 创建实施工作计划
3. ⏳ 搭建开发环境
4. ⏳ 开始 MVP 开发

### 13.2 需要确认的问题

1. **首选 ERP** - 选择哪个 ERP 作为第一个目标？
   - 建议：API 文档最规范的 ERP（如有 Swagger）
2. **文件格式** - 用户主要准备什么格式的接口文件？
   - 这影响优先级
3. **部署方式** - 自动部署还是人工审核后部署？
   - MVP 建议：人工审核
4. **学习粒度** - 记录哪些指标？
   - MVP 建议：成功率、响应时间

---

**设计完成日期**: 2026-01-02
**设计者**: Claude (AI Architecture Agent)
**版本**: v1.0
