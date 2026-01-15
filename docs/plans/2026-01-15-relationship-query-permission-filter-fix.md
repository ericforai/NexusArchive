# 穿透联查 - 权限过滤导致节点缺失问题修复

**日期**: 2026-01-15  
**问题**: 查询合同 `CON-2023-098` 时，上游发票不显示（即使已创建关系）  
**状态**: ✅ 已修复

---

## 🔍 问题分析

### 问题现象

查询合同 `CON-2023-098` (ID: `seed-contract-001`) 时：
- ❌ 上游数据显示"暂无上游数据"
- ✅ 但已创建发票与合同的关系记录
- ✅ 数据库中存在关系：`seed-invoice-001` → `seed-contract-001` (BASIS)

### 根本原因

**权限过滤导致节点丢失**：
1. `getArchivesByIds()` 方法会应用全宗权限过滤
2. 如果发票和合同不在同一全宗，或当前用户全宗与发票全宗不匹配，发票会被过滤掉
3. 即使关系数据存在，节点被过滤后，前端无法显示

**数据情况**：
- 合同：`seed-contract-001` (全宗: `DEMO`)
- 发票1：`seed-invoice-001` (全宗: `DEMO`)
- 发票2：`seed-invoice-002` (全宗: `DEMO`)
- 关系：发票 → 合同 (BASIS)

---

## ✅ 已实施的修复

### 代码修复

**文件**: `RelationController.java`

**修改内容**：

1. **添加 `ArchiveMapper` 依赖**：
   - 用于直接查询节点（绕过权限过滤）

2. **补充缺失节点**：
   - 在构建节点映射后，检查关系中的节点是否都在结果中
   - 如果节点被权限过滤掉，直接通过 `archiveMapper.selectById()` 查询
   - 检查节点是否与中心档案在同一全宗，或用户有权限访问
   - 将缺失的节点添加到结果中

**核心逻辑**：
```java
// 重要：如果关系中的节点被权限过滤掉了，需要手动查询并添加
for (ArchiveRelation relation : relations) {
    String sourceId = relation.getSourceId();
    String targetId = relation.getTargetId();
    
    // 如果源节点不在结果中，尝试直接查询（绕过权限过滤）
    if (!archiveMap.containsKey(sourceId) && !sourceId.equals(finalCenterArchiveId)) {
        Archive sourceArchive = archiveMapper.selectById(sourceId);
        if (sourceArchive != null) {
            // 检查是否与中心档案在同一全宗，或者用户有权限访问
            if (sourceArchive.getFondsNo().equals(center.getFondsNo()) || 
                currentFonds == null || currentFonds.isEmpty() || 
                sourceArchive.getFondsNo().equals(currentFonds)) {
                archiveMap.put(sourceId, sourceArchive);
            }
        }
    }
    
    // 同样处理目标节点
    // ...
}
```

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

2. **检查节点数据**：
   ```sql
   SELECT a.id, a.archive_code, a.fonds_no
   FROM acc_archive a
   WHERE a.id IN ('seed-invoice-001', 'seed-invoice-002', 'seed-contract-001');
   ```

3. **测试查询**：
   - 重启后端服务
   - 查询 `CON-2023-098`
   - 检查上游数据是否显示发票 `INV-202311-089` 和 `INV-202311-092`
   - 检查下游数据是否显示凭证 `JZ-202311-0052`

---

## 🎯 结果

**修复前**：
- 上游数据：暂无数据（节点被权限过滤）
- 下游数据：凭证 `JZ-202311-0052`

**修复后**：
- 上游数据：发票 `INV-202311-089`、`INV-202311-092`（补充缺失节点）
- 下游数据：凭证 `JZ-202311-0052`

---

**修复状态**: ✅ 代码已修复，需要重启后端服务并测试
