# 电子会计档案系统 - 后续开发 OpenSpec (Sprint 4+)

> **版本**: v1.0  
> **日期**: 2025-01  
> **对齐基准**: [PRD v1.0](prd-v1.0.md)  
> **当前状态**: Sprint 3 已完成（核心架构、四性检测、长期保存、预览接口）

---

## 📊 对齐状态概览

### ✅ 已完成 (Sprint 0-3)

| 模块 | PRD 章节 | 完成度 | 备注 |
|------|---------|--------|------|
| 核心架构 | Section 1 | 100% | 全宗隔离、数据模型、隔离兜底 |
| 四性检测 | Section 3.1 | 100% | 真实性、完整性、可用性、安全性 |
| 长期保存 | Section 3.2 | 100% | AIP 导出、XML 封装、SHA-256 Manifest |
| 预览接口 | Section 5.1 | 100% | 流式预览、动态水印 |
| 检索与脱敏 | Section 2.1 | 100% | 高级检索、数据脱敏 |

### ⏸️ 暂缓/Out of Scope

| 模块 | PRD 章节 | 状态 | 原因 |
|------|---------|------|------|
| 实物档案管理 | Section 1.2-1.5 | 已移出范围 | 专家组审查建议，聚焦电子合规 |

### 🔨 待开发 (Sprint 4+)

| 模块 | PRD 章节 | 优先级 | 当前状态 |
|------|---------|--------|----------|
| 档案销毁流程 | Section 4.1 | **P2** | 基础状态位已实现，缺少完整工作流 |
| 跨全宗授权票据 | Section 2.4 | **P1** | 数据库表已就绪，业务逻辑未实现 |
| 全宗沿革管理 | Section 1.1 | **P1** | 数据库表已就绪，业务逻辑未实现 |
| 冻结/保全机制 | Section 7.2 | **P1** | 部分字段存在，完整流程未实现 |
| 审计证据链验真 | Section 6.2 | **P1** | 哈希链已实现，验真接口未提供 |

---

## 🎯 Sprint 4 目标：档案销毁流程完整实现

### 4.1 业务目标

**用户故事**: 作为档案管理员，当档案保管期限到期后，我需要发起销毁申请，经过审批后执行逻辑销毁，系统保留销毁清册用于审计追溯。

### 4.2 功能范围

#### 4.2.1 到期档案自动识别 (P0)

**PRD 来源**: Section 4.1 - 状态机 `Normal -> Expired`

**实现要求**:
- 定时任务（每日凌晨执行）扫描 `archive_object` 表
- **保管期限起算规则**: 使用 `retention_start_date`（保管期限起算日期）而非 `doc_date` 计算到期
  - 若 `retention_start_date` 为空，则使用 `archived_at`（归档时间）或会计年度结束日期
  - 计算方式：`retention_start_date + retention_policy.years <= 当前日期`
- 自动将到期档案状态更新为 `EXPIRED`
- 生成到期提醒通知（可选）

**技术规格**:

```java
@Service
public class ArchiveExpirationService {
    /**
     * 扫描到期档案并更新状态（支持分布式锁）
     * @return 本次扫描发现的到期档案数量
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    @DistributedLock(key = "archive:expiration:scan", timeout = 3600) // 分布式锁，防止多实例重复执行
    public int scanAndMarkExpired();
    
    /**
     * 分页扫描到期档案（每批1000条）
     */
    private List<ArchiveObject> scanExpiredArchives(int page, int size);
    
    /**
     * 检查单个档案是否到期
     */
    boolean isExpired(ArchiveObject archive, RetentionPolicy policy);
}
```

**数据库变更**:
- 新增字段：`archive_object.retention_start_date DATE`（保管期限起算日期）
- 建议添加索引：`(retention_policy_id, retention_start_date, destruction_status)`
- 支持断点续扫：记录最后扫描的 `id`，支持增量扫描

#### 4.2.2 鉴定清单生成 (P0)

**PRD 来源**: Section 4.1 - 状态机 `Expired -> Appraising`  
**法规依据**: 《会计档案管理办法》要求销毁前需进行鉴定

**实现要求**:
- 档案管理员可批量选择 `EXPIRED` 状态的档案
- 系统生成鉴定清单（包含档案元数据快照）
- **鉴定清单必备字段**（符合法规要求）:
  - 档案基本信息：档案编号、标题、形成时间、归档时间
  - 保管期限信息：保管期限名称、起算日期、到期日期
  - **鉴定信息**：鉴定人、鉴定日期、鉴定结论（同意销毁/不同意销毁/延期保管）
  - 鉴定意见（可选）
- 清单支持导出为 Excel/PDF
- **鉴定清单作为销毁审批的前置条件**，生成后状态流转为 `APPRAISING`

**技术规格**:

```java
public interface ArchiveAppraisalService {
    /**
     * 生成鉴定清单
     * @param archiveIds 待鉴定档案ID列表
     * @param fondsNo 全宗号（从登录态获取）
     * @param appraiserId 鉴定人ID
     * @param appraisalDate 鉴定日期
     * @return 鉴定清单ID
     */
    String createAppraisalList(List<String> archiveIds, String fondsNo, 
                               String appraiserId, LocalDate appraisalDate);
    
    /**
     * 提交鉴定结论
     * @param appraisalListId 鉴定清单ID
     * @param conclusion 鉴定结论：APPROVED（同意销毁）/ REJECTED（不同意销毁）/ DEFERRED（延期保管）
     * @param comment 鉴定意见
     */
    void submitAppraisalConclusion(String appraisalListId, String conclusion, String comment);
    
    /**
     * 导出鉴定清单
     */
    byte[] exportAppraisalList(String appraisalListId, ExportFormat format);
}
```

**数据模型**:
- 复用 `biz_destruction` 表（或新增 `appraisal_list` 表）
- 清单快照存储在 `archive_ids` (JSON) 字段
- 新增字段：`appraiser_id`（鉴定人ID）、`appraisal_date`（鉴定日期）、`appraisal_conclusion`（鉴定结论）、`appraisal_comment`（鉴定意见）

#### 4.2.3 销毁审批流程 (P0)

**PRD 来源**: Section 4.1 - 状态机 `Appraising -> DESTRUCTION_APPROVED`

**实现要求**:
- 支持双人审批（初审 + 复核）
- 审批链记录在 `approval_snapshot` (JSON) 字段
- 审批通过后状态流转为 `DESTRUCTION_APPROVED`
- 审批拒绝可回退到 `APPRAISING` 或 `EXPIRED`

**技术规格**:

```java
public interface DestructionApprovalService {
    /**
     * 初审审批
     */
    void firstApproval(String destructionId, String approverId, String comment, boolean approved);
    
    /**
     * 复核审批（双人复核）
     */
    void secondApproval(String destructionId, String approverId, String comment, boolean approved);
    
    /**
     * 获取审批链快照
     */
    ApprovalChain getApprovalChain(String destructionId);
}
```

**审批链 JSON 格式**:
```json
{
  "firstApproval": {
    "approverId": "user-001",
    "approverName": "张三",
    "comment": "同意销毁",
    "approved": true,
    "timestamp": "2025-01-15T10:30:00"
  },
  "secondApproval": {
    "approverId": "user-002",
    "approverName": "李四",
    "comment": "复核通过",
    "approved": true,
    "timestamp": "2025-01-15T14:20:00"
  }
}
```

#### 4.2.4 在借校验 (P0)

**PRD 来源**: Section 4.1 - 核心逻辑 "在借校验"

**实现要求**:
- 执行销毁前，必须检查所有档案是否存在 `borrow_record.status = 'BORROWED'`
- 若存在在借记录，禁止销毁，返回错误信息
- 仅允许 `RETURNED` 或无借阅记录的档案进入销毁

**技术规格**:

```java
public interface DestructionValidationService {
    /**
     * 校验档案是否可销毁
     * @throws DestructionNotAllowedException 若存在在借记录或冻结状态
     */
    void validateDestructionEligibility(List<String> archiveIds, String fondsNo);
    
    /**
     * 检查档案是否在借
     */
    boolean isBorrowed(String archiveObjectId, String fondsNo, Integer archiveYear);
}
```

#### 4.2.5 逻辑销毁执行 (P0)

**PRD 来源**: Section 4.1 - 核心逻辑 "逻辑销毁"

**实现要求**:
- **权限要求**: 销毁执行需 `Archivist`（档案管理员）角色 + 审批通过状态（`DESTRUCTION_APPROVED`）
- 更新 `archive_object.destruction_status = 'DESTROYED'`
- **清空文件引用**：删除对象存储中的物理文件（或标记为已删除）
- **保留元数据**：核心元数据（`id`, `fonds_no`, `archive_year`, `title`, `doc_date`, `amount` 等）与 `metadata_ext` 必须保留
- 生成销毁清册记录（见 4.2.6）

**技术规格**:

```java
@Service
@Transactional(rollbackFor = Exception.class)
public class DestructionExecutionService {
    /**
     * 执行逻辑销毁
     * @param destructionId 销毁申请ID
     * @param executorId 执行人ID（需校验 Archivist 角色）
     */
    void executeDestruction(String destructionId, String executorId);
    
    /**
     * 销毁单个档案（事务边界：清册写入 → 元数据更新 → 物理文件删除）
     */
    private void destroyArchive(ArchiveObject archive, String executorId, String traceId);
    
    /**
     * 删除物理文件（对象存储）
     * 默认模式：软删除（标记删除），物理文件保留在隔离存储区
     * 硬删除模式：需额外审批，删除前必须完成备份验证
     */
    private void deletePhysicalFile(String fileId, DestructionMode mode);
    
    /**
     * 记录文件删除审计日志
     */
    private void logFileDeletion(String fileId, String filePath, long fileSize, 
                                 String fileHash, DestructionMode mode);
}

public enum DestructionMode {
    SOFT_DELETE,  // 软删除（默认）：标记删除，文件保留
    HARD_DELETE   // 硬删除：物理删除，需额外审批+备份验证
}
```

**关键实现点**:
- **事务边界**: 清册写入 → 元数据更新 → 物理文件删除（或标记删除）应在同一事务中
- **回滚策略**: 若物理文件删除失败，应回滚清册写入，避免数据不一致
- **默认模式**: **默认使用软删除**（标记删除），物理文件保留在隔离存储区
- **硬删除要求**: 硬删除需额外审批，且删除前必须完成备份验证
- **审计日志**: 删除操作需记录审计日志，包含文件路径、大小、哈希值、删除模式
- **权限校验**: 执行操作需校验 `Archivist` 角色，记录执行人、执行时间、TraceID

#### 4.2.6 销毁清册记录 (P0)

**PRD 来源**: Section 4.1 - 核心逻辑 "销毁清册" + Section 6.1

**实现要求**:
- 销毁执行时，将每个档案的**完整元数据快照**写入 `destruction_log` 表
- 清册记录**永久只读**，禁止修改/删除
- 支持哈希链（`prev_hash`/`curr_hash`）用于验真
- 清册支持导出为 Excel/PDF

**技术规格**:

```java
@Service
public class DestructionLogService {
    /**
     * 写入销毁清册记录
     */
    void logDestruction(ArchiveObject archive, String destructionId, 
                       String executorId, String traceId);
    
    /**
     * 导出销毁清册
     */
    byte[] exportDestructionLog(String fondsNo, Integer archiveYear, 
                                LocalDate fromDate, LocalDate toDate);
    
    /**
     * 计算清册哈希链
     */
    String calculateHashChain(String prevHash, DestructionLog log);
}
```

**数据库表结构** (已定义在 PRD 4.1，需优化):
```sql
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
    snapshot TEXT NOT NULL,  -- JSON 格式的完整元数据快照
    prev_hash VARCHAR(128),
    curr_hash VARCHAR(128),
    sig TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, fonds_no, archive_year),
    -- 唯一性约束：防止同一档案被重复记录销毁
    UNIQUE (archive_object_id, fonds_no, archive_year)
);

-- 按年度分区（可选，大数据量时启用）
-- CREATE TABLE destruction_log_2025 PARTITION OF destruction_log FOR VALUES FROM (2025) TO (2026);

-- 索引优化
CREATE INDEX idx_destruction_log_fonds_year ON destruction_log(fonds_no, archive_year, destroyed_at);
CREATE INDEX idx_destruction_log_destroyed_at ON destruction_log(destroyed_at);

-- 数据库触发器：禁止 UPDATE/DELETE 操作（不可篡改性保障）
CREATE OR REPLACE FUNCTION prevent_destruction_log_modification()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' OR TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'destruction_log table is read-only. Modification is not allowed.';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER destruction_log_readonly_trigger
    BEFORE UPDATE OR DELETE ON destruction_log
    FOR EACH ROW
    EXECUTE FUNCTION prevent_destruction_log_modification();
```

**快照 JSON 格式**（完整版，包含档案管理维度的时间戳）:
```json
{
  "archiveObjectId": "arch-001",
  "fondsNo": "JD",
  "archiveYear": 2020,
  "title": "2020年度财务报表",
  "docDate": "2020-12-31",
  "amount": 1000000.00,
  "counterparty": "XX公司",
  "retentionPolicy": {
    "id": "ret-001",
    "name": "30年",
    "years": 30
  },
  "metadataExt": {...},
  "snapshotTime": "2025-01-15T16:00:00",
  "archivedAt": "2021-03-15T10:30:00",
  "retentionStartDate": "2021-01-01",
  "originalFormationDate": "2020-12-31",
  "expiredDate": "2051-01-01"
}
```

**存储容量规划**:
- 按年度分区存储（如 `destruction_log_2025`），支持大数据量场景
- 定期归档历史清册到对象存储（不可变桶）
- 查询性能优化：按全宗/年度索引

#### 4.2.7 冻结/保全状态处理 (P0)

**PRD 来源**: Section 4.1 - 状态机 `FROZEN`/`HOLD`

**实现要求**:
- 档案可进入 `FROZEN`（审计/诉讼冻结）或 `HOLD`（保全）状态
- 冻结/保全期间**禁止销毁**
- 解除冻结/保全需审批
- 状态流转记录在审计日志

**技术规格**:

```java
public interface ArchiveFreezeService {
    /**
     * 冻结档案
     */
    void freezeArchive(String archiveId, String reason, String operatorId, LocalDate expireDate);
    
    /**
     * 解除冻结
     */
    void unfreezeArchive(String archiveId, String reason, String operatorId);
    
    /**
     * 检查档案是否被冻结
     */
    boolean isFrozen(String archiveId);
}
```

**数据库字段** (已存在):
- `archive_object.destruction_status` 支持 `FROZEN`/`HOLD` 值
- 或使用 `destruction_hold` (boolean) + `hold_reason` (TEXT) 字段

### 4.3 API 设计

#### 4.3.1 到期扫描接口

```
POST /api/archive/expiration/scan
响应: { "expiredCount": 15, "message": "扫描完成" }
```

#### 4.3.2 鉴定清单接口

```
POST /api/archive/appraisal/create
请求: { "archiveIds": ["arch-001", "arch-002"], "fondsNo": "JD" }
响应: { "appraisalListId": "appr-001", "archiveCount": 2 }

GET /api/archive/appraisal/{id}/export?format=excel
响应: Excel 文件流
```

#### 4.3.3 销毁审批接口

```
POST /api/destruction/{id}/first-approval
请求: { "approverId": "user-001", "comment": "同意", "approved": true }

POST /api/destruction/{id}/second-approval
请求: { "approverId": "user-002", "comment": "复核通过", "approved": true }
```

#### 4.3.4 销毁执行接口

```
POST /api/destruction/{id}/execute
请求: { 
  "executorId": "user-003",
  "mode": "SOFT_DELETE"  // 可选：SOFT_DELETE（默认）或 HARD_DELETE（需额外审批）
}
响应: { 
  "destroyedCount": 10, 
  "traceId": "trace-xxx",
  "mode": "SOFT_DELETE"
}

注意：执行人需具备 Archivist 角色，且销毁申请状态必须为 DESTRUCTION_APPROVED
```

#### 4.3.5 销毁清册查询/导出

```
GET /api/destruction/log?fondsNo=JD&archiveYear=2020&fromDate=2025-01-01&toDate=2025-01-31
响应: 分页列表

GET /api/destruction/log/export?fondsNo=JD&archiveYear=2020
响应: Excel/PDF 文件流
```

### 4.4 前端页面

#### 4.4.1 到期档案列表页

- 显示所有 `EXPIRED` 状态的档案
- 支持批量选择
- 操作按钮：生成鉴定清单

#### 4.4.2 鉴定清单页

- 显示待鉴定档案列表
- 操作按钮：提交审批、导出清单

#### 4.4.3 销毁审批页

- 显示待审批的销毁申请
- 审批表单：审批意见、同意/拒绝
- 显示审批链历史

#### 4.4.4 销毁清册页

- 查询条件：全宗、年度、日期范围
- 列表展示：档案信息、销毁时间、执行人
- 操作按钮：导出清册、查看详情

### 4.5 验收标准

1. **到期识别测试**: 定时任务正确识别到期档案并更新状态（基于 `retention_start_date`）
2. **分布式锁测试**: 多实例部署时，定时任务仅单实例执行
3. **鉴定清单测试**: 鉴定清单包含必备字段（鉴定人、鉴定日期、鉴定结论）
4. **在借校验测试**: 在借档案无法进入销毁流程
5. **冻结校验测试**: 冻结/保全档案无法销毁
6. **权限校验测试**: 非 Archivist 角色无法执行销毁
7. **逻辑销毁测试**: 销毁后文件无法下载，但元数据可查询
8. **清册完整性测试**: 销毁清册包含完整元数据快照（含时间戳字段）
9. **哈希链测试**: 清册哈希链可验真，篡改后能检测
10. **不可篡改测试**: 数据库触发器禁止 UPDATE/DELETE 操作
11. **唯一性约束测试**: 同一档案无法重复记录销毁
12. **事务一致性测试**: 清册写入失败时，物理文件删除应回滚
13. **软删除测试**: 默认软删除模式下，物理文件保留在隔离存储区

---

## 🎯 Sprint 5 目标：跨全宗授权与全宗沿革

### 5.1 跨全宗授权票据 (Auth Ticket)

**PRD 来源**: Section 2.4

**功能范围**:
- 授权票据申请（申请人、源全宗、目标全宗、范围、有效期）
- 审批流程（双审批/复核）
- 票据状态管理（active/revoked/expired）
- 跨全宗访问时校验票据有效性
- 审计日志绑定票据ID

**技术规格**:

```java
public interface AuthTicketService {
    /**
     * 申请跨全宗授权票据
     */
    AuthTicket createTicket(AuthTicketRequest request);
    
    /**
     * 审批票据
     */
    void approveTicket(String ticketId, String approverId, boolean approved);
    
    /**
     * 校验票据有效性
     */
    boolean validateTicket(String ticketId, String sourceFonds, String targetFonds);
    
    /**
     * 撤销票据
     */
    void revokeTicket(String ticketId, String operatorId);
}
```

**数据库表** (已定义在 PRD 4.1):
```sql
CREATE TABLE auth_ticket (
    id VARCHAR(32) PRIMARY KEY,
    applicant_id VARCHAR(32) NOT NULL,
    source_fonds VARCHAR(50) NOT NULL,
    target_fonds VARCHAR(50) NOT NULL,
    scope TEXT NOT NULL,  -- JSON: { "archiveYears": [2020, 2021], "docTypes": ["AC01"], "keywords": [...] }
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,  -- PENDING, ACTIVE, REVOKED, EXPIRED
    approval_snapshot TEXT,  -- JSON 审批链
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 5.2 全宗沿革管理

**PRD 来源**: Section 1.1 - "全宗沿革可追溯"

**功能范围**:
- 全宗迁移（MIGRATE）
- 全宗合并（MERGE）
- 全宗分立（SPLIT）
- 全宗重命名（RENAME）
- 沿革历史查询与追溯

**技术规格**:

```java
public interface FondsHistoryService {
    /**
     * 记录全宗迁移
     */
    void recordMigration(String fondsNo, String fromFondsNo, String reason, String approvalTicketId);
    
    /**
     * 记录全宗合并
     */
    void recordMerge(String targetFondsNo, List<String> sourceFondsNos, String reason, String approvalTicketId);
    
    /**
     * 记录全宗分立
     */
    void recordSplit(String sourceFondsNo, List<String> newFondsNos, String reason, String approvalTicketId);
    
    /**
     * 查询全宗沿革历史
     */
    List<FondsHistory> getHistory(String fondsNo);
}
```

**数据库表** (已定义在 PRD 4.1):
```sql
CREATE TABLE sys_fonds_history (
    id VARCHAR(32) PRIMARY KEY,
    fonds_no VARCHAR(50) NOT NULL,
    event_type VARCHAR(20) NOT NULL,  -- MERGE, SPLIT, RENAME, MIGRATE
    from_fonds_no VARCHAR(50),
    to_fonds_no VARCHAR(50),
    effective_date DATE NOT NULL,
    reason TEXT,
    approval_ticket_id VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (fonds_no) REFERENCES sys_fonds(fonds_no)
);
```

---

## 🎯 Sprint 6+ 目标：审计增强与合规完善

### 6.1 审计证据链验真

**PRD 来源**: Section 6.2

**功能范围**:
- 提供哈希链验真接口
- 支持抽检与批量验真
- 证据包导出（包含审计日志、销毁清册、审批链）

### 6.2 备份恢复审计

**PRD 来源**: Section 2.3

**功能范围**:
- 备份操作绑定审批票据与 TraceID
- 恢复操作全链路留痕
- 证据包导出

### 6.3 销毁清册数据迁移策略

**实现要求**:
- 销毁清册为"只读迁移"，迁移后需验证哈希链完整性
- 迁移操作需记录审计日志，包含迁移时间、迁移人、源系统、目标系统
- 迁移后禁止修改清册记录，保持不可篡改性

**技术规格**:

```java
public interface DestructionLogMigrationService {
    /**
     * 迁移销毁清册（只读迁移）
     */
    void migrateDestructionLog(String sourceSystem, String targetSystem, 
                              String operatorId, LocalDate fromDate, LocalDate toDate);
    
    /**
     * 验证迁移后哈希链完整性
     */
    boolean verifyHashChainIntegrity(String fondsNo, Integer archiveYear);
}
```

---

## 📅 开发计划

### Sprint 4 (2周)

| 任务 | 优先级 | 预计工时 | 负责人 | 备注 |
|------|--------|----------|--------|------|
| 到期档案自动识别 | P0 | 1.5d | Backend | 含分布式锁、分页扫描、retention_start_date 字段 |
| 鉴定清单生成 | P0 | 2.5d | Backend + Frontend | 含鉴定人、鉴定日期、鉴定结论字段 |
| 销毁审批流程 | P0 | 2d | Backend + Frontend | 双人审批 |
| 在借校验 | P0 | 1d | Backend | 销毁前校验 |
| 逻辑销毁执行 | P0 | 2.5d | Backend | 含权限校验、事务一致性、软删除/硬删除模式 |
| 销毁清册记录 | P0 | 2.5d | Backend | 含唯一性约束、数据库触发器、快照完整性 |
| 冻结/保全处理 | P0 | 1d | Backend | 禁止销毁校验 |
| 前端页面开发 | P0 | 3d | Frontend | 到期列表、鉴定清单、审批、清册查询 |
| 集成测试 | P0 | 2d | QA | 含不可篡改测试、事务一致性测试 |

### Sprint 5 (2周)

| 任务 | 优先级 | 预计工时 |
|------|--------|----------|
| 跨全宗授权票据 | P1 | 3d |
| 全宗沿革管理 | P1 | 2d |
| 前端页面 | P1 | 2d |
| 集成测试 | P1 | 1d |

### Sprint 6+ (待定)

| 任务 | 优先级 | 预计工时 |
|------|--------|----------|
| 审计证据链验真 | P1 | 2d |
| 备份恢复审计 | P1 | 2d |

---

## ⚠️ 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 物理文件删除不可逆 | 高 | **默认使用软删除**（标记删除），硬删除需额外审批+备份验证 |
| 销毁清册数据量大 | 中 | **按年度分区存储**，定期归档历史清册到对象存储 |
| 销毁清册被篡改 | 高 | **数据库触发器禁止 UPDATE/DELETE**，定期导出到不可变存储 |
| 定时任务重复执行 | 中 | **分布式锁**（Redis/数据库），防止多实例重复执行 |
| 事务一致性风险 | 中 | **明确事务边界**，清册写入失败时回滚物理文件删除 |
| 跨全宗授权复杂度高 | 中 | 先实现基础功能，复杂场景后续迭代 |
| 全宗沿革数据迁移 | 高 | 先实现记录功能，数据迁移工具后续开发 |

---

## 📚 参考资料

- [PRD v1.0](prd-v1.0.md) - 产品需求文档
- [Sprint 2 Revised Spec](../planning/sprint-2-revised-spec.md) - Sprint 2 技术规格
- [Development Roadmap](../planning/development_roadmap_v1.0.md) - 开发路线图

---

## 📝 变更记录

| 版本 | 日期 | 修改人 | 备注 |
|------|------|--------|------|
| v1.1 | 2025-01 | PM | 根据专家审查报告优化：补充鉴定清单法规依据、销毁清册完整性、不可篡改性保障、物理文件删除策略等 |
| v1.0 | 2025-01 | PM | 初始版本，基于 PRD v1.0 与 Sprint 3 完成情况 |

---

## 📋 专家审查优化说明

本文档已根据 [专家审查报告](openspec-sprint4-plus-expert-review.md) 进行优化，主要补充内容：

1. **合规层面**:
   - 补充鉴定清单的法规依据与必备字段（鉴定人、鉴定日期、鉴定结论）
   - 完善销毁清册快照内容（补充时间戳字段：归档时间、保管期限起算日期、原始形成时间）
   - 明确保管期限起算规则（`retention_start_date` 字段）

2. **架构层面**:
   - 增加销毁清册表的唯一性约束（防止重复记录）
   - 增强销毁清册的不可篡改性保障（数据库触发器）
   - 优化物理文件删除策略（默认软删除，硬删除需额外审批）
   - 完善定时任务调度（分布式锁、分页扫描）

3. **交付层面**:
   - 明确销毁执行权限（Archivist 角色）
   - 规划销毁清册存储容量（按年度分区、定期归档）
   - 完善事务一致性保障（明确事务边界与回滚策略）

