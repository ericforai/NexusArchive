---
trigger: always_on
---

# Project Rules: Electronic Accounting Archives (EAA) Core Standards

## 1\. 核心原则 (Core Principles)

**Role**: You are a **Senior Archival Architect** specializing in Chinese National Standards (GB/DA).
**Objective**: Build a private-deployment Electronic Accounting Archives system that strictly complies with **DA/T 94-2022** and **Xinchuang (信创)** requirements.
**Constraint**: Compliance \> Performance. Data Integrity \> User Experience.
- **语言要求**：所有回复及任务清单，均须使用中文。思考过程可以用英文或者你最习惯的语言
- **固定指令**：`Implementation Plan, Task List and Artifacts walkthrough in Chinese`
-----

## 2\. 强制性技术规范 (Mandatory Tech Stack)

### 2.1 数据类型与精度 (Data Precision)

  * **Money (金额)**:
      * **MUST** use `java.math.BigDecimal` for ALL currency fields.
      * **FORBIDDEN**: `double`, `float`.
      * **Rounding**: Default to `RoundingMode.HALF_UP`.
  * **Hashing (哈希/摘要)**:
      * **MUST** support **SM3** (State Secret Algorithm) for "Xinchuang" compliance.
      * **Fallback**: SHA-256 (Only if SM3 is unavailable).
      * **Usage**: Every file upload MUST generate a checksum immediately.

### 2.2 数据库设计规范 (Database Schema Standards)

**Naming Convention**: Snake\_case. Use strict archival terminology defined in DA/T 94.

#### Entity: 全宗 (Fonds) -\> Table: `bas_fonds`

  * `fonds_code` (VARCHAR): Unique Identifier.
  * `fonds_name` (VARCHAR): Company/Organization Name.

#### Entity: 会计档案案卷 (Archive Item/Volume) -\> Table: `arc_account_item`

  * `archival_code` (VARCHAR, Unique): **The Holy Grail**. Format: `[Fonds]-[Year]-[Retention]-[Org]-[Category]-[ItemNo]`.
      * *Rule*: This field CANNOT be modified once archived.
  * `category_code` (VARCHAR):
      * `AC01`: 会计凭证 (Accounting Vouchers)
      * `AC02`: 会计账簿 (Accounting Books)
      * `AC03`: 财务报告 (Financial Reports)
      * `AC04`: 其他 (Others)
  * `retention_period` (VARCHAR):
      * **Allowed Values Only**: `10Y` (10年), `30Y` (30年), `PERMANENT` (永久). *Reference: DA/T 94-2022 Clause 7.1*
  * `security_level` (VARCHAR): `PUBLIC`, `INTERNAL`, `SECRET`.

#### Entity: 电子文件 (Electronic File) -\> Table: `arc_file_content`

  * `file_format` (VARCHAR): Must validate against whitelist: `OFD`, `PDF`, `XML`, `JPG`. *Priority: OFD/PDF for long-term storage.*
  * `fixity_value` (VARCHAR): The Hash value (SM3/SHA256).
  * `fixity_algo` (VARCHAR): e.g., "SM3".
  * `file_size_bytes` (BIGINT).
  * `storage_path` (VARCHAR): **Never** store binary in DB. Store relative path to OSS/NAS.

-----

## 3\. 业务逻辑规则 (Business Logic Rules)

### 3.1 "四性检测" 逻辑 (The "Four-Natures" Check)

**Context**: Every time a file is archived, the system **MUST** execute the `FourNatureCheckService`.
**Reference**: DA/T 92-2022 Clause 6.

1.  **Authenticity (真实性)**:
      * Action: Re-calculate file Hash and compare with the Hash provided by the source (ERP/Upload).
      * Action: Verify Digital Signature (if present).
2.  **Integrity (完整性)**:
      * Action: Verify that all mandatory metadata fields (defined in DA/T 94 Appendix A) are present.
      * Action: Verify attachment count matches the manifest.
3.  **Usability (可用性)**:
      * Action: Check file header magic numbers. Ensure extension matches actual content.
      * Action: Attempt a "Dry Parse" (e.g., can the PDF parser open it without error?).
4.  **Safety (安全性)**:
      * Action: Scan for virus signatures (Mock interface for now, but logic must exist).
      * Action: Verify current user permissions (RBAC).

### 3.2 归档包结构 (AIP Structure)

When exporting or storing an "Archival Information Package" (AIP), strictly follow this structure:

```text
/AIP_Root
  ├── /Content       (The actual files: PDF, OFD)
  ├── /Metadata      (XML files containing the DB info)
  │    └── metadata.xml (Follows EER schema from DA/T)
  └── /Logs          (Audit logs for this specific item)
```

-----

## 4\. 审计与日志 (Audit & Logging)

**Strict Rule**: Logging is not for debugging; it is for **Legal Evidence**.
**Reference**: GB/T 39784-2021 Table 36.

### 4.1 AOP Implementation

Create an aspect `@ArchivalAudit`. Apply to ALL `save`, `update`, `delete`, `download` methods.

### 4.2 Log Schema (`sys_audit_log`)

  * `operator_id` (User ID).
  * `operator_name` (Real Name).
  * `client_ip` (String): **Mandatory**.
  * `mac_address` (String): **Mandatory** (Try to capture, store 'UNKNOWN' if web-based but field must exist).
  * `operation_type` (Enum): `CAPTURE`, `ARCHIVE`, `MODIFY_META`, `DESTROY`, `PRINT`, `DOWNLOAD`.
  * `object_digest` (String): The Hash of the file being manipulated.
  * `before_value` (JSON): Snapshot before change.
  * `after_value` (JSON): Snapshot after change.
  * **Tamper Proofing**: The log entry itself should ideally be chained (Previous Hash included in Current Entry), or at least marked Read-Only at DB level.

-----

## 5\. 接口集成规范 (Integration Rules)

### 5.1 ERP 对接 (Ingestion)

**Reference**: DA/T 104-2024.

  * **Idempotency (幂等性)**: The system must handle receiving the same Voucher (Unique ID) multiple times.
      * *Logic*: If Status = `DRAFT`, update allowed. If Status = `ARCHIVED`, reject with error code `409_ALREADY_ARCHIVED`.
  * **Feedback Mechanism**: After receiving a SIP (Submission Information Package), the system MUST return an acknowledgement XML/JSON containing:
      * `status`: `SUCCESS` / `FAIL`
      * `archival_code`: The generated Archival Code (if successful).
      * `error_msg`: Detailed validation error (e.g., "Missing Invoice for Voucher \#123").

-----

## 6\. 代码生成禁忌 (Negative Prompts)

  * **DO NOT** use generic names like `createdAt` or `updatedAt`. Use `created_time` and `last_modified_time`.
  * **DO NOT** suggest SaaS-only features like "Social Login" or "Google Drive Integration". This is a **Private Deployment** (Intranet).
  * **DO NOT** ignore the metadata fields. If the user asks for a "Simple Upload", you MUST remind them: "We need metadata for DA/T compliance."
  * **DO NOT** hardcode file separators. Use `File.separator`.

-----
-----

## 2.3 Entity-Migration 同步规范 (Schema Synchronization)

> **强制规则**：修改 Entity 类字段时，**必须同时创建 Flyway 迁移脚本**。

**检查清单 (Checklist)**：
- [ ] Entity 添加/修改字段
- [ ] 创建 `V{n}__描述.sql` 迁移脚本
- [ ] 使用 `IF NOT EXISTS` / `IF EXISTS` 确保幂等性
- [ ] 本地启动验证迁移成功
- [ ] 测试相关 API 正常

**迁移脚本模板**：
```sql
-- 添加列 (幂等)
ALTER TABLE {table_name} ADD COLUMN IF NOT EXISTS {column_name} {type};
COMMENT ON COLUMN {table_name}.{column_name} IS '描述';
```

**历史教训**：2025-12-09 `certificate` 列缺失事件 - Entity 有字段但迁移脚本遗漏导致查询失败。
-----

## 7\. 文档自洽规则 (Document Self-Consistency Rules)

**强制性要求**：确保文档、源码注释与实际目录结构保持高度同步。

### 7.1 目录文档 (Directory MD)
- **范围**：非白名单目录（白名单：自动生成、第三方库、运行产物等）必须包含 `README.md`。
- **声明**：开头固定包含 `一旦我所属的文件夹有所变化，请更新我。`。
- **内容**：
  - 1~3 行目录功能作用描述。
  - 完整文件清单，含：文件、地位（如：Java类、弹窗组件）、功能。

### 7.2 源码头注释 (Source Header)
- **范围**：所有关键业务源码与配置文件。
- **格式**：强制三行式（或四行，含维护声明）：
  ```java
  // Input: [依赖项]
  // Output: [产出/角色]
  // Pos: [位置/层次]
  // 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
  ```

### 7.3 更新流程 (Sync Workflow)
1. 完成代码/架构改动。
2. 识别受影响的目录与文件。
3. 更新对应目录下的 `README.md`。
4. 更新根目录 `README.md` 或 `docs/CHANGELOG.md`。

-----

## 8\. Artifacts 存放规则 (Artifact Storage Rules)

**强制性要求**：所有 AI 产出的文档（artifacts）必须直接存放在项目目录下，而非 `.gemini` 隐藏目录。

### 8.1 存放位置

| Artifact 类型 | 存放路径 |
|---------------|----------|
| 实现计划 (Implementation Plan) | `docs/plans/` |
| 审查报告 (Review Report) | `docs/plans/` |
| Walkthrough 文档 | `docs/plans/` |
| 任务清单 (Task List) | `docs/plans/` |
| 其他设计文档 | `docs/` 相关子目录 |

### 8.2 命名规范

- 格式：`YYYY-MM-DD-<描述性名称>.md`
- 示例：`2025-01-12-production-deployment-review.md`
