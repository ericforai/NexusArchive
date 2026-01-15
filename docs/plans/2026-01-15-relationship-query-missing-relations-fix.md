# 穿透联查 - 缺失关系数据问题修复

**日期**: 2026-01-15  
**问题**: 真实凭证数据 `BR-GROUP-2025-30Y-FIN-AC01-1001` 显示没有上下游，但实际有发票文件  
**原因**: 发票文件未转换为 `acc_archive` 记录，或未建立 `acc_archive_relation` 关系  
**状态**: 🔧 修复中

---

## 🔍 问题分析

### 问题现象

查询真实凭证 `BR-GROUP-2025-30Y-FIN-AC01-1001` (ID: `voucher-2024-11-001`) 时：
- ❌ 显示"没有上下游数据"
- ✅ 但在凭证详情页面可以看到"电子发票_吴奕聪餐饮店_657元.pdf"文件

### 根本原因

1. **关系数据缺失**：
   - `acc_archive_relation` 表中没有该凭证的关系记录
   - `arc_voucher_relation` 表中也没有原始凭证关联记录

2. **发票文件存储方式**：
   - 发票可能仅作为文件存储在 `arc_original_voucher_file` 表中
   - 未转换为 `acc_archive` 记录
   - 或已转换为 `acc_archive` 记录，但未建立关系

3. **查询逻辑局限**：
   - 当前查询逻辑只查询 `acc_archive_relation` 表
   - 未包含原始凭证文件关联

---

## ✅ 修复方案

### 方案1：补充关系数据（推荐）

如果发票已转换为 `acc_archive` 记录，但缺少关系数据，需要手动创建关系：

```sql
-- 查找发票档案记录（根据金额、日期匹配）
-- 然后创建关系记录
INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted)
SELECT 
    'rel-' || gen_random_uuid()::text,
    invoice.id,
    'voucher-2024-11-001',
    'ORIGINAL_VOUCHER',
    '原始凭证',
    'system',
    NOW(),
    0
FROM acc_archive invoice
WHERE invoice.fonds_no = 'BR-GROUP'
  AND invoice.amount = 657.00
  AND invoice.doc_date = '2025-10-25'
  AND invoice.category_code IN ('AC01', 'AC04')
  AND NOT EXISTS (
    SELECT 1 FROM acc_archive_relation ar
    WHERE ar.source_id = invoice.id
      AND ar.target_id = 'voucher-2024-11-001'
      AND ar.deleted = 0
  );
```

### 方案2：扩展查询逻辑（已实现）

修改 `RelationController.getRelationGraph()` 方法，包含原始凭证关联：

1. ✅ 查询 `arc_voucher_relation` 表获取原始凭证关联
2. ✅ 通过 `source_doc_id` 查找对应的 `acc_archive` 记录
3. ✅ 将原始凭证关联转换为 `ArchiveRelation` 添加到关系列表

**已实现代码**：
- 添加 `VoucherRelationMapper` 和 `OriginalVoucherMapper` 依赖
- 在查询关系时，同时查询 `arc_voucher_relation` 表
- 通过 `source_doc_id` 查找对应的档案记录
- 创建虚拟的 `ArchiveRelation` 添加到关系列表

---

## 🔧 实施步骤

### Step 1: 查找发票档案记录

```sql
-- 查找可能的发票档案记录
SELECT a.id, a.archive_code, a.title, a.amount, a.doc_date, a.category_code
FROM acc_archive a
WHERE a.fonds_no = 'BR-GROUP'
  AND a.amount = 657.00
  AND a.doc_date = '2025-10-25'
  AND a.category_code IN ('AC01', 'AC04');
```

### Step 2: 创建关系记录

如果找到发票档案记录，创建关系：

```sql
INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted)
VALUES 
('rel-fix-1001-001', '{发票档案ID}', 'voucher-2024-11-001', 'ORIGINAL_VOUCHER', '原始凭证-电子发票', 'system', NOW(), 0);
```

### Step 3: 验证

查询关系数据，确认已创建：

```sql
SELECT ar.*, a1.archive_code as source_code, a2.archive_code as target_code
FROM acc_archive_relation ar
LEFT JOIN acc_archive a1 ON ar.source_id = a1.id
LEFT JOIN acc_archive a2 ON ar.target_id = a2.id
WHERE ar.target_id = 'voucher-2024-11-001' AND ar.deleted = 0;
```

---

## 📋 检查清单

- [ ] 查找发票档案记录（通过金额、日期匹配）
- [ ] 如果找到，创建 `acc_archive_relation` 关系记录
- [ ] 如果未找到，检查发票是否仅作为文件存在（`arc_original_voucher_file`）
- [ ] 如果发票仅作为文件，考虑是否需要转换为 `acc_archive` 记录
- [ ] 验证修复后的查询结果

---

**修复状态**: 🔧 代码已更新，待查找发票档案记录并创建关系数据
