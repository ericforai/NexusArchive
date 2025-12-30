# 电子会计档案系统（集团私有化版）PRD v1.0

| 版本 | 日期 | 修改人 | 备注 |
| :--- | :--- | :--- | :--- |
| v1.0 | 2025-01 | PM | 基于产品说明书 v1.1 生成，包含实物管理与销毁流程 |

## 1. 全局约束与架构原则

### 1.1 核心业务逻辑
*   **多法人隔离**：所有业务数据必须绑定 `entity_id`；后端从登录态/授权票据解析 `allowed_entity_ids` 并校验 `current_entity_id`，请求体/URL 中的 `entity_id` 不作为权限依据。
*   **对象访问二次校验**：访问 `archive_object_id` 等资源时，先查对象 `entity_id` 再做授权校验。
*   **法人唯一隔离（硬约束）**：法人是唯一隔离边界，权限判定与数据隔离只基于 `entity_id`/`fonds_no`；`department_id` 仅用于法人内展示/筛选/分派，**不得**用于权限表达式。
*   **可选业务维度（扁平标签）**：如需法人内细分，使用 `scope_tag`/`biz_dimension`（扁平枚举/标签），仅用于列表筛选与工作分派，不参与权限推导。
*   **默认拒绝（Default Deny）**：未明确授权的功能或数据域，默认不可访问。
*   **三员分立**：`SysAdmin`（系统管理员）、`SecAdmin`（安全管理员）、`AuditAdmin`（审计管理员）权限严格互斥。

### 1.2 术语与字段对齐
*   **法人实体/`entity_id` = 全宗/`fonds_id`（bas_fonds.id）**：现有实现与历史文档中的“全宗”用于表示法人边界，业务表中常用 `fonds_no` 作为法人编号。
*   **`archive_year` = `fiscal_year`**：存量实现以 `fiscal_year`（VARCHAR）表示归档年度，PRD 统一使用 `archive_year`（INT）。
*   **全宗号展示字段**：`sys_entity.fonds_code` 用于盒脊标签、清册等对外展示；`entity_id` 为系统内部主键。
*   **组织边界仅法人**：现有文档中涉及“组织/部门”的表述仅用于业务审批角色，不作为数据隔离维度。

### 1.3 数据模型概要（ER核心）
*   **Entity (法人)**: `id`, `fonds_code`, `name`, `tax_id`
*   **RetentionPolicy (保管期限)**: `id`, `name`, `years`, `is_permanent`
*   **ArchiveObject (档案)**: `id`, `entity_id`, `archive_year`, `type`, `metadata` (JSON), `retention_policy_id`, `destruction_status` (NORMAL/EXPIRED/APPRAISING/APPROVED/DESTROYED/FROZEN/HOLD), `physical_location` (JSONB), `status`
*   **BorrowRecord (借阅记录)**: `id`, `entity_id`, `archive_year`, `archive_object_id`, `type`, `return_deadline`, `actual_return_time`, `status`
*   **AuthTicket (跨法人授权票据)**: `id`, `applicant_id`, `source_entity`, `target_entity`, `scope`, `expires_at`, `status`
*   **DestructionLog (销毁清册)**: `id`, `archive_object_id`, `snapshot` (JSONB), `approved_by`, `destroyed_at`
*   **AuditLog (审计日志)**: `trace_id`, `operator`, `action`, `target`, `data_snapshot`（脱敏快照）, `prev_hash`, `curr_hash`, `sig`

### 1.4 数据分区策略
*   所有大表按 `entity_id`（List）+ `archive_year`（Range）复合分区。
*   适用表：`archive_object`, `borrow_record`, `audit_log`, `destruction_log`。
*   分区目的：法人隔离、冷热分层、提升年度查询性能。
*   分区治理：仅为有数据的法人/年度建分区，年末预建下一年度分区；设置分区数量上限与告警。
*   索引建议：`(entity_id, archive_year, doc_type)` BTree、`metadata` GIN、审计表 `action_time` BTree。

### 1.5 现有实现对齐（代码/DDL）
*   兼容阶段允许保留 `department_id` 字段，但**不得**参与权限表达式；仅用于法人内展示/筛选/工作分派。
*   `data_scope` 仅允许 `self/all`；`dept/dept_and_child` 作为历史值降级处理，不得启用。
*   落地要求：补齐基于 `entity_id`/`fonds_no` 的强制过滤，与扁平业务维度并行生效。

---

## 2. 角色与权限系统 (RBAC + Scope)

### 2.1 系统级角色（互斥）
1.  **SysAdmin**: 用户管理、备份恢复、系统配置。**[限制]** 不可见档案内容，不可见审计日志。
2.  **SecAdmin**: 策略配置（如密码强度、水印策略）。**[限制]** 不可见档案内容。
3.  **AuditAdmin**: 查看审计日志、导出证据包。**[限制]** 不可操作业务，不可配置系统。

### 2.2 业务级角色（需绑定 Entity）
1.  **档案管理员 (Archivist)**: 归档、编目、实物装盒、发起销毁申请。
2.  **财务查阅者 (Viewer)**: 检索、预览（受控/脱敏）。
3.  **审计人员 (Auditor-Biz)**: 跨法人查询（需特批）、查看原始凭证。

### 2.3 密钥与备份职责边界（审计可验收）
*   备份介质全量加密（数据库 + 对象存储），解密密钥由 `SecAdmin` 或双人双控托管。
*   `SysAdmin` 可执行备份/恢复流程，但**不持有**业务明文读取密钥。
*   备份恢复需绑定审批票据与 TraceID，全链路留痕并可出具审计证据包。

### 2.4 跨法人访问授权票据（Auth Ticket）
*   **必经票据**：跨法人访问必须绑定 `auth_ticket_id`，且票据在有效期内。
*   **字段要求**：`applicant_id`, `source_entity`, `target_entity`, `scope`（法人/期间/类型/关键词）, `expires_at`, `approval_chain`（双审批/复核）, `status`（active/revoked/expired）。
*   **审计绑定**：所有跨法人访问日志必须写入 `auth_ticket_id` 与审批链快照，证据包导出需包含票据与审批快照。

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

#### 1.2 实物装盒与打印盒脊标签 (P1)
*   **用户故事**: 作为档案管理员，我需要将纸质凭证装订入盒，并打印盒脊标签贴在档案盒上。
*   **功能逻辑**:
    1.  **创建盒子**: 输入“所属法人”、“会计年度”、“档案类型”、“保管期限” -> 系统自动生成 `box_code` (规则：`法人-年份-类型-流水号`)。
    2.  **装盒操作**: 扫描/勾选档案条目 -> 绑定到该 `box_code`。
    3.  **打印标签**: 生成盒脊标签 PDF（含二维码、盒号、起止日期、全宗号）。
    4.  **标签格式**: `fonds_code-archive_year-retention-serial`（示例：`JD-2025-永久-001`）。
*   **数据字段**: `box_table` (`id`, `code`, `entity_id`, `fonds_code`, `location_str`, `status`)。

#### 1.3 实物借阅审批 (P1)
*   **用户故事**: 作为库房管理员，我需要对实物借阅申请进行审批，并记录出库/归还时间。
*   **功能逻辑**:
    1.  借阅申请 -> 审批（可双人复核） -> 出库登记。
    2.  系统生成 `borrow_record`，`type=physical`，设置 `return_deadline`。
    3.  归还时登记 `actual_return_time`，状态流转 `BORROWED` -> `RETURNED`。
    4.  逾期自动预警并写入审计日志。
*   **验收标准**: 审批链完整；逾期提醒可追溯。

#### 1.4 库房盘点 (P1)
*   **用户故事**: 作为档案管理员，我需要生成盘点清单并核对库房实物位置，发现异常并记录。
*   **功能逻辑**:
    1.  按法人/年度/库房生成盘点清单（支持二维码扫描）。
    2.  **盲盘模式**: 不下发清单，库管员扫描现物，系统自动比对差异并输出报告。
    3.  盘点结果：在库、缺失、位置不符、破损。
    4.  异常项生成整改任务与审计记录。
*   **验收标准**: 清单可导出；异常项留痕可追溯。

---

### 模块二：检索与利用 (P0/P1)

#### 2.1 高级检索与脱敏
*   **用户故事**: 作为财务查阅者，我需要按“金额范围”和“摘要”搜索凭证，但不能看到敏感的银行账号。
*   **功能逻辑**:
    1.  **查询范围**: 仅限用户 `current_scope` 内的 `entity_id`。
    2.  **列表展示**: 命中结果展示。
    3.  **脱敏规则**: 若用户无 `FULL_ACCESS` 权限，返回前端的 JSON 中 `bank_account` 字段中间 8 位替换为 `********`。

#### 2.2 流式预览与动态水印 (P1)
*   **用户故事**: 我预览一份 500 页的合同时，不希望等待全量下载，且屏幕上必须有可追责的动态水印。
*   **前端逻辑**:
    *   组件：PDF/OFD 阅读器。
    *   **大文件流式加载**: 仅请求当前视口页面的数据流，支持 Range/分块读取。
    *   **动态水印**: `Canvas` 覆盖全屏，内容 = `User.name + Timestamp + TraceID`，副文本 = `TraceID + Entity`（用于追责与威慑）。
    *   **防篡改**: 监听 DOM 变动，若水印被移除，立即重绘或强制白屏，并上报安全日志。
*   **高敏模式**: `mode=rendered` 时由服务端按页渲染带水印的流式内容。
*   **后端接口**: `POST /api/archive/preview` 返回流式数据或预签名 URL，且包含水印元数据。

---

### 模块三：生命周期与销毁 (P2)

#### 3.1 鉴定与销毁流程
*   **用户故事**: 档案到期后，需生成鉴定清单，经审批后进行逻辑销毁，但保留销毁记录。
*   **状态机**: `Normal` -> `Expired` -> `Appraising` -> `Destruction_Approved` -> `Destroyed`；可进入 `FROZEN`（审计/诉讼冻结）与 `HOLD`（保全）状态，冻结/保全未解除不得销毁。
*   **核心逻辑**:
    *   **在借校验**: 若存在 `borrow_record.status = BORROWED`，禁止销毁；仅允许 `RETURNED` 或无借阅记录的档案进入销毁。
    *   **逻辑销毁**: `archive_object` 表中标记 `destruction_status = 'DESTROYED'`, 清空 `file_path`（物理文件删除），但**保留** `metadata`。
    *   **销毁清册**: 将本次销毁的所有元数据快照写入 `destruction_log` 表（永久只读，独立存储，保留哈希链）。
    *   **分类证据**: 电子文件销毁、纸质销毁、介质销毁分别留存复核记录与附件证据。
    *   **备份口径**: 销毁完成后新备份不包含明文；历史备份按制度口径处理并形成审计留痕。

---

## 4. 数据库设计 (Schema 核心)

### 4.1 分区策略
*   采用复合分区：`LIST(entity_id)` + `RANGE(archive_year)`。
*   大表统一执行复合分区（`archive_object`, `borrow_record`, `audit_log`, `destruction_log`）。
*   PostgreSQL 分区表主键必须包含分区键（`entity_id`, `archive_year`）。

```sql
-- 1. 法人实体表 (租户隔离基石)
CREATE TABLE sys_entity (
    id VARCHAR(32) PRIMARY KEY,
    fonds_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    tax_code VARCHAR(50) UNIQUE,
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

-- 2. 保管期限表
CREATE TABLE retention_policy (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    years INT NOT NULL,
    is_permanent BOOLEAN DEFAULT FALSE
);

-- 3. 档案主表 (按法人 + 年度复合分区)
CREATE TABLE archive_object (
    id VARCHAR(32) NOT NULL,
    entity_id VARCHAR(32) NOT NULL REFERENCES sys_entity(id),
    archive_year INT NOT NULL,
    doc_type VARCHAR(20) NOT NULL,
    metadata JSONB NOT NULL,
    file_path VARCHAR(255),
    file_hash VARCHAR(64),
    retention_policy_id VARCHAR(32) NOT NULL REFERENCES retention_policy(id),
    destruction_status VARCHAR(20) DEFAULT 'NORMAL',
    physical_location JSONB,
    security_level VARCHAR(20) DEFAULT 'INTERNAL',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (id, entity_id, archive_year)
) PARTITION BY LIST (entity_id);

-- 4. 借阅记录 (按法人 + 年度复合分区)
CREATE TABLE borrow_record (
    id VARCHAR(32) NOT NULL,
    entity_id VARCHAR(32) NOT NULL REFERENCES sys_entity(id),
    archive_year INT NOT NULL,
    archive_object_id VARCHAR(32) NOT NULL,
    type VARCHAR(20) NOT NULL, -- electronic, physical
    status VARCHAR(20) NOT NULL,
    return_deadline TIMESTAMP,
    actual_return_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (id, entity_id, archive_year),
    FOREIGN KEY (archive_object_id, entity_id, archive_year)
        REFERENCES archive_object(id, entity_id, archive_year)
) PARTITION BY LIST (entity_id);

-- 5. 销毁清册 (永久保留，按法人 + 年度复合分区)
CREATE TABLE destruction_log (
    id VARCHAR(32) NOT NULL,
    entity_id VARCHAR(32) NOT NULL REFERENCES sys_entity(id),
    archive_year INT NOT NULL,
    archive_object_id VARCHAR(32) NOT NULL,
    retention_policy_id VARCHAR(32) NOT NULL,
    approval_ticket_id VARCHAR(64) NOT NULL,
    destroyed_by VARCHAR(32) NOT NULL,
    destroyed_at TIMESTAMP NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    snapshot JSONB NOT NULL,
    prev_hash VARCHAR(128),
    curr_hash VARCHAR(128),
    sig TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (id, entity_id, archive_year),
    FOREIGN KEY (archive_object_id, entity_id, archive_year)
        REFERENCES archive_object(id, entity_id, archive_year)
) PARTITION BY LIST (entity_id);

-- 6. 审计日志 (证据链)
CREATE TABLE audit_log (
    id BIGSERIAL NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(32) NOT NULL,
    entity_id VARCHAR(32) NOT NULL,
    archive_year INT NOT NULL,
    action VARCHAR(50) NOT NULL,
    resource_id VARCHAR(32),
    client_ip VARCHAR(50),
    action_time TIMESTAMP DEFAULT NOW(),
    data_snapshot JSONB NOT NULL,
    prev_hash VARCHAR(128),
    curr_hash VARCHAR(128),
    sig TEXT,
    PRIMARY KEY (id, entity_id, archive_year)
) PARTITION BY LIST (entity_id);

-- 7. 复合分区示例 (entity_id + archive_year)
CREATE TABLE archive_object_e001 PARTITION OF archive_object
    FOR VALUES IN ('E001') PARTITION BY RANGE (archive_year);
CREATE TABLE archive_object_e001_2024 PARTITION OF archive_object_e001
    FOR VALUES FROM (2024) TO (2025);
```

---

## 5. API 设计

### 5.1 `POST /api/archive/preview`
*   **用途**: 档案预览，返回流式数据或预签名 URL，包含水印元数据。
*   **兼容说明**: 现有 `GET /api/archive/{id}/content` 仍用于下载或传统预览，`POST /api/archive/preview` 为增强预览入口（动态水印/流式加载）。
*   **请求参数**:
    *   `entity_id`, `archive_object_id`, `file_id`（可选）
    *   `mode`: `stream` | `presigned`
    *   `client`: `range_supported`, `max_chunk_kb`
*   **响应 (stream)**:
    *   HTTP `200/206`，支持 `Accept-Ranges`
    *   响应头包含水印元数据：`X-Trace-Id`, `X-Watermark-Text`, `X-Watermark-Subtext`, `X-Watermark-Opacity`, `X-Watermark-Rotate`
*   **响应 (presigned)**:
    *   JSON 返回 `presigned_url`, `expires_at`, `trace_id`, `watermark` 元数据
*   **审计要求**:
    *   每次预览必须记录审计日志，`trace_id` 全链路贯穿。

---

## 6. 审计规范与日志

### 6.1 电子档案销毁审计规范
*   **销毁前**: 校验保管期限/冻结状态，备份完整性检查，审批链完整性核对。
*   **销毁中**: 双人复核，记录销毁方式、执行人、TraceID、时间戳。
*   **销毁后**: 生成销毁清册（WORM/不可变存储），清册哈希链可验真。

### 6.2 跨法人访问审计日志格式
*   **必填字段**: `user_id`, `source_entity`, `target_entity`, `auth_ticket_id`, `trace_id`, `action`。
*   **推荐字段**: `timestamp`, `resource_id`, `result`, `ip`, `user_agent`。

---

## 7. 开发指令集 (Cursor Prompts)

**请按照以下步骤，将这段 Prompt 复制给 Cursor/AI，即可生成项目骨架。**

### 步骤 1: 初始化后端 (Spring Boot)

> **Prompt:**
> 你是资深 Java 架构师。我正在开发“电子会计档案系统”。
> 请基于 Spring Boot 3 + MyBatis Plus + PostgreSQL 创建项目结构。
>
> **核心要求：**
> 1.  **实体类 (Entity)**: 生成 `SysEntity`, `RetentionPolicy`, `ArchiveObject`, `BorrowRecord`, `DestructionLog`, `SysUser`, `AuditLog`。注意 `ArchiveObject` 必须包含 `entityId`, `archiveYear`, `retentionPolicyId`, `destructionStatus`, `physicalLocation`。
> 2.  **MyBatis 拦截器**: 请编写一个 `EntitySecurityInterceptor`，实现 `InnerInterceptor` 接口。
>     *   逻辑：在所有 SQL 执行前（除了 SysAdmin 操作），强制在 WHERE 子句中追加 `AND entity_id = {current_user_scope}`，防止跨法人数据越权。
> 3.  **Controller**: 生成 `ArchiveController`，包含 `upload`, `search` (支持动态 JSONB 查询), `preview` 接口（支持流式或预签名返回）。

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
> 你是前端专家。请使用 React 19 + Ant Design 6 生成“档案检索与预览”页面。
> 1.  **搜索栏**: 包含“法人选择（下拉）”、“档案类型”、“关键字”、“金额区间”。
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
>     *   检查所有 `archiveIds` 是否属于同一个 `entity_id` 和 `archive_year`。
>     *   更新这些档案的 `physical_location` 字段。
>     *   生成一条 `AuditLog`，action="PHYSICAL_BOXING"。
> 3.  生成一个工具类 `LabelPrinter`，输入 `BoxInfo`，调用 PDF 库（如 iText）生成 10cm x 3cm 的标签 PDF，包含二维码。

---

## 8. 验收测试用例 (QA Checklist)

1.  **数据隔离测试**：
    *   登录法人 A 的账号，直接通过 API 修改参数请求法人 B 的 `entity_id` 数据 -> **预期**：返回 403 Forbidden 或 空数据（被拦截器过滤）。
2.  **三员互斥测试**：
    *   使用 `sysadmin` 账号登录，尝试访问“档案检索”页面 -> **预期**：提示“权限不足”或菜单不可见。
3.  **水印测试**：
    *   在浏览器 F12 开发者工具中，手动删除水印 DOM 节点 -> **预期**：节点立即被 JS 重新插入，或屏幕变白并在后台记录一次“篡改尝试”。
4.  **实物借阅审批测试**：
    *   发起实物借阅申请 -> 审批通过 -> 出库登记 -> 归还登记 -> **预期**：`borrow_record` 状态正确，逾期提醒可用。
5.  **库房盘点测试**：
    *   生成盘点清单并扫描核对 -> **预期**：异常项可追溯并产生审计日志。
6.  **销毁测试**：
    *   执行销毁流程 -> **预期**：文件无法下载，但数据库中仍能查到该档案的元数据，且 `destruction_log` 表中有一条快照记录（永久保留）。
