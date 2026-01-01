# 修复用户全宗权限 - 确保现有数据可见

> **修复日期**: 2025-01  
> **问题类型**: P0级数据可见性问题修复  
> **相关变更**: 多组织数据隔离改造

---

## 🎯 问题描述

在多组织改造后，系统改为基于 `fonds_no`（全宗号）进行数据隔离。导致以下问题：

1. **用户看不到数据**：如果用户的 `sys_user_fonds_scope` 表中没有对应全宗号的权限记录，查询时会返回空结果
2. **现有数据不可见**：现有数据的 `fonds_no`（如 'DEMO', 'COMP001'）与用户的 `org_code` 可能不匹配，导致用户无法看到这些数据
3. **V79 迁移脚本的局限性**：V79 脚本只基于用户的 `org_code` 初始化权限，但如果用户没有 `org_code` 或 `org_code` 与数据的 `fonds_no` 不匹配，仍然看不到数据

---

## 📋 修复方案

### 修复脚本

**文件**: `nexusarchive-java/src/main/resources/db/migration/V84__fix_user_fonds_scope_for_existing_data.sql`

### 修复策略

脚本采用两个步骤确保所有用户都能访问现有数据：

1. **为所有用户添加现有数据全宗号的权限**：
   - 收集所有现有数据的唯一 `fonds_no`
   - 使用 `CROSS JOIN` 为每个用户添加这些全宗号的权限
   - 使用 `NOT EXISTS` 避免重复插入

2. **为有 org_code 的用户添加其 org_code 对应的全宗权限**：
   - 确保用户至少可以访问与其组织代码匹配的全宗数据
   - 这作为补充，确保用户未来创建的数据也能被自己看到

### 关键代码逻辑

```sql
-- 步骤1：为所有用户添加现有数据全宗号的权限
INSERT INTO sys_user_fonds_scope (id, user_id, fonds_no, scope_type, ...)
SELECT 
    CONCAT(u.id, '-', fonds.fonds_no) as id,
    u.id as user_id,
    fonds.fonds_no,
    'MIGRATION' as scope_type,  -- 标记为迁移来源
    ...
FROM (
    -- 获取所有现有数据的唯一全宗号
    SELECT DISTINCT fonds_no 
    FROM acc_archive 
    WHERE fonds_no IS NOT NULL 
      AND fonds_no <> '' 
      AND deleted = 0
) fonds
CROSS JOIN sys_user u
WHERE u.deleted = 0
  AND NOT EXISTS (
      SELECT 1 
      FROM sys_user_fonds_scope s 
      WHERE s.user_id = u.id 
        AND s.fonds_no = fonds.fonds_no
        AND s.deleted = 0
  );
```

---

## ✅ 修复验证

### 执行前检查

```sql
-- 检查现有数据的全宗号分布
SELECT fonds_no, COUNT(*) as archive_count 
FROM acc_archive 
WHERE deleted = 0 
GROUP BY fonds_no 
ORDER BY fonds_no;

-- 检查用户的权限分布
SELECT user_id, COUNT(*) as permission_count 
FROM sys_user_fonds_scope 
WHERE deleted = 0 
GROUP BY user_id;
```

### 执行后验证

```sql
-- 验证权限记录数量
SELECT COUNT(DISTINCT user_id) as user_count, COUNT(*) as total_permissions 
FROM sys_user_fonds_scope 
WHERE deleted = 0;

-- 验证每个全宗号的用户权限分布
SELECT fonds_no, COUNT(DISTINCT user_id) as user_count 
FROM sys_user_fonds_scope 
WHERE deleted = 0 
GROUP BY fonds_no 
ORDER BY fonds_no;

-- 验证所有现有数据的全宗号都有对应的用户权限
SELECT DISTINCT a.fonds_no
FROM acc_archive a
WHERE a.deleted = 0
  AND a.fonds_no IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 
      FROM sys_user_fonds_scope s 
      WHERE s.fonds_no = a.fonds_no 
        AND s.deleted = 0
  );
```

如果最后一个查询返回空结果，说明所有现有数据的全宗号都有对应的用户权限。

### 功能验证

1. **登录系统**：使用现有用户账号登录
2. **访问全景视图**：进入 `/system/panorama` 页面
3. **检查数据可见性**：应该能看到所有现有数据（'DEMO' 和 'COMP001' 全宗的数据）
4. **检查数据隔离**：不同用户应该能看到相同的数据（因为都添加了相同的全宗权限）

---

## 🔗 相关文件

- **迁移脚本**: `nexusarchive-java/src/main/resources/db/migration/V84__fix_user_fonds_scope_for_existing_data.sql`
- **V79 迁移脚本**: `nexusarchive-java/src/main/resources/db/migration/V79__fonds_audit_borrow_alignment.sql`
- **数据隔离服务**: `nexusarchive-java/src/main/java/com/nexusarchive/service/DataScopeService.java`
- **数据隔离修复报告**: `docs/reports/datascope-fonds-isolation-fix.md`

---

## ⚠️ 注意事项

### 数据权限范围

此修复脚本为**所有用户**添加了**所有现有数据全宗号**的权限。这意味着：

1. **所有用户都能看到现有数据**：这是修复数据可见性问题所必需的
2. **未来数据隔离**：未来创建的新数据，如果使用新的全宗号，用户默认无法看到，除非明确授权
3. **权限管理**：建议后续通过系统管理界面为不同用户配置适当的全宗权限范围

### 性能考虑

- 如果用户数量和数据全宗号数量都很大，`CROSS JOIN` 可能会产生大量权限记录
- 建议在生产环境执行前，先评估用户数量和全宗号数量
- 确保 `sys_user_fonds_scope` 表有适当的索引（V79 已创建）

### 回滚方案

如果需要回滚此修复，可以执行：

```sql
-- 删除标记为 'MIGRATION' 的权限记录
DELETE FROM sys_user_fonds_scope 
WHERE scope_type = 'MIGRATION';
```

但注意：回滚后，用户将无法看到现有数据，除非有其他方式配置权限。

---

## 📊 修复状态

- [x] 创建修复脚本 V84
- [x] 脚本语法验证通过
- [ ] 在测试环境执行验证
- [ ] 在生产环境执行
- [ ] 功能验证通过

---

**修复完成时间**: 2025-01  
**修复人员**: AI Assistant  
**审核状态**: 待测试验证

