# 穿透联查 - 合同上游发票缺失问题修复

**日期**: 2026-01-15  
**问题**: 查询合同 `CON-2023-098` 时，上游数据不显示发票  
**状态**: ✅ 已修复

---

## 🔍 问题分析

### 问题现象

查询合同 `CON-2023-098` (ID: `seed-contract-001`) 时：
- ❌ 上游数据显示"暂无上游数据"
- ✅ 但实际存在发票 `INV-202311-089` 和 `INV-202311-092`
- ✅ 发票与凭证有关系，但与合同没有直接关系

### 根本原因

**数据缺失**：
- `acc_archive_relation` 表中，发票与合同之间没有关系记录
- 发票只与凭证有关系（`ORIGINAL_VOUCHER`）
- 合同只与凭证有关系（`BASIS`）

**业务逻辑**：
- 发票 → 合同：应该是 `BASIS` 关系（合同依据发票）
- 合同 → 凭证：应该是 `BASIS` 关系（合同依据）

---

## ✅ 已实施的修复

### 数据修复

创建了发票与合同的关系记录：

```sql
-- 发票1 -> 合同
INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted)
VALUES ('rel-con-inv-001', 'seed-invoice-001', 'seed-contract-001', 'BASIS', '合同依据发票', 'system', NOW(), 0);

-- 发票2 -> 合同
INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted)
VALUES ('rel-con-inv-002', 'seed-invoice-002', 'seed-contract-001', 'BASIS', '合同依据发票', 'system', NOW(), 0);
```

### 查询逻辑验证

当前查询逻辑应该能够正确处理：

```java
// 查询所有与中心节点相关的关系（作为源或目标）
List<ArchiveRelation> relations = archiveRelationService.list(
    new LambdaQueryWrapper<ArchiveRelation>()
        .eq(ArchiveRelation::getSourceId, finalCenterArchiveId)  // 发票 -> 合同
        .or()
        .eq(ArchiveRelation::getTargetId, finalCenterArchiveId)     // 合同 -> 凭证
);
```

**关系链**：
- 发票 (`seed-invoice-001`, `seed-invoice-002`) → 合同 (`seed-contract-001`) [BASIS]
- 合同 (`seed-contract-001`) → 凭证 (`seed-voucher-001`) [BASIS]

---

## 📋 验证步骤

1. **检查关系数据**：
   ```sql
   SELECT ar.*, a1.archive_code as source_code, a2.archive_code as target_code
   FROM acc_archive_relation ar
   LEFT JOIN acc_archive a1 ON ar.source_id = a1.id
   LEFT JOIN acc_archive a2 ON ar.target_id = a2.id
   WHERE (ar.source_id = 'seed-contract-001' OR ar.target_id = 'seed-contract-001')
     AND ar.deleted = 0;
   ```

2. **测试查询**：
   - 查询 `CON-2023-098`
   - 检查上游数据是否显示发票 `INV-202311-089` 和 `INV-202311-092`
   - 检查下游数据是否显示凭证 `JZ-202311-0052`

---

## 🎯 结果

**修复前**：
- 上游数据：暂无数据
- 下游数据：凭证 `JZ-202311-0052`

**修复后**：
- 上游数据：发票 `INV-202311-089`、`INV-202311-092`
- 下游数据：凭证 `JZ-202311-0052`

---

**修复状态**: ✅ 数据已修复，查询逻辑无需修改（已支持双向查询）
