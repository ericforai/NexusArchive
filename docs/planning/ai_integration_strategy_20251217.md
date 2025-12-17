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
