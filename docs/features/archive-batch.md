# 归档批次管理功能文档

> 版本: 1.0.0
> 更新日期: 2025-12-26
> 模块: 档案作业

---

## 一、功能概述

归档批次管理是电子会计档案系统的核心功能，负责将预归档库中的凭证和单据批量归档到正式档案库。

### 核心价值

| 价值点 | 说明 |
|-------|------|
| 批量处理 | 按期间批量归档，提高效率 |
| 流程管控 | 提交 → 校验 → 审批 → 归档，全程可控 |
| 四性检测 | 真实性、完整性、可用性、安全性检测 |
| 期间锁定 | 归档后自动锁定期间，防止篡改 |
| 审计追溯 | 全程记录操作人和时间 |

---

## 二、业务流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                        归档批次工作流                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐      │
│  │  创建批次  │ → │  添加条目  │ → │  提交校验  │ → │  等待审批  │      │
│  │ (PENDING) │    │          │    │(VALIDATING)│    │          │      │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘      │
│                                                        │            │
│                                         ┌──────────────┴─────┐      │
│                                         ↓                    ↓      │
│                                  ┌──────────┐         ┌──────────┐  │
│                                  │  审批通过  │         │  审批驳回  │  │
│                                  │ (APPROVED) │         │ (REJECTED) │  │
│                                  └──────────┘         └──────────┘  │
│                                         │                           │
│                                         ↓                           │
│                                  ┌──────────┐                       │
│                                  │  执行归档  │                       │
│                                  │ (ARCHIVED) │                       │
│                                  └──────────┘                       │
│                                         │                           │
│                                         ↓                           │
│                                  ┌──────────┐                       │
│                                  │  期间锁定  │                       │
│                                  └──────────┘                       │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 状态说明

| 状态 | 英文 | 说明 | 可执行操作 |
|------|------|------|-----------|
| 待提交 | PENDING | 新建批次，可添加/移除条目 | 添加条目、删除批次、提交校验 |
| 校验中 | VALIDATING | 已提交，等待审批 | 审批通过、驳回 |
| 已审批 | APPROVED | 审批通过，等待执行归档 | 执行归档 |
| 已归档 | ARCHIVED | 归档完成，数据已固化 | 仅查看 |
| 已驳回 | REJECTED | 审批未通过 | 仅查看 |
| 失败 | FAILED | 校验或归档失败 | 仅查看 |

---

## 三、操作指南

### 3.1 创建归档批次

1. 进入 **档案作业 → 归档审批**
2. 点击 **创建批次** 按钮
3. 选择归档期间（起始日期 ~ 结束日期）
4. 点击确定

**注意事项**：
- 同一期间范围不能有多个进行中的批次
- 已归档的期间不能重复创建批次

### 3.2 添加条目到批次

创建批次后，需要将凭证和单据添加到批次中：

```typescript
// API 调用示例
await archiveBatchApi.addVouchers(batchId, [voucherId1, voucherId2]);
await archiveBatchApi.addDocs(batchId, [docId1, docId2]);
```

**当前版本说明**：条目添加功能需要从凭证列表页面操作，或通过 API 调用。

### 3.3 提交校验

1. 确认批次中已添加所需条目
2. 点击 **提交校验** 按钮
3. 系统自动执行校验，检查：
   - 凭证是否存在
   - 单据是否存在
   - 必要字段是否完整

### 3.4 审批操作

审批人员可以：

**通过**：
1. 点击 **通过** 按钮
2. 填写审批意见（可选）
3. 确认

**驳回**：
1. 点击 **驳回** 按钮
2. 填写驳回原因（必填）
3. 确认

### 3.5 执行归档

审批通过后：

1. 点击 **执行归档** 按钮
2. 系统自动执行：
   - 四性检测
   - 更新所有条目状态为已归档
   - 锁定相关期间
   - 生成归档记录

**警告**：归档后数据不可修改，请确认无误后再执行。

### 3.6 四性检测

可以在归档前或归档后执行四性检测：

1. 打开批次详情
2. 切换到"四性检测"标签页
3. 点击 **执行四性检测**

检测内容：

| 检测类型 | 说明 | 检测内容 |
|---------|------|---------|
| 真实性 | AUTHENTICITY | 验证来源可靠性、操作人员 |
| 完整性 | INTEGRITY | 验证数据完整、未被篡改 |
| 可用性 | USABILITY | 验证文件可正常读取 |
| 安全性 | SECURITY | 验证符合安全管理要求 |

---

## 四、API 接口

### 4.1 批次管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/archive-batch` | 创建批次 |
| GET | `/api/archive-batch/{id}` | 获取批次详情 |
| GET | `/api/archive-batch` | 分页查询批次 |
| DELETE | `/api/archive-batch/{id}` | 删除批次 |

### 4.2 条目管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/archive-batch/{id}/vouchers` | 添加凭证 |
| POST | `/api/archive-batch/{id}/docs` | 添加单据 |
| DELETE | `/api/archive-batch/{id}/items/{itemId}` | 移除条目 |
| GET | `/api/archive-batch/{id}/items` | 获取条目列表 |

### 4.3 流程操作

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/archive-batch/{id}/submit` | 提交校验 |
| POST | `/api/archive-batch/{id}/validate` | 执行校验 |
| POST | `/api/archive-batch/{id}/approve` | 审批通过 |
| POST | `/api/archive-batch/{id}/reject` | 审批驳回 |
| POST | `/api/archive-batch/{id}/archive` | 执行归档 |

### 4.4 四性检测

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/archive-batch/{id}/integrity-check` | 执行四性检测 |

### 4.5 统计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/archive-batch/stats` | 获取批次统计 |

---

## 五、数据结构

### 5.1 归档批次 (ArchiveBatch)

```json
{
  "id": 1,
  "batchNo": "AB-2024-0001",
  "fondsId": 1,
  "periodStart": "2024-01-01",
  "periodEnd": "2024-01-31",
  "scopeType": "PERIOD",
  "status": "PENDING",
  "voucherCount": 100,
  "docCount": 50,
  "fileCount": 200,
  "totalSizeBytes": 104857600,
  "validationReport": {},
  "integrityReport": {},
  "submittedBy": 1,
  "submittedAt": "2024-01-15T10:00:00",
  "approvedBy": 2,
  "approvedAt": "2024-01-15T14:00:00",
  "archivedAt": "2024-01-15T15:00:00",
  "createdAt": "2024-01-15T09:00:00"
}
```

### 5.2 批次条目 (ArchiveBatchItem)

```json
{
  "id": 1,
  "batchId": 1,
  "itemType": "VOUCHER",
  "refId": 1001,
  "refNo": "记-2024-001",
  "status": "VALIDATED",
  "hashSm3": "abc123..."
}
```

### 5.3 四性检测报告 (IntegrityReport)

```json
{
  "batchId": 1,
  "batchNo": "AB-2024-0001",
  "checkedAt": "2024-01-15T15:00:00",
  "checks": [
    { "checkType": "AUTHENTICITY", "name": "真实性检测", "result": "PASS" },
    { "checkType": "INTEGRITY", "name": "完整性检测", "result": "PASS" },
    { "checkType": "USABILITY", "name": "可用性检测", "result": "PASS" },
    { "checkType": "SECURITY", "name": "安全性检测", "result": "PASS" }
  ],
  "overallResult": "PASS"
}
```

---

## 六、数据库表结构

### 6.1 archive_batch

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| batch_no | VARCHAR(32) | 批次编号 |
| fonds_id | BIGINT | 全宗 ID |
| period_start | DATE | 期间起始 |
| period_end | DATE | 期间结束 |
| scope_type | VARCHAR(20) | 范围类型 |
| status | VARCHAR(20) | 状态 |
| voucher_count | INT | 凭证数 |
| doc_count | INT | 单据数 |
| validation_report | JSONB | 校验报告 |
| integrity_report | JSONB | 四性检测报告 |
| submitted_by | BIGINT | 提交人 |
| approved_by | BIGINT | 审批人 |
| archived_by | BIGINT | 归档执行人 |

### 6.2 archive_batch_item

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| batch_id | BIGINT | 批次 ID |
| item_type | VARCHAR(32) | 条目类型 |
| ref_id | BIGINT | 引用 ID |
| ref_no | VARCHAR(64) | 引用编号 |
| status | VARCHAR(20) | 状态 |
| hash_sm3 | VARCHAR(64) | SM3 哈希 |

### 6.3 period_lock

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| fonds_id | BIGINT | 全宗 ID |
| period | VARCHAR(7) | 期间 (YYYY-MM) |
| lock_type | VARCHAR(20) | 锁定类型 |
| locked_at | TIMESTAMP | 锁定时间 |
| locked_by | BIGINT | 锁定人 |

### 6.4 integrity_check

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| target_type | VARCHAR(32) | 目标类型 |
| target_id | BIGINT | 目标 ID |
| check_type | VARCHAR(32) | 检测类型 |
| result | VARCHAR(20) | 检测结果 |
| hash_expected | VARCHAR(64) | 期望哈希 |
| hash_actual | VARCHAR(64) | 实际哈希 |
| details | JSONB | 检测详情 |

---

## 七、权限要求

| 操作 | 所需权限 |
|------|---------|
| 查看批次列表 | archive:batch:view |
| 创建批次 | archive:batch:create |
| 提交校验 | archive:batch:submit |
| 审批批次 | archive:batch:approve |
| 执行归档 | archive:batch:archive |
| 删除批次 | archive:batch:delete |

---

## 八、常见问题

### Q1: 为什么不能创建批次？

**可能原因**：
1. 该期间已有进行中的批次
2. 该期间已被归档锁定
3. 无创建权限

### Q2: 为什么审批通过按钮是灰色的？

**原因**：存在校验失败的条目，需先处理失败条目。

### Q3: 归档后能否修改？

**不能**。归档后数据进入不可变状态，只能通过"归档更正"流程添加更正记录。

### Q4: 如何查看四性检测详情？

打开批次详情 → 切换到"四性检测"标签页。

---

## 九、版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2025-12-26 | 初始版本，包含完整归档批次管理功能 |
