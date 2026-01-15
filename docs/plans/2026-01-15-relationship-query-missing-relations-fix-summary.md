# 穿透联查 - 缺失关系数据问题修复总结

**日期**: 2026-01-15  
**问题**: 真实凭证 `BR-GROUP-2025-30Y-FIN-AC01-1001` 显示没有上下游，但实际有发票文件  
**状态**: ✅ 代码已修复，待数据验证

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

---

## ✅ 已实施的修复

### 代码修复

**文件**: `RelationController.java`

**修改内容**：

1. **添加依赖**：
   - `VoucherRelationMapper` - 查询原始凭证关联
   - `OriginalVoucherMapper` - 查询原始凭证信息

2. **扩展查询逻辑**：
   - 在查询 `acc_archive_relation` 后，同时查询 `arc_voucher_relation` 表
   - 通过 `source_doc_id` 查找对应的 `acc_archive` 记录
   - 将原始凭证关联转换为 `ArchiveRelation` 添加到关系列表

**核心逻辑**：
```java
// Step 2.2: 查询 arc_voucher_relation 表中的原始凭证关联
List<VoucherRelation> voucherRelations = voucherRelationMapper.findByAccountingVoucherId(finalCenterArchiveId);

// 将原始凭证关联转换为 ArchiveRelation
for (VoucherRelation vr : voucherRelations) {
    OriginalVoucher originalVoucher = originalVoucherMapper.selectById(vr.getOriginalVoucherId());
    
    // 方法1: 通过 source_doc_id 查找档案记录
    if (originalVoucher.getSourceDocId() != null) {
        Archive sourceArchive = archiveService.getArchiveById(originalVoucher.getSourceDocId());
        if (sourceArchive != null) {
            // 创建关系记录
            ArchiveRelation ar = new ArchiveRelation();
            ar.setSourceId(originalVoucher.getSourceDocId());
            ar.setTargetId(finalCenterArchiveId);
            ar.setRelationType("ORIGINAL_VOUCHER");
            relations.add(ar);
        }
    }
}
```

---

## 🔧 数据修复（待执行）

### 如果发票已转换为档案记录

需要手动创建关系记录：

```sql
-- 1. 查找发票档案记录（根据金额、日期、标题匹配）
SELECT a.id, a.archive_code, a.title
FROM acc_archive a
WHERE a.fonds_no = 'BR-GROUP'
  AND a.amount = 657.00
  AND a.doc_date = '2025-10-25'
  AND a.title LIKE '%发票%'
  AND a.id != 'voucher-2024-11-001';

-- 2. 如果找到发票档案，创建关系记录
INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted)
VALUES 
('rel-fix-1001-001', '{发票档案ID}', 'voucher-2024-11-001', 'ORIGINAL_VOUCHER', '原始凭证-电子发票', 'system', NOW(), 0);
```

### 如果发票未转换为档案记录

需要：
1. 将发票文件转换为 `acc_archive` 记录
2. 创建关系记录

---

## 📋 验证步骤

1. **检查原始凭证关联**：
   ```sql
   SELECT vr.*, ov.voucher_no, ov.voucher_type
   FROM arc_voucher_relation vr
   LEFT JOIN arc_original_voucher ov ON vr.original_voucher_id = ov.id
   WHERE vr.accounting_voucher_id = 'voucher-2024-11-001';
   ```

2. **检查发票档案记录**：
   ```sql
   SELECT a.id, a.archive_code, a.title
   FROM acc_archive a
   WHERE a.fonds_no = 'BR-GROUP'
     AND a.amount = 657.00
     AND a.doc_date = '2025-10-25';
   ```

3. **测试查询**：
   - 重启后端服务
   - 查询 `BR-GROUP-2025-30Y-FIN-AC01-1001`
   - 检查是否显示发票作为上游数据

---

## 🎯 下一步

1. **查找发票档案记录**：通过金额、日期、标题匹配
2. **创建关系记录**：如果找到发票档案，创建 `acc_archive_relation` 记录
3. **验证修复**：重新查询凭证，确认发票显示在上游数据中

---

**修复状态**: ✅ 代码已修复，待数据验证和关系创建
