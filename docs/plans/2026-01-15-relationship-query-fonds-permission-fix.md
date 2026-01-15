# 穿透联查 - 全宗权限问题修复

**日期**: 2026-01-15  
**问题**: 权限错误提示"您没有权限查看此档案的关系数据"  
**原因**: 测试数据在 `BR-GROUP` 全宗，用户当前全宗不匹配  
**状态**: ✅ 已修复（添加明确的权限检查和错误提示）

---

## 🔍 问题分析

### 问题现象

用户在穿透联查页面输入档号（如 `FP-2025-01-001`）时，提示：
```
您没有权限查看此档案的关系数据
```

### 根本原因

1. **测试数据位置**：
   - 所有 demo 数据（V102 迁移脚本）都在 `BR-GROUP` 全宗下
   - 档号示例：`FP-2025-01-001`、`JZ-2025-01-001`、`BX-2025-01-001` 等

2. **权限校验机制**：
   - `RelationController.getRelationGraph()` 调用 `archiveService.getArchivesByIds()`
   - `getArchivesByIds()` 通过 `DataScopeService` 应用全宗权限过滤
   - 如果用户当前全宗不是 `BR-GROUP`，或没有权限，查询结果为空

3. **错误提示不明确**：
   - 前端只显示通用错误："您没有权限查看此档案的关系数据"
   - 后端没有明确的权限检查，只是返回空结果

---

## ✅ 修复方案

### 后端修复

**文件**: `RelationController.java`

**修改内容**：

1. **添加权限检查**：
   ```java
   // 检查中心档案是否在权限过滤后的结果中
   boolean centerArchiveFound = relatedArchives.stream()
           .anyMatch(a -> a.getId().equals(centerArchiveId));
   
   if (!centerArchiveFound) {
       log.warn("[RelationController] Center archive {} not found after permission filter. Center fonds: {}, Current fonds: {}", 
           centerArchiveId, center.getFondsNo(), currentFonds);
       return Result.error(403, String.format("您没有权限查看全宗 [%s] 的档案关系数据。当前全宗: [%s]。请切换到正确的全宗后重试。", 
           center.getFondsNo(), currentFonds));
   }
   ```

2. **增强日志输出**：
   ```java
   log.debug("[RelationController] getArchivesByIds returned {} archives (after permission filter), requested: {}, currentFonds: {}", 
       relatedArchives.size(), relatedIds.size(), currentFonds);
   ```

**效果**：
- ✅ 返回明确的错误信息，包含档案所在全宗和当前全宗
- ✅ 提示用户切换到正确的全宗
- ✅ 便于调试和排查问题

---

## 📋 测试数据全宗信息

### V102 Demo 数据

所有测试数据都在 `BR-GROUP` 全宗下：

| 档号前缀 | 示例 | 说明 |
|---------|------|------|
| `SQ-` | `SQ-2025-01-001` | 出差申请单 |
| `FP-` | `FP-2025-01-001` | 发票 |
| `BX-` | `BX-2025-01-001` | 报销单 |
| `FK-` | `FK-2025-01-001` | 付款单 |
| `HD-` | `HD-2025-01-001` | 银行回单 |
| `JZ-` | `JZ-2025-01-001` | 记账凭证 |
| `BB-` | `BB-2025-01` | 月度报表 |
| `HT-` | `HT-2025-02-001` | 采购合同 |

**全宗代码**: `BR-GROUP`  
**全宗名称**: 泊冉集团有限公司

---

## 🚀 使用指南

### 前置条件

1. **用户必须有 `BR-GROUP` 全宗权限**
   - 如果用户没有权限，联系系统管理员分配
   - 权限分配路径：系统管理 → 用户管理 → 分配全宗权限

2. **切换到正确的全宗**
   - 在页面顶部使用全宗切换器
   - 选择 `BR-GROUP` 全宗
   - 如果没有看到 `BR-GROUP`，说明用户没有权限

### 测试步骤

1. **确认全宗**：
   - 查看页面顶部全宗切换器显示的当前全宗
   - 确保是 `BR-GROUP`

2. **输入档号查询**：
   - 输入：`FP-2025-01-001`（发票）
   - 或输入：`JZ-2025-01-001`（记账凭证）
   - 点击"查询"

3. **验证结果**：
   - 如果权限正确：正常显示关系图谱
   - 如果权限错误：显示明确的错误提示，包含全宗信息

---

## 🔧 故障排查

### 问题1：提示"您没有权限查看此档案的关系数据"

**检查清单**：
1. ✅ 当前选择的全宗是否是 `BR-GROUP`？
   - 查看页面顶部全宗切换器
2. ✅ 用户是否有 `BR-GROUP` 全宗权限？
   - 检查用户权限配置
3. ✅ 档号是否正确？
   - 确认档号存在于数据库中
   - 确认档号的 `fonds_no` 字段是 `BR-GROUP`

**解决方案**：
- 切换到 `BR-GROUP` 全宗
- 或联系管理员分配 `BR-GROUP` 全宗权限
- 或使用属于当前全宗的档号进行测试

### 问题2：错误提示不够明确

**已修复**：
- ✅ 后端返回详细的错误信息，包含全宗代码
- ✅ 前端显示完整的错误消息

---

## 📊 相关文件

| 文件 | 修改内容 |
|------|---------|
| `RelationController.java` | 添加权限检查和明确错误提示 |
| `V102__seed_relationship_demo_data.sql` | Demo 数据（全宗：`BR-GROUP`） |

---

## ✅ 验证清单

- [x] 后端添加权限检查逻辑
- [x] 返回明确的错误提示（包含全宗信息）
- [x] 增强日志输出
- [x] 更新文档说明测试数据所在全宗
- [ ] 手动测试：切换到 `BR-GROUP` 全宗后查询
- [ ] 手动测试：使用错误全宗时验证错误提示

---

**修复状态**: ✅ 已完成  
**待测试**: 需要手动验证权限检查和错误提示是否正常工作
