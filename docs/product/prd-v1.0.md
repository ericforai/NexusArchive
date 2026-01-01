# 电子会计档案系统（集团私有化版）PRD v1.0

| 版本 | 日期 | 修改人 | 备注 |
| :--- | :--- | :--- | :--- |
| v1.0 | 2025-01 | PM | 基于产品说明书 v1.1 生成，包含实物管理与销毁流程 |

## 1. 全局约束与架构原则

### 1.1 核心业务逻辑
*   **全宗隔离（硬约束）**：所有业务数据必须绑定 `fonds_no`；后端从登录态/授权票据解析 `allowed_fonds` 并校验 `current_fonds_no`，请求体/URL 中的 `fonds_no` 不作为权限依据。
*   **对象访问二次校验**：访问 `archive_object_id` 等资源时，先查对象 `fonds_no` 再做授权校验。
*   **法人仅管理维度**：`entity_id` 仅用于治理、统计与合规台账，不作为数据隔离键。
*   **全宗沿革可追溯**：支持全宗迁移、合并、分立的历史沿革记录，档案随全宗走。
*   **可选业务维度（扁平标签）**：如需全宗内细分，使用 `scope_tag`/`biz_dimension`（扁平枚举/标签），仅用于列表筛选与工作分派，不参与权限推导。
*   **默认拒绝（Default Deny）**：未明确授权的功能或数据域，默认不可访问。
*   **三员分立**：`SysAdmin`（系统管理员）、`SecAdmin`（安全管理员）、`AuditAdmin`（审计管理员）权限严格互斥。

### 1.2 术语与字段对齐
*   **全宗 (Fonds)**：`sys_fonds` 为最高档案容器，`fonds_no` 为逻辑隔离键；全宗通常对应一个立档单位。在集团型架构的业务实践中，一个法人实体可能因合并、分立、历史沿革等原因管理多个全宗。
*   **法人 (Entity)**：`sys_entity` 为管理维度，关联全宗但不作为隔离键。
*   **`archive_year` = `fiscal_year`**：存量实现以 `fiscal_year`（VARCHAR）表示归档年度，PRD 统一使用 `archive_year`（INT）。
*   **全宗号展示字段**：`fonds_no` 用于盒脊标签、清册等对外展示。
*   **迁移注意**：`fiscal_year` -> `archive_year` 需数据清洗（保留 4 位年度）；`fonds_no` 作为隔离键优先复用存量字段。
*   **组织边界仅法人**：现有文档中涉及“组织/部门”的表述仅用于业务审批角色，不作为数据隔离维度。

### 1.3 数据模型概要（ER核心）
*   **Entity (法人)**: `id`, `name`, `tax_id`
*   **Fonds (全宗)**: `id`, `fonds_no`, `fonds_name`, `entity_id`, `status`, `valid_from`, `valid_to`
*   **RetentionPolicy (保管期限)**: `id`, `name`, `years`, `is_permanent`
*   **ArchiveObject (档案)**: `id`, `fonds_no`, `archive_year`, `type`, `title`, `doc_date`, `amount`, `counterparty`, `metadata_ext` (JSON/TEXT), `retention_policy_id`, `destruction_status` (NORMAL/EXPIRED/APPRAISING/DESTRUCTION_APPROVED/DESTROYED/FROZEN/HOLD), `status`
*   **BorrowRecord (借阅记录)**: `id`, `fonds_no`, `archive_year`, `archive_object_id`, `type`, `return_deadline`, `actual_return_time`, `status`
*   **AuthTicket (跨全宗授权票据)**: `id`, `applicant_id`, `source_fonds`, `target_fonds`, `scope`, `expires_at`, `status`
*   **DestructionLog (销毁清册)**: `id`, `archive_object_id`, `snapshot` (JSON/TEXT), `approved_by`, `destroyed_at`
*   **AuditLog (审计日志)**: `trace_id`, `operator`, `action`, `target`, `data_snapshot`（脱敏快照）, `prev_hash`, `curr_hash`, `sig`

### 1.4 数据分区策略
*   逻辑分区键：`fonds_no` + `archive_year`，用于隔离与冷热分层策略。
*   物理实现：通过数据库适配层或应用层路由落地，避免写死某一数据库特性。
*   分区治理：仅为有数据的全宗/年度建分区，年末预建下一年度分区；设置分区数量上限与告警。
*   索引建议：`(fonds_no, archive_year, doc_type)` BTree、核心结构化字段索引、审计表 `action_time` BTree。
*   迁移期策略：先启用复合索引，满足性能后再按数据库能力落地物理分区。

### 1.5 现有实现对齐（代码/DDL）
*   兼容阶段允许保留 `department_id` 字段，但**不得**参与权限表达式；仅用于全宗内展示/筛选/工作分派。
*   `data_scope` 仅允许 `self/all`；`dept/dept_and_child` 作为历史值降级处理，不得启用。
*   落地要求：补齐基于 `fonds_no` 的强制过滤，与扁平业务维度并行生效。
*   迁移要求：存量表主键仅 `id` 时，需同步改为复合主键并调整所有引用表外键。

---

## 2. 角色与权限系统 (RBAC + Scope)

### 2.1 系统级角色（互斥）
1.  **SysAdmin**: 用户管理、备份恢复、系统配置。**[限制]** 不可见档案内容，不可见审计日志。
2.  **SecAdmin**: 策略配置（如密码强度、水印策略）。**[限制]** 不可见档案内容。
3.  **AuditAdmin**: 查看审计日志、导出证据包。**[限制]** 不可操作业务，不可配置系统。

### 2.2 业务级角色（需绑定 Entity）
1.  **档案管理员 (Archivist)**: 归档、编目、实物装盒、发起销毁申请。
2.  **财务查阅者 (Viewer)**: 检索、预览（受控/脱敏）。
3.  **审计人员 (Auditor-Biz)**: 跨全宗查询（需特批）、查看原始凭证。

### 2.3 密钥与备份职责边界（审计可验收）
*   备份介质全量加密（数据库 + 对象存储），解密密钥由 `SecAdmin` 托管，P2 引入双人双控。
*   `SysAdmin` 可执行备份/恢复流程，但**不持有**业务明文读取密钥。
*   备份恢复需绑定审批票据与 TraceID，全链路留痕并可出具审计证据包。

### 2.4 跨全宗访问授权票据（Auth Ticket）
*   **必经票据**：跨全宗访问必须绑定 `auth_ticket_id`，且票据在有效期内。
*   **字段要求**：`applicant_id`, `source_fonds`, `target_fonds`, `scope`（全宗/期间/类型/关键词）, `expires_at`, `approval_chain`（双审批/复核）, `status`（active/revoked/expired）。
*   **审计绑定**：所有跨全宗访问日志必须写入 `auth_ticket_id` 与审批链快照，证据包导出需包含票据与审批快照。

---

## 3. 功能模块详情

### 模块一：档案归集与实物管理 (P0/P1)

#### 1.1 电子归档与关联
*   **用户故事**: 作为档案管理员，我希望上传电子发票 XML 时，系统自动解析并关联对应的 PDF 版式文件。
*   **功能逻辑**:
    1.  文件上传 -> 计算 SHA256 哈希（判重）。
    2.  解析 XML/OFD 元数据 -> 提取发票号、金额、日期。
    3.  **关联引擎**: 查找是否存在匹配的“记账凭证” -> 若存在，建立 `Link` 关系。
*   **验收标准**: 重复文件提示“已存在”；元数据提取准确无误。



---

### 模块二：检索与利用 (P0/P1)

#### 2.1 高级检索与脱敏
*   **用户故事**: 作为财务查阅者，我需要按“金额范围”和“摘要”搜索凭证，但不能看到敏感的银行账号。
*   **功能逻辑**:
    1.  **查询范围**: 仅限用户 `current_scope` 内的 `fonds_no`。
    2.  **列表展示**: 命中结果展示。
    3.  **脱敏规则**: 若用户无 `FULL_ACCESS` 权限，返回前端的 JSON 中 `bank_account` 字段中间 8 位替换为 `********`。

#### 2.2 流式预览与动态水印 (P1)
*   **用户故事**: 我预览一份 500 页的合同时，不希望等待全量下载，且屏幕上必须有可追责的动态水印。
*   **前端逻辑**:
    *   组件：PDF/OFD 阅读器。
    *   **大文件流式加载**: 仅请求当前视口页面的数据流，支持 Range/分块读取。
    *   **动态水印**: `Canvas` 覆盖全屏，内容 = `User.name + Timestamp + TraceID`，副文本 = `TraceID + FondsNo`（用于追责与威慑）。
    *   **防篡改**: 监听 DOM 变动，若水印被移除，立即重绘或强制白屏，并上报安全日志。
*   **高敏模式**: `mode=rendered` 时由服务端按页渲染带水印的流式内容，`security_level=SECRET` 强制启用。
*   **后端接口**: `POST /api/archive/preview` 返回流式数据或预签名 URL，且包含水印元数据。

---

### 模块三：四性检测与合规 (P0/P1)

#### 3.1 四性检测要求
*   **真实性**: 文件哈希 + 数字签名校验（支持 SM2/SM3），并验证时间戳与签章链路。
*   **完整性**: 结构化元数据完整性校验；XML 元数据与版式文件（OFD/PDF）解析结果一致性校验（金额、日期、对方等关键字段）。
*   **可用性**: Magic Number 校验 + 结构化解析（Dry Parse）；禁止仅靠扩展名判断。
*   **安全性**: 入库与预览前病毒扫描（如 ClamAV），留存扫描记录与结果。

#### 3.2 元数据结构化要求
*   四性检测所需核心元数据必须结构化存储（金额、日期、对方、凭证号等）。
*   `metadata_ext` 仅承载扩展信息，禁止替代核心字段。

### 模块四：生命周期与销毁 (P2)

#### 4.1 鉴定与销毁流程
*   **用户故事**: 档案到期后，需生成鉴定清单，经审批后进行逻辑销毁，但保留销毁记录。
*   **状态机**: `Normal` -> `Expired` -> `Appraising` -> `DESTRUCTION_APPROVED` -> `Destroyed`；可进入 `FROZEN`（审计/诉讼冻结）与 `HOLD`（保全）状态，冻结/保全未解除不得销毁。
*   **核心逻辑**:
    *   **在借校验**: 若存在 `borrow_record.status = BORROWED`，禁止销毁；仅允许 `RETURNED` 或无借阅记录的档案进入销毁。
    *   **逻辑销毁**: `archive_object` 表中标记 `destruction_status = 'DESTROYED'`, 清空文件引用/对象存储指针（物理文件删除），但**保留**核心元数据与 `metadata_ext`。
    *   **销毁清册**: 将本次销毁的所有元数据快照写入 `destruction_log` 表（永久只读，独立存储，保留哈希链）。
    *   **分类证据**: 电子文件销毁、纸质销毁、介质销毁分别留存复核记录与附件证据。
    *   **备份口径**: 销毁完成后新备份不包含明文；历史备份按制度口径处理并形成审计留痕。

---

## 4. 数据库设计 (Schema 核心)

### 4.1 逻辑模型 (SQL-92)
*   逻辑隔离键：`fonds_no` + `archive_year`。
*   核心元数据结构化存储，`metadata_ext` 仅承载扩展信息。
*   物理分区由数据库适配层或应用层路由实现，避免写死特定数据库语法。

```sql
-- 1. 法人实体表 (管理维度)
CREATE TABLE sys_entity (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    tax_code VARCHAR(50) UNIQUE,
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

-- 2. 全宗表 (隔离维度)
CREATE TABLE sys_fonds (
    id VARCHAR(32) PRIMARY KEY,
    fonds_no VARCHAR(50) NOT NULL UNIQUE,
    fonds_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(32),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    valid_from DATE,
    valid_to DATE,
    FOREIGN KEY (entity_id) REFERENCES sys_entity(id)
);

-- 3. 保管期限表
CREATE TABLE retention_policy (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    years INT NOT NULL,
    is_permanent BOOLEAN DEFAULT FALSE
);

-- 4. 档案主表 (逻辑隔离键为 fonds_no + archive_year)
CREATE TABLE archive_object (
    id VARCHAR(32) NOT NULL,
    fonds_no VARCHAR(50) NOT NULL,
    archive_year INT NOT NULL,
    doc_type VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    doc_date DATE,
    amount DECIMAL(18, 2),
    counterparty VARCHAR(100),
    voucher_no VARCHAR(50),
    invoice_no VARCHAR(50),
    retention_policy_id VARCHAR(32) NOT NULL,
    destruction_status VARCHAR(20) DEFAULT 'NORMAL',
    status VARCHAR(20) DEFAULT 'NORMAL',
    security_level VARCHAR(20) DEFAULT 'INTERNAL',
    metadata_ext TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, fonds_no, archive_year),
    FOREIGN KEY (fonds_no) REFERENCES sys_fonds(fonds_no),
    FOREIGN KEY (retention_policy_id) REFERENCES retention_policy(id)
);

-- 5. 借阅记录
CREATE TABLE borrow_record (
    id VARCHAR(32) NOT NULL,
    fonds_no VARCHAR(50) NOT NULL,
    archive_year INT NOT NULL,
    archive_object_id VARCHAR(32) NOT NULL,
    type VARCHAR(20) NOT NULL, -- electronic, physical
    status VARCHAR(20) NOT NULL,
    return_deadline TIMESTAMP,
    actual_return_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, fonds_no, archive_year),
    FOREIGN KEY (archive_object_id, fonds_no, archive_year)
        REFERENCES archive_object(id, fonds_no, archive_year)
);

-- 6. 销毁清册 (永久保留)
CREATE TABLE destruction_log (
    id VARCHAR(32) NOT NULL,
    fonds_no VARCHAR(50) NOT NULL,
    archive_year INT NOT NULL,
    archive_object_id VARCHAR(32) NOT NULL,
    retention_policy_id VARCHAR(32) NOT NULL,
    approval_ticket_id VARCHAR(64) NOT NULL,
    destroyed_by VARCHAR(32) NOT NULL,
    destroyed_at TIMESTAMP NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    snapshot TEXT NOT NULL,
    prev_hash VARCHAR(128),
    curr_hash VARCHAR(128),
    sig TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, fonds_no, archive_year),
    FOREIGN KEY (archive_object_id, fonds_no, archive_year)
        REFERENCES archive_object(id, fonds_no, archive_year)
);

-- 7. 审计日志 (证据链)
CREATE TABLE audit_log (
    id BIGSERIAL NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(32) NOT NULL,
    fonds_no VARCHAR(50) NOT NULL,
    archive_year INT NOT NULL,
    action VARCHAR(50) NOT NULL,
    resource_id VARCHAR(32),
    client_ip VARCHAR(50),
    action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_snapshot TEXT NOT NULL,
    prev_hash VARCHAR(128) NOT NULL,
    curr_hash VARCHAR(128) NOT NULL,
    sig TEXT,
    PRIMARY KEY (id, fonds_no, archive_year)
);

-- 8. 跨全宗授权票据
CREATE TABLE auth_ticket (
    id VARCHAR(32) PRIMARY KEY,
    applicant_id VARCHAR(32) NOT NULL,
    source_fonds VARCHAR(50) NOT NULL,
    target_fonds VARCHAR(50) NOT NULL,
    scope TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    approval_snapshot TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. 实物位置与装盒
CREATE TABLE arc_location (
    id VARCHAR(32) NOT NULL,
    fonds_no VARCHAR(50) NOT NULL,
    warehouse_code VARCHAR(50) NOT NULL,
    aisle VARCHAR(20),
    rack VARCHAR(20),
    shelf VARCHAR(20),
    slot VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, fonds_no)
);

CREATE TABLE arc_box (
    id VARCHAR(32) NOT NULL,
    fonds_no VARCHAR(50) NOT NULL,
    archive_year INT NOT NULL,
    box_code VARCHAR(64) NOT NULL,
    location_id VARCHAR(32),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, fonds_no, archive_year),
    FOREIGN KEY (location_id, fonds_no)
        REFERENCES arc_location(id, fonds_no)
);

CREATE TABLE arc_box_item (
    id VARCHAR(32) NOT NULL,
    fonds_no VARCHAR(50) NOT NULL,
    archive_year INT NOT NULL,
    box_id VARCHAR(32) NOT NULL,
    archive_object_id VARCHAR(32) NOT NULL,
    boxed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    boxed_by VARCHAR(32),
    PRIMARY KEY (id, fonds_no, archive_year),
    FOREIGN KEY (box_id, fonds_no, archive_year)
        REFERENCES arc_box(id, fonds_no, archive_year),
    FOREIGN KEY (archive_object_id, fonds_no, archive_year)
        REFERENCES archive_object(id, fonds_no, archive_year)
);

CREATE TABLE arc_inventory_task (
    id VARCHAR(32) NOT NULL,
    fonds_no VARCHAR(50) NOT NULL,
    archive_year INT NOT NULL,
    mode VARCHAR(20) NOT NULL, -- normal, blind
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, fonds_no, archive_year)
);

CREATE TABLE arc_inventory_result (
    id VARCHAR(32) NOT NULL,
    fonds_no VARCHAR(50) NOT NULL,
    archive_year INT NOT NULL,
    task_id VARCHAR(32) NOT NULL,
    result_status VARCHAR(20) NOT NULL, -- in_stock, missing, mismatch, damaged
    notes VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, fonds_no, archive_year),
    FOREIGN KEY (task_id, fonds_no, archive_year)
        REFERENCES arc_inventory_task(id, fonds_no, archive_year)
);
```

### 4.2 数据库适配与隔离兜底
*   **数据库适配层**：为 PostgreSQL/达梦/金仓提供独立 DDL 与类型映射。
*   **隔离兜底**：核心表使用复合主外键（`fonds_no` + `archive_year`）。
*   **可选 RLS**：数据库支持时启用行级安全，作为防御深度手段。

---

## 5. API 设计

### 5.1 `POST /api/archive/preview`
*   **用途**: 档案预览，返回流式数据或预签名 URL，包含水印元数据。
*   **兼容说明**: 现有 `GET /api/archive/{id}/content` 仍用于下载或传统预览，`POST /api/archive/preview` 为增强预览入口（动态水印/流式加载）。
*   **请求参数**:
    *   `archive_object_id`, `file_id`（可选）
    *   `current_fonds_no` 从登录态或 `X-Fonds-No` header 获取，必须属于 `allowed_fonds`
    *   `mode`: `stream` | `presigned` | `rendered`
    *   `client`: `range_supported`, `max_chunk_kb`
*   **响应 (stream)**:
    *   HTTP `200/206`，支持 `Accept-Ranges`
    *   响应头包含水印元数据：`X-Trace-Id`, `X-Watermark-Text`, `X-Watermark-Subtext`, `X-Watermark-Opacity`, `X-Watermark-Rotate`
*   **响应 (presigned)**:
    *   JSON 返回 `presigned_url`, `expires_at`, `trace_id`, `watermark` 元数据
*   **响应 (rendered)**:
    *   HTTP `200/206`，服务端按页渲染带水印内容，适用于高敏档案
*   **审计要求**:
    *   每次预览必须记录审计日志，`trace_id` 全链路贯穿。

---

## 6. 审计规范与日志

### 6.1 电子档案销毁审计规范
*   **销毁前**: 校验保管期限/冻结状态，备份完整性检查，审批链完整性核对。
*   **销毁中**: 双人复核，记录销毁方式、执行人、TraceID、时间戳。
*   **销毁后**: 生成销毁清册（WORM/不可变存储），清册哈希链可验真。
*   **清册归档格式**: `archive_object_id`, `fonds_no`, `archive_year`, `retention_policy_id`, `approved_by`, `destroyed_by`, `destroyed_at`, `trace_id`, `snapshot_hash`。

### 6.2 审计日志防篡改要求
*   **哈希链**：每条日志包含 `prev_hash`，由数据库触发器或应用层强制生成 `curr_hash`，形成可验真的链式结构。
*   **验真**：提供链路校验接口，支持抽检与证据包导出。

### 6.3 跨全宗访问审计日志格式
*   **必填字段**: `user_id`, `source_fonds`, `target_fonds`, `auth_ticket_id`, `trace_id`, `action`。
*   **推荐字段**: `timestamp`, `resource_id`, `result`, `ip`, `user_agent`。

---

## 7. 运行与合规补充

### 7.1 身份与账号生命周期
*   入职/离职/调岗需触发账号创建/停用/权限回收。
*   最小权限与定期复核（Access Review）为强制流程。
*   登录失败锁定、口令策略与可选 MFA 需可配置。

### 7.2 冻结/保全（Legal Hold）
*   审计/诉讼/合规触发冻结或保全，生成票据并记录原因与期限。
*   冻结/保全期间禁止销毁与敏感导出，解除需审批。

### 7.3 文件存储与防病毒
*   对象存储与本地存储可配置；归档后写入不可变桶或启用保留策略。
*   哈希去重仅限同全宗或授权范围内，避免跨全宗数据关联泄露。
*   文件入库/预览前触发病毒扫描（如 ClamAV），记录扫描结果。

### 7.4 不可变证据链
*   P0/P1 采用哈希链（`prev_hash`/`curr_hash`），P2 可引入 SM2 签名增强不可抵赖性。
*   支持证据链验真接口与抽检报告输出。

### 7.5 非功能指标
*   单全宗容量、并发检索、最大文件大小、预览首屏时间、日志留存周期需量化。
*   数据库版本需满足适配层分区与索引能力要求。

---

## 8. 开发指令集 (Cursor Prompts)

**请按照以下步骤，将这段 Prompt 复制给 Cursor/AI，即可生成项目骨架。**

### 步骤 1: 初始化后端 (Spring Boot)

> **Prompt:**
> 你是资深 Java 架构师。我正在开发“电子会计档案系统”。
> 请基于 Spring Boot 3.1.6 + MyBatis Plus + PostgreSQL/达梦/金仓 创建项目结构。
>
> **核心要求：**
> 1.  **实体类 (Entity)**: 生成 `SysEntity`, `SysFonds`, `RetentionPolicy`, `ArchiveObject`, `BorrowRecord`, `DestructionLog`, `SysUser`, `AuditLog`。注意 `ArchiveObject` 必须包含 `fondsNo`, `archiveYear`, `retentionPolicyId`, `destructionStatus`。
> 2.  **MyBatis 拦截器**: 请编写一个 `EntitySecurityInterceptor`，实现 `InnerInterceptor` 接口。
>     *   逻辑：在所有 SQL 执行前（除了 SysAdmin 操作），强制在 WHERE 子句中追加 `AND fonds_no = {current_fonds_scope}`；`fonds_no` 必须从登录态解析，不得信任请求入参。
> 3.  **数据库兜底**: 复合主外键（包含 `fonds_no`/`archive_year`）作为隔离兜底，数据库特性按适配层实现。
> 4.  **Controller**: 生成 `ArchiveController`，包含 `upload`, `search` (支持结构化字段 + 扩展字段查询), `preview` 接口（支持流式或预签名返回）。

### 步骤 2: 实现权限与三员分立

> **Prompt:**
> 请实现基于 Spring Security 的权限控制逻辑。
> 1.  定义注解 `@RequiresThreeRole(RoleType.SYS_ADMIN)`。
> 2.  编写 AOP 切面：
>     *   如果是 `SysAdmin` 尝试访问 `ArchiveService` 的业务方法，直接抛出 `AccessDeniedException`（系统管理员不可见业务数据）。
>     *   如果是 `AuditAdmin` 尝试调用 `save/update/delete` 方法，直接抛出异常（审计员只读）。
> 3.  实现 `DynamicWatermarkService`：生成包含“用户名+时间戳+TraceID”的文本，用于前端水印。

### 步骤 3: 前端页面开发 (React)

> **Prompt:**
> 你是前端专家。请使用 React 19 + Ant Design 6 生成“档案检索与预览”页面（如生态不兼容，自动降级为 React 18 + Ant Design 5）。
> 1.  **搜索栏**: 包含“全宗选择（下拉）”、“档案类型”、“关键字”、“金额区间”。
> 2.  **列表**: 展示档案表格，敏感字段（如 `bankAccount`）根据后端返回的 `isMasked` 字段决定是否显示星号。
> 3.  **预览弹窗**:
>     *   使用 `pdfjs-dist` 或 `ofd.js` 渲染文件。
>     *   **关键**: 编写一个 `WatermarkOverlay` 组件，创建一个全屏 `div`，`z-index: 9999`，`pointer-events: none`，背景使用 Canvas 绘制倾斜重复的水印文字。
>     *   使用 `MutationObserver` 保护水印层，如果被删除则自动重新插入或触发白屏。

### 步骤 4: 实物管理逻辑

> **Prompt:**
> 请生成“实物装盒”功能的业务逻辑代码（Service层）。
> 1.  方法 `boxArchives(List<String> archiveIds, String boxCode)`。
> 2.  逻辑：
>     *   检查所有 `archiveIds` 是否属于同一个 `fonds_no` 和 `archive_year`。
>     *   更新这些档案的装盒关联（`arc_box_item`）。
>     *   生成一条 `AuditLog`，action="PHYSICAL_BOXING"。
> 3.  生成一个工具类 `LabelPrinter`，输入 `BoxInfo`，调用 PDF 库（如 iText）生成 10cm x 3cm 的标签 PDF，包含二维码。

---

## 9. 验收测试用例 (QA Checklist)

1.  **数据隔离测试**：
    *   登录全宗 A 的账号，直接通过 API 修改参数请求全宗 B 的 `fonds_no` 数据 -> **预期**：返回 403 Forbidden 或 空数据（忽略入参并按授权过滤）。
2.  **数据库兜底测试**：
    *   关闭应用层过滤或手写 SQL -> **预期**：复合主外键或适配层隔离阻断跨全宗访问。
3.  **三员互斥测试**：
    *   使用 `sysadmin` 账号登录，尝试访问“档案检索”页面 -> **预期**：提示“权限不足”或菜单不可见。
4.  **四性-真实性测试**：
    *   提交无效签名文件 -> **预期**：真实性检测失败并阻断归档。
5.  **四性-可用性测试**：
    *   文件扩展名与 Magic Number 不一致 -> **预期**：可用性检测失败。
6.  **四性-完整性测试**：
    *   XML 元数据金额与版式文件解析金额不一致 -> **预期**：完整性检测失败并记录差异。
7.  **水印测试**：
    *   在浏览器 F12 开发者工具中，手动删除水印 DOM 节点 -> **预期**：节点立即被 JS 重新插入，或屏幕变白并在后台记录一次“篡改尝试”。
8.  **水印渲染模式测试**：
    *   预览高敏档案，`mode=rendered` -> **预期**：返回服务端渲染水印的流式内容。
9.  **实物借阅审批测试**：
    *   发起实物借阅申请 -> 审批通过 -> 出库登记 -> 归还登记 -> **预期**：`borrow_record` 状态正确，逾期提醒可用。
10.  **库房盘点测试**：
    *   生成盘点清单并扫描核对 -> **预期**：异常项可追溯并产生审计日志。
11.  **盲盘测试**：
    *   启动盲盘模式 -> **预期**：系统不下发清单，自动比对差异并输出报告。
12.  **销毁测试**：
    *   执行销毁流程 -> **预期**：文件无法下载，但数据库中仍能查到该档案的元数据，且 `destruction_log` 表中有一条快照记录（永久保留）。
    *   档案处于借阅中（`BORROWED`）或 `FROZEN/HOLD` -> **预期**：禁止销毁。
13.  **跨全宗票据测试**：
    *   未绑定 `auth_ticket_id` 的跨全宗查询 -> **预期**：拒绝；绑定有效票据 -> **预期**：允许并记录审计。
