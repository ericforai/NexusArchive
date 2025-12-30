# AI Integration Agent Strategy: "The Connector Factory"

## 1. 核心理念 (Core Concept)

**"不要为了喝牛奶而养奶牛，而是让 AI 帮你建一个自动化养牛场。"**

传统的对接方式是：每个系统 -> 分析文档 -> 手写代码 -> 调试 -> 部署。
**AI Agent 模式**是：Interface Docs (文档) -> **AI Agent** -> Connector Code (代码/脚本) -> Hot Load (热加载)。

您不再需要手动编写 Java 代码，而是作为**"Prompt Engineer"** 指挥 AI 生成符合 `ErpAdapter` 标准的代码。

---

## 2. 架构设计 (Architecture)

### 2.1 标准化抽象 (The "Socket")

为了让 AI 能批量生产 Connector，我们必须先定义好“插座”的标准。您现有的 `ErpAdapter` 接口已经非常完美，只需进一步细化：

*   **Capabilities**:
    *   `Map<String, Object> authenticate(config)`: 解决各种奇葩认证 (Cookie, OAuth, SoapHeader)。
    *   `List<VoucherDTO> pullData(context)`: 统一拉取逻辑。
    *   `DataMapper`: 将异构 JSON 映射为标准 DTO。

### 2.2 Agent 工作流 (The Workflow)

我们设计一个 **"Connector Forge" (连接器锻造工厂)**，包含三个步骤：

#### Step 1: 知识摄入 (Context Ingestion)
用户提供接口文档 (PDF/Word/Swagger/Postman)。
*   **Prompt**: "阅读这份《金蝶 K3Cloud 接口文档.pdf》，提取关于 '凭证查询' 和 '登录认证' 的所有字段定义和 URL 路径。"

#### Step 2: 语义映射 (Semantic Mapping)
AI 理解文档并将字段映射到我们的标准模型 (`VoucherDTO`)。
*   **AI Thinking**: "金蝶的 `FBillHead.FDate` 对应我们系统的 `voucherDate`。`FDetail.FAmount` 对应 `debitAmount`。"
*   **Output**: 生成一份 Mapping JSON 或伪代码。

#### Step 3: 代码生成 (Code Generation)
AI 根据 Mapping 生成可执行代码。这里有两种技术路线：

> **路线 A: 静态代码生成 (Current Best Practice)**
> *   AI 直接生成 `KingdeeAdapter.java` 源码。
> *   您 Copy 进项目，编译运行。
> *   **优点**: 性能高，调试方便，利用现有 Spring 容器。
> *   **缺点**: 需要重启服务。

> **路线 B: 动态脚本 (Dynamic Scripting - Future)**
> *   AI 生成 `Groovy` 或 `JavaScript (Nashorn/GraalVM)` 脚本。
> *   系统内置 ScriptEngine，运行时加载脚本。
> *   **优点**: **零停机**，随时上传文档生成脚本即刻运行。适合 SaaS 模式。

---

## 3. 实战演练：以金蝶/泛微为例 (Case Study)

### 场景 A: 对接金蝶云星空 (Kingdee K3 Cloud)
*   **难点**: 它的认证是基于 Cookie 的 (SessionId)，而不是 Token。且查询参数是经过 JSON 序列化后再 Base64 或者直接 JSON 字符串传参。
*   **AI 策略**:
    *   你把 `KingdeeAdapter.java` 的 TODO 给 AI。
    *   扔给 AI 一段金蝶 SDK 的 C# 或 Java 示例代码。
    *   指令："将这段 Login 逻辑转换为 Java Hutool HTTP 调用，并实现 Cookie 保持。"

### 场景 B: 对接泛微 OA (Weaver Ecology / E-teams)
*   **难点**: OA 没有"凭证"的概念，只有"流程 (Workflow)"。数据分散在 `formtable_main_x` 和 `formtable_dt_x` 表里。
*   **AI 策略**:
    *   指令："这是一个'费用报销流程'的表单结构 JSON。请写一个 SQL 或 API 调用，把'申请日期'映射为'凭证日期'，把'报销金额'映射为'借方金额'。"

---

## 4. 落地建议 (Action Plan)

鉴于您是创业公司：

1.  **短期 (Now)**: 采用 **"AI 辅助编程模式" (Copilot Mode)**。
    *   保持 `ErpAdapter` 接口不变。
    *   每当有新客户用新 ERP，直接把文档扔给 AI (比如我)，让我生成 `XxxAdapter.java`。
    *   您 Review 后合入代码。这是最稳妥、成本最低的方式。

2.  **中期 (3-6 Months)**: 开发 **"通用脚本适配器" (`ScriptedErpAdapter`)**。
    *   在系统中集成 Groovy 引擎。
    *   让 AI 生成 Groovy 脚本存储在数据库中。
    *   实现"上传脚本文档 -> AI 生成脚本 -> 热加载运行"的闭环。

---

## 5. 关键挑战与落地问答 (Crucial Q&A)

针对您提出的"Agent模式"、"文档异构性"等核心疑虑，以下是深度技术拆解：

### Q1: 这是一个 "Agent" 吗？还是仅仅是工作流？
**A: 这是一个 "Dev-Time Assistant Agent" (开发期辅助智能体)。**

我们必须区分两种形态：
*   **Runtime Agent (运行时智能体)**：部署在客户服务器，自动读PDF、自动写代码并热加载。
    *   *风险*：**极大**。如果 AI 误解文档导致删库怎么办？在企业级私有化部署中，稳定性是红线。
    *   *结论*：**严禁**在初期做成全自动 Runtime Agent。
*   **Dev-Time Agent (开发时智能体)**：运行在您的电脑或内部中台。
    *   *模式*：Input (文档) -> Agent Interaction (确认) -> Output (Java Code)。
    *   *本质*：它是一个**"交互式超级脚手架"**。它赋予了您"无限的初级程序员"能力。您 Review 代码后，再发布给客户。

### Q2: 接口文档五花八门（图片/PDF/Word/Excel），AI 怎么读？
**A: 核心战术 —— "双过降维策略" (The Two-Pass Strategy)。**

不要指望 AI 能"一步到位"直接从乱七八糟的 PDF生成完美的 Java 代码（幻觉率极高）。我们必须把过程拆解：

#### Pass 1: 归一化 (Normalization Phase)
**目标**：把五花八门的文档（OCR 截图、Word 表格）清洗为统一的 **SIM JSON** (Standard Integration Metadata)。

*   **工具**：Gemini 1.5 Pro / GPT-4o (利用其超强多模态能力)。
*   **Prompt**: "你是一个数据分析师。请阅读附件（可能是图片或文本），提取以下关键信息并填入 JSON：Login URL, Token Key, Voucher Endpoint, Field Mapping..."
*   **Output**: 一个标准化的 `kingdee_k3_cloud.json`。
    *   *优势*：这一步**只做提取，不做编程**。容错率高。人类 Review JSON 比 Review 代码容易得多。

##### SIM JSON Schema 定义 (Standard Integration Metadata Schema)

为确保 Pass 1 输出的一致性和可验证性，所有 SIM JSON **必须**符合以下 Schema：

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "SIM JSON Schema",
  "description": "Standard Integration Metadata for ERP Adapter Generation",
  "type": "object",
  "required": ["erpType", "version", "authentication", "endpoints"],
  "properties": {
    "erpType": {
      "type": "string",
      "description": "ERP系统标识符",
      "enum": ["KINGDEE_K3", "KINGDEE_CLOUD", "YONSUITE", "SAP", "ORACLE", "WEAVER", "OTHER"]
    },
    "version": {
      "type": "string",
      "description": "SIM Schema 版本号",
      "pattern": "^\\d+\\.\\d+\\.\\d+$"
    },
    "metadata": {
      "type": "object",
      "properties": {
        "docSource": { "type": "string", "description": "原始文档来源/文件名" },
        "extractedAt": { "type": "string", "format": "date-time" },
        "extractedBy": { "type": "string", "description": "提取所用的 AI 模型" },
        "confidence": { "type": "number", "minimum": 0, "maximum": 1, "description": "AI 提取置信度" }
      }
    },
    "authentication": {
      "type": "object",
      "required": ["type", "loginUrl"],
      "properties": {
        "type": {
          "type": "string",
          "enum": ["OAUTH2", "API_KEY", "COOKIE_SESSION", "SOAP_HEADER", "BASIC_AUTH", "CUSTOM"]
        },
        "loginUrl": { "type": "string", "format": "uri" },
        "tokenUrl": { "type": "string", "format": "uri" },
        "refreshUrl": { "type": "string", "format": "uri" },
        "headers": {
          "type": "object",
          "additionalProperties": { "type": "string" }
        },
        "params": {
          "type": "object",
          "description": "认证所需的参数模板",
          "additionalProperties": { "type": "string" }
        },
        "tokenPath": { "type": "string", "description": "从响应中提取 Token 的 JSONPath" },
        "expiresInPath": { "type": "string", "description": "Token 过期时间的 JSONPath" }
      }
    },
    "endpoints": {
      "type": "array",
      "minItems": 1,
      "items": {
        "type": "object",
        "required": ["name", "url", "method", "purpose"],
        "properties": {
          "name": { "type": "string", "description": "接口名称" },
          "url": { "type": "string" },
          "method": { "type": "string", "enum": ["GET", "POST", "PUT", "DELETE"] },
          "purpose": {
            "type": "string",
            "enum": ["VOUCHER_LIST", "VOUCHER_DETAIL", "PAYMENT_LIST", "INVOICE_LIST", "FILE_DOWNLOAD", "OTHER"]
          },
          "requestFormat": { "type": "string", "enum": ["JSON", "FORM", "XML", "QUERY_STRING"] },
          "requestTemplate": { "type": "object", "description": "请求体模板" },
          "responseDataPath": { "type": "string", "description": "响应中数据列表的 JSONPath" },
          "pagination": {
            "type": "object",
            "properties": {
              "type": { "type": "string", "enum": ["PAGE_NUMBER", "OFFSET", "CURSOR", "NONE"] },
              "pageParam": { "type": "string" },
              "sizeParam": { "type": "string" },
              "defaultSize": { "type": "integer" }
            }
          }
        }
      }
    },
    "fieldMappings": {
      "type": "array",
      "description": "字段映射规则",
      "items": {
        "type": "object",
        "required": ["sourceField", "targetField"],
        "properties": {
          "sourceField": { "type": "string", "description": "ERP 原始字段路径 (JSONPath)" },
          "targetField": { "type": "string", "description": "目标 DTO 字段名" },
          "transform": {
            "type": "string",
            "enum": ["DIRECT", "DATE_FORMAT", "NUMBER_SCALE", "ENUM_MAP", "CONCAT", "CUSTOM"],
            "default": "DIRECT"
          },
          "transformConfig": { "type": "object", "description": "转换配置" }
        }
      }
    },
    "knownIssues": {
      "type": "array",
      "description": "已知的坑和注意事项",
      "items": { "type": "string" }
    }
  }
}
```

##### SIM JSON 验证清单 (Validation Checklist)

在 Pass 1 完成后，**必须**进行以下人工 Review：

| 检查项 | 必填 | 验证方式 |
|--------|------|----------|
| `erpType` 是否正确 | ✅ | 与文档标题/来源核对 |
| `authentication.type` 是否匹配文档描述 | ✅ | 查找文档中"认证"/"登录"章节 |
| `authentication.loginUrl` 是否可访问 | ✅ | 用 curl 测试连通性 |
| `endpoints` 至少包含一个业务接口 | ✅ | 确认有凭证/付款/发票相关接口 |
| `fieldMappings` 是否覆盖核心字段 | ✅ | 对照 VoucherDTO 检查 |
| `metadata.confidence` < 0.8 的字段是否已人工确认 | ⚠️ | 重点复核低置信度提取 |
| `knownIssues` 是否记录了文档中的特殊说明 | ⚠️ | 检查文档备注/注意事项 |

#### Pass 2: 代码生成 (Code Generation Phase)
**目标**：把 SIM JSON 转换为 `Java` 代码。

*   **工具**：任何编程 LLM (Claude 3.5 Sonnet / DeepSeek Coder)。
*   **Input**: `kingdee_k3_cloud.json` + `ErpAdapter.java` (模板)。
*   **Output**: 100% 可编译运行的 Java 代码。
    *   *优势*：因为输入是结构化的 JSON，代码生成的准确率接近 100%。

### Q3: 这算是 Agent 吗？
**算的。** 当 Pass 1 遇到模糊信息时（例如"这里有两个登录接口，用哪个？"），它会**反问**用户。这种"主动感知不确定性并寻求人类反馈"的能力，就是 Agent 的核心特征。

---

## 6. 最终建议 (Final Recommendation)

对于现在的 nexusarchive 项目：

1.  **Do Not Over-Engineer**: 不要急着写真正的 Agent 软件平台。
2.  **Copilot First**: 建立您的 "Prompt Library"。
    *   Prompt 1: "文档清洗器 (Doc -> JSON)"。
    *   Prompt 2: "代码生成器 (JSON -> Java)"。
3.  **Human-in-the-Loop**: 始终保持"文档 -> AI -> **人(Review)** -> 代码"的流程。这才是对客户负责的态度。

---

## 7. SOP: 从 Mock 到 Production 的落地流程 (Real-World Integration SOP)

**[新增章节 2025-12-17]**
基于 YonSuite 对接实战经验，以下是 "Generated Code" -> "Production Ready" 的必经步骤。以后所有对接工作**必须**严格遵循此 SOP。

### 7.1 "最后一公里"：真实 Token 对接 (The Token Handshake)
AI 生成的代码 (Pass 2) 通常包含 `mock_token` 占位符。**绝对禁止**带着 Mock Token 上线。
必须将其替换为系统内建的认证服务 calls。

**步骤：**
1.  **依赖注入 (Dependency Injection)**:
    在生成的 Service 类中注入 `YonAuthService` (或其他 ERP 的 AuthService)。
    ```java
    @Service
    @RequiredArgsConstructor
    public class YonPaymentListService {
        private final YonAuthService yonAuthService; // [ACTION] Inject This
    ```

2.  **逻辑替换 (Replace Logic)**:
    查找 `getAccessToken` 或 mock 字符串，替换为真实调用。
    ```java
    // [BEFORE] AI Generated
    String accessToken = "mock_token";
    
    // [AFTER] Integrated
    // 自动处理：缓存(Cache)、过期刷新(Refresh)、签名计算(Sign)
    String accessToken = yonAuthService.getAccessToken(config.getAppKey(), config.getAppSecret());
    ```

### 7.2 配置注入 (Credential Seeding)
AppKey / AppSecret 等敏感信息**严禁硬编码**在 Java 代码中。必须通过数据库配置注入。

**步骤：**
1.  **创建迁移脚本**: `db/migration/V{N}__update_{erp}_config.sql`
2.  **SQL 模板**:
    ```sql
    UPDATE sys_erp_config
    SET config_json = '{
        "baseUrl": "https://server.com",
        "appKey": "YOUR_REAL_KEY",
        "appSecret": "YOUR_REAL_SECRET" 
    }'
    WHERE id = {CONFIG_ID};
    ```
3.  **安全检查**: 提交前确保 AppSecret 是正确的。

### 7.3 常见故障排查 (Troubleshooting Guide)

#### 🔴 Issue: "Unsupported ERP Type: YONSUITE"
*   **现象**: 前端传大写 `YONSUITE`，后端报错找不到 Adapter。
*   **根因**: `ErpAdapterFactory` 默认 Map 查找是大小写敏感的，而 Bean Name 通常是小写 (`yonsuite`)。
*   **Fix**: 修改工厂类，增加 **Lowercase Fallback** 机制。
    ```java
    if (adapter == null && type != null) {
        adapter = adapters.get(type.toLowerCase());
    }
    ```

#### 🔴 Issue: "Column does not exist" after SQL Update
*   **现象**: 修改了 SQL 文件 (如删除了错误列)，但启动时依然报旧错误。
*   **根因**: Maven `target` 目录有旧缓存，或 Zombie Java 进程锁定了旧 Jar 包。
*   **Fix (Clean Build SOP)**:
    1.  `killall java` (确保进程彻底杀死，防止 Zombie)
    2.  `mvn clean package -Dmaven.test.skip=true` (强制全量重编译)
    3.  `mvn spring-boot:run`

#### 🔴 Issue: "Migration Failed" / "Validate failed: detected failed migration"
*   **现象**: Flyway 启动报错，因为之前的迁移失败留下了脏记录。
*   **Fix**:
    1.  在 `FlywayRunner` 中启用 `flyway.repair()`。
    2.  或者手动以 SQL 删除 `flyway_schema_history` 表中 `success=0` 的记录。

### 7.4 数据同步集成 (Data Sync Loop)
AI 生成的代码通常是"孤立"的 Service。必须将其挂载到主流程 `ErpAdapter` 中。

**步骤：**
1.  **在 Adapter 中注入新 Service**:
    ```java
    public class YonSuiteErpAdapter implements ErpAdapter {
        private final YonPaymentListService paymentListService; // 新生成的 Service
    ```
2.  **在 `pullData` 或 `syncFile` 中调用**:
    ```java
    @Override
    public List<String> syncPaymentFiles(ErpConfig config, ...) {
        // [New Logic]
        return paymentListService.queryPaymentIds(config, start, end);
    }
    ```

### 7.5 PDF 生成格式规范 (PDF Generation Format Standards)

**[新增章节 2025-12-17]**
当 ERP 数据同步进入系统后,需要将其转换为 PDF 进行归档。PDF 的格式设计不能"千篇一律",**必须**符合业务场景的特点。

#### 7.5.1 格式设计原则 (Format Design Principles)

**核心原则**: PDF 不是简单的"数据打印",而是**业务单据的电子化还原**。

| 原则 | 说明 | 反例 (错误) | 正例 (正确) |
|------|------|-------------|-------------|
| **业务可识别性** | 一眼能看出单据类型 | 所有 PDF 都用同一个表格模板 | 付款单有「付」字水印,凭证有「凭」字标记 |
| **纸质对标** | 与用户习惯的纸质单据布局一致 | 用通用 Grid 布局 | 参考客户现有纸质单据设计 |
| **信息完整性** | 包含业务审批流/附件关联等元信息 | 只有金额和日期 | 包括制单人、审核人、审批流状态 |
| **合规性** | 满足档案法对归档文件的格式要求 | 不可搜索的图片 PDF | 可搜索的 OFD/PDF-A |

#### 7.5.2 单据类型与格式映射 (Document Type Mapping)

不同业务单据应采用不同的 PDF 布局模板:

```
┌────────────────────────────────────────────────────────────┐
│  付款单 (Payment Bill)                                      │
├────────────────────────────────────────────────────────────┤
│  布局特点:                                                  │
│  • 页面方向: Landscape (横向 A4)                            │
│  • 头部: Logo + 单据类型大标题 + 单据编号                    │
│  • 摘要横幅: 总金额、收款方、付款日期（高亮展示）             │
│  • 主体: 网格化表单信息 (4列布局)                            │
│  • 明细表: 多行物料/金额明细 (含合计行)                      │
│  • 底部: 制单人、审核人、审批流程签名栏                       │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│  记账凭证 (Voucher)                                         │
├────────────────────────────────────────────────────────────┤
│  布局特点:                                                  │
│  • 页面方向: Portrait (竖向 A4)                             │
│  • 严格的借贷分栏表格                                       │
│  • 必须有「借方合计 = 贷方合计」校验横线                     │
│  • 包含会计科目树形展示                                     │
│  • 底部: 制单、复核、会计主管签字栏                          │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│  发票 (Invoice)                                             │
├────────────────────────────────────────────────────────────┤
│  布局特点:                                                  │
│  • 高度还原税务局规定的发票格式                              │
│  • 包含 QR Code (发票号码、校验码)                          │
│  • 税额单独标注                                             │
│  • 必须有发票专用章的扫描图像区域                            │
└────────────────────────────────────────────────────────────┘
```

#### 7.5.3 AI 生成 PDF 格式的策略 (AI-Driven PDF Layout Strategy)

**挑战**: AI 在 Pass 2 生成代码时,无法"看到"客户的实际纸质单据。

**解决方案**: **用户提供预览格式 + AI 辅助生成**

##### 方案 A: 用户提供样例单据图片 (Recommended)

**流程**:
1. 客户上传一张现有单据的照片/扫描件
2. AI (Gemini 1.5 Pro 多模态) 分析图片,识别:
   - 页面布局 (横向/竖向)
   - 字段分区 (Header/Body/Footer)
   - 表格列数和对齐方式
3. AI 生成对应的 PDF 模板代码 (基于 iText / Apache PDFBox)

**Prompt 示例**:
```markdown
# Role: PDF Layout Analyzer & Code Generator

## Input
[Upload image of existing paper document]

## Task
1. Analyze the layout structure (orientation, sections, table columns)
2. Generate Java code using iText to replicate this layout
3. Map the fields from SIM JSON to corresponding positions in the PDF

## Output
Java method: `byte[] generatePaymentPDF(PaymentDTO dto)`
```

##### 方案 B: 配置化模板系统 (Template Configuration)

如果不希望每次都生成代码,可以在系统中内置**模板配置 DSL**:

**示例配置 (YAML)**:
```yaml
templates:
  - templateId: PAYMENT_LANDSCAPE_V1
    erpType: YONSUITE
    purpose: PAYMENT_LIST
    pageOrientation: LANDSCAPE
    pageSize: A4
    sections:
      - type: HEADER
        height: 80
        elements:
          - type: TEXT
            content: "付款单 Payment Bill"
            fontSize: 20
            align: CENTER
            bold: true
          - type: FIELD
            field: voucherNo
            label: "单据编号:"
            x: 500
            y: 60
      - type: SUMMARY_BANNER
        height: 60
        backgroundColor: "#F0F0F0"
        elements:
          - {field: amount, label: "总金额", format: "¥ #,##0.00"}
          - {field: payeeName, label: "收款方"}
          - {field: voucherDate, label: "付款日期", format: "yyyy-MM-dd"}
      - type: FORM_GRID
        columns: 4
        fields:
          - {field: orderNo, label: "订单号"}
          - {field: materialName, label: "物料名称"}
          # ... 其他字段
      - type: TABLE
        dataSource: detailList
        columns:
          - {header: "序号", field: index, width: 50}
          - {header: "物料", field: materialName, width: 200}
          - {header: "数量", field: quantity, width: 80, align: RIGHT}
          - {header: "单价", field: price, width: 100, align: RIGHT, format: "#,##0.00"}
          - {header: "金额", field: lineAmount, width: 120, align: RIGHT, format: "#,##0.00"}
```

**代码生成时**: AI 读取此配置并生成对应的 `PdfGenerator.java`。

#### 7.5.4 PDF 生成实现规范 (Implementation Standards)

无论采用哪种方案,生成的代码**必须**遵循以下规范:

##### 代码模板

```java
@Service
@RequiredArgsConstructor
public class YonPaymentPdfGenerator {

    private final TemplateConfigRepository templateRepo; // 如果使用模板配置

    /**
     * 生成付款单 PDF
     * 
     * @param dto 付款单数据
     * @param templateId 模板 ID (可选,为 null 时使用默认模板)
     * @return PDF 字节数组
     */
    public byte[] generatePDF(PaymentDTO dto, String templateId) throws IOException {
        // 1. 加载模板配置 (如果使用配置化方案)
        TemplateConfig template = templateId != null
            ? templateRepo.findById(templateId).orElseThrow()
            : getDefaultTemplate(dto.getErpType());

        // 2. 创建 PDF 文档
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        
        // 设置页面方向
        PageSize pageSize = template.getPageOrientation() == Orientation.LANDSCAPE
            ? PageSize.A4.rotate()
            : PageSize.A4;
        pdfDoc.setDefaultPageSize(pageSize);

        Document document = new Document(pdfDoc);

        // 3. 渲染各个 Section
        for (SectionConfig section : template.getSections()) {
            switch (section.getType()) {
                case HEADER -> renderHeader(document, section, dto);
                case SUMMARY_BANNER -> renderSummaryBanner(document, section, dto);
                case FORM_GRID -> renderFormGrid(document, section, dto);
                case TABLE -> renderDetailTable(document, section, dto);
                case FOOTER -> renderFooter(document, section, dto);
            }
        }

        document.close();
        return baos.toByteArray();
    }

    private void renderSummaryBanner(Document doc, SectionConfig section, PaymentDTO dto) {
        // 创建带背景色的横幅
        Table bannerTable = new Table(section.getElements().size());
        bannerTable.setWidth(UnitValue.createPercentValue(100));
        bannerTable.setBackgroundColor(ColorConstants.LIGHT_GRAY);

        for (ElementConfig elem : section.getElements()) {
            Cell cell = new Cell();
            // 获取字段值
            Object value = getFieldValue(dto, elem.getField());
            String formatted = formatValue(value, elem.getFormat());
            
            cell.add(new Paragraph(elem.getLabel() + ": " + formatted)
                .setFontSize(14)
                .setBold());
            bannerTable.addCell(cell);
        }

        doc.add(bannerTable);
    }

    private void renderDetailTable(Document doc, SectionConfig section, PaymentDTO dto) {
        // 渲染明细表
        List<?> detailList = (List<?>) getFieldValue(dto, section.getDataSource());
        
        Table table = new Table(section.getColumns().size());
        table.setWidth(UnitValue.createPercentValue(100));

        // 添加表头
        for (ColumnConfig col : section.getColumns()) {
            Cell headerCell = new Cell()
                .add(new Paragraph(col.getHeader()))
                .setBackgroundColor(ColorConstants.GRAY)
                .setFontColor(ColorConstants.WHITE)
                .setBold();
            table.addHeaderCell(headerCell);
        }

        // 添加数据行
        for (Object item : detailList) {
            for (ColumnConfig col : section.getColumns()) {
                Object value = getFieldValue(item, col.getField());
                String formatted = formatValue(value, col.getFormat());
                
                Cell dataCell = new Cell()
                    .add(new Paragraph(formatted != null ? formatted : ""))
                    .setTextAlignment(col.getAlign());
                table.addCell(dataCell);
            }
        }

        doc.add(table);
    }

    /**
     * 使用反射或 JSONPath 从 DTO 中获取字段值
     */
    private Object getFieldValue(Object obj, String fieldPath) {
        // 实现字段提取逻辑 (支持嵌套路径如 "supplier.name")
        // ...
    }

    /**
     * 格式化字段值 (金额、日期等)
     */
    private String formatValue(Object value, String format) {
        if (value == null) return "";
        
        if (value instanceof BigDecimal && format != null) {
            DecimalFormat df = new DecimalFormat(format);
            return df.format(value);
        }
        
        if (value instanceof LocalDate && format != null) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
            return ((LocalDate) value).format(dtf);
        }
        
        return value.toString();
    }
}
```

#### 7.5.5 用户自定义预览格式 (User-Defined Preview Format)

**功能需求**: 允许用户在系统中可视化配置 PDF 预览格式。

**实现方式**: 提供一个**模板编辑器**前端页面

```
┌─────────────────────────────────────────────────────────────┐
│  PDF 模板编辑器 (Template Editor)                            │
├─────────────────────────────────────────────────────────────┤
│  左侧: 拖拽组件库                                            │
│  ├── 文本框 (Text)                                          │
│  ├── 字段绑定 (Field Binding)                               │
│  ├── 表格 (Table)                                           │
│  ├── 图片/Logo (Image)                                      │
│  └── 签名栏 (Signature)                                     │
│                                                             │
│  中间: 画布预览 (WYSIWYG)                                    │
│  ┌─────────────────────────────────────┐                   │
│  │   [拖拽元素到此处编排布局]            │                   │
│  │                                     │                   │
│  │   【付款单】                         │                   │
│  │   单据编号: {voucherNo}             │                   │
│  │   ────────────────────────          │                   │
│  │   金额: {amount}  日期: {date}      │                   │
│  │                                     │                   │
│  └─────────────────────────────────────┘                   │
│                                                             │
│  右侧: 属性面板                                              │
│  • 字段: [选择字段]                                          │
│  • 格式: [¥ #,##0.00]                                       │
│  • 字体: [宋体 ▼] 大小: [14 ▼]                              │
│  • 对齐: [左 中 右]                                          │
└─────────────────────────────────────────────────────────────┘
```

**保存逻辑**:
- 用户配置的模板保存为 JSON (template_config 表)
- 绑定到特定 ERP 类型 + 单据类型
- 生成 PDF 时动态读取配置

#### 7.5.6 PDF 生成检查清单 (PDF Generation Checklist)

在 AI 生成 PDF 代码后,**必须**人工验证以下项:

| 检查项 | 说明 | 工具 |
|--------|------|------|
| **字段完整性** | 所有业务字段都已映射到 PDF | 对照 SIM JSON fieldMappings |
| **格式正确性** | 金额保留2位小数,日期格式统一 | 生成样例 PDF 检查 |
| **页面方向** | 与业务单据特点匹配 | 参考用户提供的样例 |
| **可搜索性** | PDF 文本可被选中和搜索 | 用 Adobe Reader 测试 |
| **OFD 兼容性** | 如需 OFD,使用 suwell 库 | 转换测试 |
| **打印还原度** | 打印后与原始单据相似度 | 实际打印对比 |
| **附件关联** | PDF 中标注了附件清单 | 查看 PDF Footer |

---

### 7.6 测试验收 (Testing & Acceptance)

AI 生成的代码**必须**经过完整的测试验收流程才能上线。

#### 7.6.1 单元测试模板 (Unit Test Template)

每个新生成的 Adapter/Service **必须**附带对应的单元测试：

```java
@ExtendWith(MockitoExtension.class)
class YonPaymentListServiceTest {

    @Mock
    private YonAuthService yonAuthService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private YonPaymentListService paymentListService;

    private ErpConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = new ErpConfig();
        testConfig.setBaseUrl("https://api.yonyou.com");
        testConfig.setAppKey("test_key");
        testConfig.setAppSecret("test_secret");
    }

    // ========== 认证测试 ==========
    @Test
    @DisplayName("应正确获取并缓存 AccessToken")
    void shouldGetAndCacheAccessToken() {
        when(yonAuthService.getAccessToken(anyString(), anyString()))
            .thenReturn("mock_access_token");

        String token = paymentListService.getAccessToken(testConfig);

        assertThat(token).isEqualTo("mock_access_token");
        verify(yonAuthService, times(1)).getAccessToken("test_key", "test_secret");
    }

    // ========== 数据拉取测试 ==========
    @Test
    @DisplayName("应正确解析付款单列表响应")
    void shouldParsePaymentListResponse() {
        // Given: Mock API 响应
        String mockResponse = """
            {
                "code": "200",
                "data": {
                    "records": [
                        {"id": "PAY001", "amount": 1000.00},
                        {"id": "PAY002", "amount": 2000.00}
                    ]
                }
            }
            """;
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenReturn(mockResponse);
        when(yonAuthService.getAccessToken(anyString(), anyString()))
            .thenReturn("mock_token");

        // When
        List<String> paymentIds = paymentListService.queryPaymentIds(
            testConfig,
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 31)
        );

        // Then
        assertThat(paymentIds).containsExactly("PAY001", "PAY002");
    }

    // ========== 异常处理测试 ==========
    @Test
    @DisplayName("当 API 返回错误码时应抛出异常")
    void shouldThrowExceptionOnApiError() {
        String errorResponse = """
            {"code": "401", "message": "Token expired"}
            """;
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenReturn(errorResponse);

        assertThatThrownBy(() ->
            paymentListService.queryPaymentIds(testConfig, LocalDate.now(), LocalDate.now())
        ).isInstanceOf(ErpApiException.class)
         .hasMessageContaining("Token expired");
    }

    // ========== 字段映射测试 ==========
    @Test
    @DisplayName("应正确映射 ERP 字段到 DTO")
    void shouldMapFieldsCorrectly() {
        // 测试 fieldMappings 中定义的每个映射规则
        // ...
    }
}
```

#### 7.6.2 集成测试策略 (Integration Test Strategy)

```
┌─────────────────────────────────────────────────────────────┐
│                    测试环境分层                              │
├─────────────────────────────────────────────────────────────┤
│  Level 1: Mock 测试 (本地)                                  │
│  ├── 使用 WireMock 模拟 ERP API                             │
│  ├── 验证请求格式、Header、签名                              │
│  └── 验证响应解析、字段映射                                  │
├─────────────────────────────────────────────────────────────┤
│  Level 2: Sandbox 测试 (沙箱环境)                           │
│  ├── 使用 ERP 厂商提供的测试环境                             │
│  ├── 验证真实认证流程                                       │
│  └── 验证端到端数据流转                                     │
├─────────────────────────────────────────────────────────────┤
│  Level 3: UAT 测试 (客户预发布环境)                          │
│  ├── 使用客户真实数据的脱敏副本                              │
│  ├── 验证业务场景覆盖率                                     │
│  └── 获取客户签字确认                                       │
└─────────────────────────────────────────────────────────────┘
```

**WireMock 配置示例：**
```java
@WireMockTest(httpPort = 8089)
class YonSuiteIntegrationTest {

    @Test
    void shouldAuthenticateWithYonSuite(WireMockRuntimeInfo wmRuntimeInfo) {
        // Stub 认证接口
        stubFor(post(urlEqualTo("/api/auth/token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(matchingJsonPath("$.appKey", equalTo("test_key")))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"access_token": "abc123", "expires_in": 7200}
                    """)));

        // 执行测试
        // ...
    }
}
```

#### 7.6.3 回归测试清单 (Regression Checklist)

新增 Adapter 上线前，**必须**确保不影响现有功能：

| 检查项 | 命令 | 预期结果 |
|--------|------|----------|
| 全量单元测试通过 | `mvn test` | BUILD SUCCESS |
| 现有 Adapter 功能正常 | `mvn test -Dtest=*AdapterTest` | 所有 Adapter 测试通过 |
| API 契约未变更 | `mvn verify -Pcontract-test` | 契约测试通过 |
| 无新增安全漏洞 | `mvn dependency-check:check` | 无 HIGH/CRITICAL 漏洞 |
| 编译无警告 | `mvn compile -Werror` | 零警告 |

#### 7.6.4 测试覆盖率要求 (Coverage Requirements)

| 模块类型 | 行覆盖率 | 分支覆盖率 | 说明 |
|----------|----------|------------|------|
| AI 生成的 Adapter | ≥ 80% | ≥ 70% | 核心业务逻辑必须覆盖 |
| 认证模块 | ≥ 90% | ≥ 85% | 安全相关代码从严要求 |
| 字段映射逻辑 | 100% | 100% | 每个映射规则都要有测试 |
| 异常处理路径 | ≥ 70% | ≥ 60% | 常见错误场景必须覆盖 |

### 7.7 版本管理与回滚 (Versioning & Rollback)

#### 7.7.1 版本追溯体系 (Traceability)

每个 AI 生成的代码文件**必须**包含版本元信息注释：

```java
/**
 * AI Generated Adapter for YonSuite ERP
 *
 * @generated true
 * @generator Claude 3.5 Sonnet
 * @simVersion 1.0.0
 * @simFile yonsuite_payment_v1.0.0.json
 * @generatedAt 2025-12-17T10:30:00Z
 * @promptVersion prompt-lib/code-gen/v2.1.0
 *
 * [CHANGE LOG]
 * v1.0.0 (2025-12-17): Initial generation from SIM JSON
 * v1.0.1 (2025-12-18): Manual fix for date format issue
 */
@Service
public class YonPaymentListService {
    // ...
}
```

#### 7.7.2 Git 分支策略 (Branch Strategy)

```
main
  │
  ├── feature/erp-yonsuite-payment    <- AI 生成代码在此分支开发
  │     │
  │     ├── commit: "feat: AI generated payment service (SIM v1.0.0)"
  │     ├── commit: "fix: manual correction for auth header"
  │     └── commit: "test: add unit tests for payment service"
  │
  └── release/v2.5.0                  <- Review 后合入 release
```

**Commit Message 规范：**
```
feat(erp): AI generated YonSuite payment adapter

- SIM Source: docs/sim/yonsuite_payment_v1.0.0.json
- Prompt Version: prompt-lib/code-gen/v2.1.0
- Generator: Claude 3.5 Sonnet
- Confidence: 0.92

Manual modifications:
- Fixed date format from ISO to yyyy-MM-dd
- Added null check for optional fields

Closes #123
```

#### 7.7.3 SIM JSON 存档策略 (SIM Archival)

所有 SIM JSON 文件必须存档在版本控制中：

```
docs/
└── sim/
    ├── archive/                      # 历史版本存档
    │   ├── yonsuite_payment_v0.9.0.json
    │   └── yonsuite_payment_v1.0.0.json
    ├── current/                      # 当前生产版本
    │   └── yonsuite_payment.json -> ../archive/yonsuite_payment_v1.0.0.json
    └── schema/
        └── sim-schema-v1.0.0.json    # Schema 定义文件
```

#### 7.7.4 回滚流程 (Rollback Procedure)

**场景：新上线的 Adapter 出现生产问题**

```
┌─────────────────────────────────────────────────────────────┐
│  Step 1: 立即止血 (Immediate Mitigation)                    │
│  └── 在 sys_erp_config 中禁用该 ERP 类型                    │
│      UPDATE sys_erp_config SET enabled=false WHERE type='X' │
├─────────────────────────────────────────────────────────────┤
│  Step 2: 代码回滚 (Code Rollback)                           │
│  └── git revert <commit-hash>                               │
│  └── 或 git checkout <previous-tag> -- path/to/Adapter.java │
├─────────────────────────────────────────────────────────────┤
│  Step 3: 快速部署 (Fast Deploy)                             │
│  └── mvn clean package -Dmaven.test.skip=true               │
│  └── 重启服务                                               │
├─────────────────────────────────────────────────────────────┤
│  Step 4: 事后复盘 (Post-Mortem)                             │
│  └── 分析 SIM JSON 哪里出错                                 │
│  └── 更新 Prompt 或 Schema 防止复发                         │
│  └── 补充相关测试用例                                       │
└─────────────────────────────────────────────────────────────┘
```

### 7.8 监控与告警 (Monitoring & Alerting)

#### 7.8.1 关键监控指标 (Key Metrics)

| 指标名称 | 采集方式 | 告警阈值 | 说明 |
|----------|----------|----------|------|
| `erp.sync.success_rate` | Micrometer Counter | < 95% | 同步成功率 |
| `erp.sync.duration_ms` | Micrometer Timer | p99 > 30s | 同步耗时 |
| `erp.auth.token_refresh_count` | Counter | > 10/hour | Token 刷新频率异常 |
| `erp.auth.token_expiry_seconds` | Gauge | < 300 | Token 即将过期预警 |
| `erp.api.error_count` | Counter by error_code | > 5/min | API 错误激增 |
| `erp.adapter.active` | Gauge | = 0 | Adapter 不可用 |

#### 7.8.2 健康检查端点 (Health Check Endpoint)

```java
@Component
public class ErpHealthIndicator implements HealthIndicator {

    private final Map<String, ErpAdapter> adapters;
    private final ErpConfigRepository configRepo;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean allHealthy = true;

        for (ErpConfig config : configRepo.findAllEnabled()) {
            try {
                ErpAdapter adapter = adapters.get(config.getType().toLowerCase());
                if (adapter == null) {
                    details.put(config.getType(), "ADAPTER_NOT_FOUND");
                    allHealthy = false;
                    continue;
                }

                // 检查 Token 有效性
                boolean tokenValid = adapter.validateToken(config);
                // 检查 API 连通性
                boolean apiReachable = adapter.ping(config);

                details.put(config.getType(), Map.of(
                    "tokenValid", tokenValid,
                    "apiReachable", apiReachable,
                    "lastSyncAt", config.getLastSyncAt()
                ));

                if (!tokenValid || !apiReachable) {
                    allHealthy = false;
                }
            } catch (Exception e) {
                details.put(config.getType(), "ERROR: " + e.getMessage());
                allHealthy = false;
            }
        }

        return allHealthy
            ? Health.up().withDetails(details).build()
            : Health.down().withDetails(details).build();
    }
}
```

#### 7.8.3 告警规则配置 (Alert Rules - Prometheus)

```yaml
groups:
  - name: erp-integration-alerts
    rules:
      # Token 即将过期告警
      - alert: ErpTokenExpiringSoon
        expr: erp_auth_token_expiry_seconds < 300
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "ERP Token 即将过期"
          description: "{{ $labels.erp_type }} 的 Token 将在 {{ $value }} 秒后过期"

      # 同步失败率过高
      - alert: ErpSyncFailureRateHigh
        expr: |
          (
            sum(rate(erp_sync_total{status="failed"}[5m])) by (erp_type)
            /
            sum(rate(erp_sync_total[5m])) by (erp_type)
          ) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "ERP 同步失败率过高"
          description: "{{ $labels.erp_type }} 同步失败率达到 {{ $value | humanizePercentage }}"

      # API 连续错误
      - alert: ErpApiConsecutiveErrors
        expr: increase(erp_api_error_count[5m]) > 10
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "ERP API 连续错误"
          description: "{{ $labels.erp_type }} 在过去 5 分钟内发生 {{ $value }} 次 API 错误"

      # 同步任务卡住
      - alert: ErpSyncStuck
        expr: time() - erp_sync_last_success_timestamp > 3600
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "ERP 同步任务可能卡住"
          description: "{{ $labels.erp_type }} 已超过 1 小时未成功同步"
```

#### 7.8.4 日志规范 (Logging Standards)

所有 ERP 相关日志**必须**包含以下 MDC 字段：

```java
@Aspect
@Component
public class ErpLoggingAspect {

    @Around("execution(* com.nexusarchive.erp.adapter.*.*(..))")
    public Object logErpOperation(ProceedingJoinPoint pjp) throws Throwable {
        ErpConfig config = extractConfig(pjp.getArgs());

        try (MDC.MDCCloseable ignored1 = MDC.putCloseable("erpType", config.getType());
             MDC.MDCCloseable ignored2 = MDC.putCloseable("erpConfigId", config.getId().toString());
             MDC.MDCCloseable ignored3 = MDC.putCloseable("traceId", UUID.randomUUID().toString())) {

            log.info("[ERP] Starting operation: {}", pjp.getSignature().getName());
            long start = System.currentTimeMillis();

            try {
                Object result = pjp.proceed();
                log.info("[ERP] Operation completed in {}ms", System.currentTimeMillis() - start);
                return result;
            } catch (Exception e) {
                log.error("[ERP] Operation failed: {}", e.getMessage(), e);
                throw e;
            }
        }
    }
}
```

**日志格式示例：**
```
2025-12-17 10:30:00.123 INFO  [erpType=YONSUITE, erpConfigId=42, traceId=abc-123]
  c.n.e.a.YonSuiteAdapter - [ERP] Starting operation: syncPaymentFiles
2025-12-17 10:30:05.456 INFO  [erpType=YONSUITE, erpConfigId=42, traceId=abc-123]
  c.n.e.a.YonSuiteAdapter - [ERP] Operation completed in 5333ms
```

### 7.9 错误处理规范 (Error Handling Standards)

#### 7.9.1 异常层级定义 (Exception Hierarchy)

```java
/**
 * ERP 集成异常基类
 */
public abstract class ErpIntegrationException extends RuntimeException {
    private final String erpType;
    private final String errorCode;
    private final boolean retryable;

    // ... constructors and getters
}

/**
 * 认证失败 - Token 过期/无效
 */
public class ErpAuthenticationException extends ErpIntegrationException {
    public ErpAuthenticationException(String erpType, String message) {
        super(erpType, "AUTH_FAILED", message, true); // 可重试
    }
}

/**
 * API 调用失败 - 网络/超时/服务端错误
 */
public class ErpApiException extends ErpIntegrationException {
    private final int httpStatus;
    private final String responseBody;

    public ErpApiException(String erpType, int httpStatus, String responseBody) {
        super(erpType, "API_ERROR_" + httpStatus, responseBody, httpStatus >= 500); // 5xx 可重试
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }
}

/**
 * 数据映射失败 - 字段缺失/格式错误
 */
public class ErpMappingException extends ErpIntegrationException {
    private final String fieldName;
    private final Object actualValue;

    public ErpMappingException(String erpType, String fieldName, Object actualValue, String message) {
        super(erpType, "MAPPING_FAILED", message, false); // 不可重试，需人工介入
        this.fieldName = fieldName;
        this.actualValue = actualValue;
    }
}

/**
 * 配置错误 - AppKey/URL 等配置问题
 */
public class ErpConfigurationException extends ErpIntegrationException {
    public ErpConfigurationException(String erpType, String message) {
        super(erpType, "CONFIG_ERROR", message, false); // 不可重试
    }
}
```

#### 7.9.2 重试策略模板 (Retry Strategy Template)

```java
@Configuration
public class ErpRetryConfig {

    @Bean
    public RetryTemplate erpRetryTemplate() {
        return RetryTemplate.builder()
            // 最多重试 3 次
            .maxAttempts(3)
            // 指数退避：1s -> 2s -> 4s
            .exponentialBackoff(1000, 2, 10000)
            // 只重试特定异常
            .retryOn(ErpAuthenticationException.class)
            .retryOn(ErpApiException.class)
            // 不重试的异常
            .notRetryOn(ErpConfigurationException.class)
            .notRetryOn(ErpMappingException.class)
            // 重试监听器
            .withListener(new RetryListenerSupport() {
                @Override
                public <T, E extends Throwable> void onError(
                        RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                    log.warn("[ERP] Retry attempt {} failed: {}",
                        context.getRetryCount(), throwable.getMessage());
                }
            })
            .build();
    }
}

// 使用示例
@Service
@RequiredArgsConstructor
public class YonPaymentListService {

    private final RetryTemplate retryTemplate;

    public List<PaymentDTO> fetchPayments(ErpConfig config, LocalDate start, LocalDate end) {
        return retryTemplate.execute(ctx -> {
            if (ctx.getRetryCount() > 0) {
                log.info("[ERP] Retrying fetchPayments, attempt {}", ctx.getRetryCount() + 1);
                // 重试前刷新 Token
                refreshToken(config);
            }
            return doFetchPayments(config, start, end);
        });
    }
}
```

#### 7.9.3 AI 生成代码的错误处理模板 (Error Handling Template for AI)

在 Pass 2 (代码生成) 阶段，Prompt 中**必须**包含以下错误处理模板要求：

```java
/**
 * [AI CODE GENERATION TEMPLATE]
 * 所有生成的 Service 方法必须遵循此错误处理模式
 */
public List<PaymentDTO> queryPayments(ErpConfig config, LocalDate start, LocalDate end) {
    // 1. 参数校验
    Objects.requireNonNull(config, "ErpConfig cannot be null");
    Objects.requireNonNull(start, "Start date cannot be null");
    Objects.requireNonNull(end, "End date cannot be null");
    if (start.isAfter(end)) {
        throw new IllegalArgumentException("Start date must be before end date");
    }

    // 2. 获取 Token (带异常转换)
    String accessToken;
    try {
        accessToken = authService.getAccessToken(config.getAppKey(), config.getAppSecret());
    } catch (Exception e) {
        throw new ErpAuthenticationException(config.getType(),
            "Failed to obtain access token: " + e.getMessage());
    }

    // 3. 调用 API (带详细错误信息)
    String responseBody;
    try {
        HttpHeaders headers = buildHeaders(accessToken);
        HttpEntity<String> request = new HttpEntity<>(buildRequestBody(start, end), headers);

        ResponseEntity<String> response = restTemplate.exchange(
            config.getBaseUrl() + "/api/payments",
            HttpMethod.POST,
            request,
            String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ErpApiException(config.getType(),
                response.getStatusCode().value(),
                response.getBody());
        }
        responseBody = response.getBody();

    } catch (RestClientException e) {
        throw new ErpApiException(config.getType(), 0,
            "Network error: " + e.getMessage());
    }

    // 4. 解析响应 (带字段级错误定位)
    try {
        JsonNode root = objectMapper.readTree(responseBody);

        // 检查业务错误码
        String code = root.path("code").asText();
        if (!"200".equals(code) && !"0".equals(code)) {
            throw new ErpApiException(config.getType(),
                Integer.parseInt(code),
                root.path("message").asText("Unknown error"));
        }

        // 解析数据
        JsonNode dataNode = root.path("data").path("records");
        if (dataNode.isMissingNode()) {
            throw new ErpMappingException(config.getType(),
                "data.records", null, "Response missing 'data.records' field");
        }

        List<PaymentDTO> result = new ArrayList<>();
        for (JsonNode item : dataNode) {
            result.add(mapToDTO(item, config.getType()));
        }
        return result;

    } catch (JsonProcessingException e) {
        throw new ErpMappingException(config.getType(),
            "responseBody", responseBody, "Invalid JSON response: " + e.getMessage());
    }
}

private PaymentDTO mapToDTO(JsonNode node, String erpType) {
    PaymentDTO dto = new PaymentDTO();

    // 必填字段 - 缺失时抛异常
    String id = node.path("id").asText(null);
    if (id == null) {
        throw new ErpMappingException(erpType, "id", null, "Payment ID is required");
    }
    dto.setId(id);

    // 金额字段 - 格式错误时抛异常
    try {
        dto.setAmount(new BigDecimal(node.path("amount").asText("0")));
    } catch (NumberFormatException e) {
        throw new ErpMappingException(erpType, "amount",
            node.path("amount").asText(), "Invalid amount format");
    }

    // 可选字段 - 使用默认值
    dto.setRemark(node.path("remark").asText(""));

    return dto;
}
```

---

## 附录 A: Prompt Library (提示词库)

本附录提供完整的、可直接使用的 Prompt 模板，覆盖 AI 集成工作流的两个核心阶段。

### A.1 Prompt 1: 文档清洗器 (Doc → SIM JSON)

**用途**: Pass 1 归一化阶段，从各种格式的接口文档中提取结构化元数据。

**推荐模型**: Gemini 1.5 Pro / GPT-4o (多模态能力强)

```markdown
# Role: ERP Integration Document Analyst

You are a senior integration engineer specializing in extracting structured metadata from ERP API documentation.

## Task
Analyze the provided documentation (PDF/Word/Image/Swagger) and extract integration metadata into a standardized SIM JSON format.

## Input
[Attach or paste the ERP API documentation here]

## Output Requirements
Generate a valid JSON conforming to the SIM Schema. You MUST include:

1. **erpType**: Identify the ERP system (KINGDEE_K3, KINGDEE_CLOUD, YONSUITE, SAP, ORACLE, WEAVER, OTHER)

2. **authentication**: Extract ALL authentication details:
   - Login/Token URL
   - Authentication type (OAUTH2, API_KEY, COOKIE_SESSION, etc.)
   - Required headers and parameters
   - Token response path (JSONPath format)

3. **endpoints**: For EACH API endpoint mentioned:
   - URL path and HTTP method
   - Purpose classification (VOUCHER_LIST, PAYMENT_LIST, etc.)
   - Request format and template
   - Response data path
   - Pagination details if applicable

4. **fieldMappings**: Create mappings for standard fields:
   - Map source field paths to these target fields:
     * id, voucherDate, voucherNo, debitAmount, creditAmount
     * accountCode, accountName, summary, remark
   - Specify transform type if needed (DATE_FORMAT, NUMBER_SCALE, etc.)

5. **knownIssues**: Document any:
   - Special authentication quirks (Cookie handling, signature algorithms)
   - Non-standard response formats
   - Rate limits or pagination limits
   - Known API bugs or workarounds mentioned in docs

## Confidence Scoring
For EACH extracted field, internally assess your confidence (0.0-1.0):
- 1.0: Explicitly stated in documentation
- 0.8: Strongly implied from examples
- 0.5: Inferred from patterns, needs verification
- 0.3: Guessed based on common conventions

Include overall confidence in metadata.confidence field.

## Handling Ambiguity
If you encounter:
- Multiple possible values: List all options in knownIssues, pick most likely for main field
- Missing information: Use null, document in knownIssues
- Contradictory information: Flag in knownIssues, use the value from more authoritative section

## Output Format
```json
{
  "erpType": "...",
  "version": "1.0.0",
  "metadata": {
    "docSource": "[Original document name]",
    "extractedAt": "[ISO timestamp]",
    "extractedBy": "[Model name]",
    "confidence": 0.XX
  },
  "authentication": { ... },
  "endpoints": [ ... ],
  "fieldMappings": [ ... ],
  "knownIssues": [ ... ]
}
```

## Example (Partial)
If the document says:
> "调用接口前需要先获取 access_token，请求地址：https://api.xxx.com/auth/token，使用 POST 方法，参数为 appKey 和 appSecret"

You should extract:
```json
{
  "authentication": {
    "type": "OAUTH2",
    "loginUrl": "https://api.xxx.com/auth/token",
    "params": {
      "appKey": "{{appKey}}",
      "appSecret": "{{appSecret}}"
    },
    "tokenPath": "$.access_token"
  }
}
```

Now analyze the provided documentation and generate the complete SIM JSON.
```

---

### A.2 Prompt 2: 代码生成器 (SIM JSON → Java Code)

**用途**: Pass 2 代码生成阶段，从 SIM JSON 生成可编译运行的 Java 代码。

**推荐模型**: Claude 3.5 Sonnet / DeepSeek Coder (编程能力强)

```markdown
# Role: Java ERP Adapter Code Generator

You are a senior Java developer specializing in ERP integration. Generate production-ready Spring Boot code based on the provided SIM JSON and project templates.

## Input 1: SIM JSON
```json
[Paste the SIM JSON here]
```

## Input 2: Project Context
- Framework: Spring Boot 3.1.6
- HTTP Client: RestTemplate (or specify WebClient)
- JSON Library: Jackson
- Auth Service: Inject existing `{ErpType}AuthService` for token management
- Base Interface: Implement `ErpAdapter` interface
- Package: `com.nexusarchive.erp.adapter.{erptype}`

## Output Requirements

Generate the following files:

### File 1: `{ErpType}ErpAdapter.java`
- Implement `ErpAdapter` interface
- Inject auth service and generated services
- Delegate to specific service methods

### File 2: `{ErpType}{Purpose}Service.java` (for each endpoint purpose)
- Service class for specific API calls
- Include full error handling per 7.9 standards
- Use retry template for network calls

### File 3: `{ErpType}DTOMapper.java`
- Static mapping methods based on fieldMappings
- Handle all transform types (DATE_FORMAT, NUMBER_SCALE, etc.)

## Code Standards

1. **File Header** (REQUIRED):
```java
/**
 * AI Generated Adapter for {erpType}
 *
 * @generated true
 * @generator [Your Model Name]
 * @simVersion {version from SIM}
 * @simFile {docSource from metadata}
 * @generatedAt {current ISO timestamp}
 */
```

2. **Dependencies** (inject, never hardcode):
```java
@Service
@RequiredArgsConstructor
public class XxxService {
    private final XxxAuthService authService;  // For token management
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RetryTemplate retryTemplate;
```

3. **Token Usage** (NEVER use mock):
```java
// CORRECT
String token = authService.getAccessToken(config.getAppKey(), config.getAppSecret());

// WRONG - Never generate this
String token = "mock_token";
```

4. **Error Handling** (follow 7.9 template):
- Wrap auth failures in `ErpAuthenticationException`
- Wrap API errors in `ErpApiException`
- Wrap mapping errors in `ErpMappingException`
- Include erpType in all exceptions

5. **Logging** (use SLF4J with MDC):
```java
log.info("[ERP] Calling {} endpoint", endpointName);
log.debug("[ERP] Request body: {}", requestBody);
log.error("[ERP] API error: {} - {}", statusCode, responseBody);
```

6. **Field Mapping** (handle nulls gracefully):
```java
// Required field
String id = node.path("id").asText(null);
if (id == null) {
    throw new ErpMappingException(erpType, "id", null, "ID is required");
}

// Optional field with default
String remark = node.path("remark").asText("");
```

## Handling SIM JSON Issues

If the SIM JSON has:
- **knownIssues**: Generate TODO comments at relevant code locations
- **Low confidence fields**: Add validation and fallback logic
- **Custom transforms**: Generate helper methods with clear documentation

## Output Format

Provide complete, compilable Java code for each file. Use this structure:

---
### File: `{filename}.java`
```java
[Complete code here]
```
---

Generate the code now based on the provided SIM JSON.
```

---

### A.3 Prompt 3: 测试用例生成器 (SIM JSON → Unit Tests)

**用途**: 为生成的代码自动生成单元测试。

```markdown
# Role: Java Test Code Generator

Generate comprehensive unit tests for the ERP adapter code based on the SIM JSON.

## Input
1. SIM JSON: [Paste here]
2. Generated Service Code: [Paste here]

## Test Requirements

Generate tests covering:

1. **Authentication Tests**
   - Successful token retrieval
   - Token refresh on expiry
   - Auth failure handling

2. **API Call Tests**
   - Successful response parsing
   - Error response handling (4xx, 5xx)
   - Network timeout handling
   - Empty response handling

3. **Field Mapping Tests**
   - Each fieldMapping from SIM JSON needs a dedicated test
   - Test transform functions (DATE_FORMAT, NUMBER_SCALE)
   - Test required vs optional field handling
   - Test null/missing field handling

4. **Edge Cases from knownIssues**
   - Generate test for each known issue
   - Include comments explaining the edge case

## Test Framework
- JUnit 5 + Mockito
- AssertJ for assertions
- WireMock for integration tests (optional)

## Output
Complete test class with all test methods.
```

---

### A.4 SIM JSON 示例 (完整示例)

以下是一个完整的 YonSuite 付款单接口的 SIM JSON 示例：

```json
{
  "erpType": "YONSUITE",
  "version": "1.0.0",
  "metadata": {
    "docSource": "YonSuite_OpenAPI_付款单_v2.3.pdf",
    "extractedAt": "2025-12-17T10:00:00Z",
    "extractedBy": "GPT-4o",
    "confidence": 0.92
  },
  "authentication": {
    "type": "OAUTH2",
    "loginUrl": "https://openapi.yonyoucloud.com/token",
    "tokenUrl": "https://openapi.yonyoucloud.com/token",
    "refreshUrl": null,
    "headers": {
      "Content-Type": "application/x-www-form-urlencoded"
    },
    "params": {
      "grant_type": "client_credentials",
      "appKey": "{{appKey}}",
      "appSecret": "{{appSecret}}"
    },
    "tokenPath": "$.data.access_token",
    "expiresInPath": "$.data.expires_in"
  },
  "endpoints": [
    {
      "name": "查询付款单列表",
      "url": "/api/payment/list",
      "method": "POST",
      "purpose": "PAYMENT_LIST",
      "requestFormat": "JSON",
      "requestTemplate": {
        "pageIndex": "{{pageIndex}}",
        "pageSize": "{{pageSize}}",
        "startDate": "{{startDate}}",
        "endDate": "{{endDate}}",
        "status": "approved"
      },
      "responseDataPath": "$.data.records",
      "pagination": {
        "type": "PAGE_NUMBER",
        "pageParam": "pageIndex",
        "sizeParam": "pageSize",
        "defaultSize": 100
      }
    },
    {
      "name": "获取付款单详情",
      "url": "/api/payment/detail",
      "method": "POST",
      "purpose": "VOUCHER_DETAIL",
      "requestFormat": "JSON",
      "requestTemplate": {
        "paymentId": "{{paymentId}}"
      },
      "responseDataPath": "$.data",
      "pagination": null
    }
  ],
  "fieldMappings": [
    {
      "sourceField": "$.id",
      "targetField": "id",
      "transform": "DIRECT"
    },
    {
      "sourceField": "$.payDate",
      "targetField": "voucherDate",
      "transform": "DATE_FORMAT",
      "transformConfig": {
        "sourceFormat": "yyyy-MM-dd HH:mm:ss",
        "targetFormat": "yyyy-MM-dd"
      }
    },
    {
      "sourceField": "$.payNo",
      "targetField": "voucherNo",
      "transform": "DIRECT"
    },
    {
      "sourceField": "$.payAmount",
      "targetField": "amount",
      "transform": "NUMBER_SCALE",
      "transformConfig": {
        "scale": 2,
        "roundingMode": "HALF_UP"
      }
    },
    {
      "sourceField": "$.supplierName",
      "targetField": "payeeName",
      "transform": "DIRECT"
    },
    {
      "sourceField": "$.memo",
      "targetField": "remark",
      "transform": "DIRECT"
    }
  ],
  "knownIssues": [
    "Token 有效期仅 2 小时，需要实现自动刷新机制",
    "分页 pageIndex 从 1 开始，不是 0",
    "日期字段返回格式不固定，有时带时区有时不带",
    "当 records 为空时返回 null 而不是空数组",
    "部分历史数据的 supplierName 可能为空"
  ]
}
```

---

## 附录 B: 核心字段映射参考 (Field Mapping Reference)

### B.1 标准 DTO 字段定义

| 目标字段 | 类型 | 必填 | 说明 |
|----------|------|------|------|
| `id` | String | ✅ | 唯一标识符 |
| `voucherNo` | String | ✅ | 单据编号 |
| `voucherDate` | LocalDate | ✅ | 单据日期 |
| `amount` | BigDecimal | ✅ | 金额 |
| `debitAmount` | BigDecimal | ⚪ | 借方金额 |
| `creditAmount` | BigDecimal | ⚪ | 贷方金额 |
| `accountCode` | String | ⚪ | 科目代码 |
| `accountName` | String | ⚪ | 科目名称 |
| `payeeName` | String | ⚪ | 收款方名称 |
| `summary` | String | ⚪ | 摘要 |
| `remark` | String | ⚪ | 备注 |
| `status` | String | ⚪ | 状态 |
| `createdAt` | LocalDateTime | ⚪ | 创建时间 |
| `updatedAt` | LocalDateTime | ⚪ | 更新时间 |

### B.2 常见 ERP 字段映射速查表

| ERP 类型 | 常见原始字段 | 目标字段 | 转换说明 |
|----------|--------------|----------|----------|
| 金蝶 K3 | FBillNo | voucherNo | 直接映射 |
| 金蝶 K3 | FDate | voucherDate | yyyy-MM-dd |
| 金蝶 K3 | FAmount | amount | 保留 2 位小数 |
| YonSuite | payNo | voucherNo | 直接映射 |
| YonSuite | payDate | voucherDate | 可能带时分秒，需截断 |
| YonSuite | payAmount | amount | 直接映射 |
| SAP | BELNR | voucherNo | 可能需要补零 |
| SAP | BUDAT | voucherDate | YYYYMMDD 格式 |
| SAP | WRBTR | amount | 注意货币转换 |

---

## 附录 C: 检查清单汇总 (Checklist Summary)

### C.1 Pass 1 完成检查清单
- [ ] SIM JSON 通过 Schema 校验
- [ ] 所有必填字段已提取
- [ ] authentication.loginUrl 可访问
- [ ] 至少有一个业务 endpoint
- [ ] fieldMappings 覆盖所有核心字段
- [ ] knownIssues 记录所有注意事项
- [ ] confidence < 0.8 的字段已人工复核

### C.2 Pass 2 完成检查清单
- [ ] 代码包含正确的 @generated 注释
- [ ] 无 mock_token 等硬编码
- [ ] 正确注入 AuthService
- [ ] 异常处理符合 7.9 规范
- [ ] 日志包含 MDC 字段
- [ ] 单元测试覆盖率达标

### C.3 上线前检查清单
- [ ] 全量单元测试通过
- [ ] 集成测试在沙箱环境验证
- [ ] 回归测试无失败
- [ ] 代码已 Review 并合入
- [ ] 数据库配置已注入
- [ ] 监控告警已配置
- [ ] 回滚方案已准备
