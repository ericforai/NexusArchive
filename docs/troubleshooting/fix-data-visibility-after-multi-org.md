# 修复多组织改造后数据不可见问题

> **问题**: 加了多组织以后，之前的数据都没了（显示"暂无数据"）
> **原因**: 数据隔离改为基于 `fonds_no`（全宗号），用户缺少对应的全宗权限记录

---

## 🔍 快速诊断

### 方法1: 使用诊断脚本（推荐）

```bash
# 执行诊断脚本，查看当前状态
./scripts/diagnose_fonds_scope.sh
```

### 方法2: 手动SQL查询

连接到数据库后执行：

```sql
-- 1. 检查现有数据的全宗号
SELECT DISTINCT fonds_no 
FROM acc_archive 
WHERE deleted = 0 
  AND fonds_no IS NOT NULL 
  AND fonds_no <> '';

-- 2. 检查用户权限
SELECT u.username, COUNT(s.fonds_no) as permission_count
FROM sys_user u
LEFT JOIN sys_user_fonds_scope s ON u.id = s.user_id AND s.deleted = 0
WHERE u.deleted = 0
GROUP BY u.id, u.username;

-- 3. 检查是否有数据的全宗号没有权限
SELECT DISTINCT a.fonds_no
FROM acc_archive a
WHERE a.deleted = 0
  AND a.fonds_no IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_fonds_scope s 
      WHERE s.fonds_no = a.fonds_no AND s.deleted = 0
  );
```

---

## 🔧 修复步骤

### 步骤1: 执行修复SQL

**方式A: 手动执行SQL脚本（推荐，立即生效）**

```bash
# 连接到数据库
psql -h localhost -p 54321 -U nexusarchive -d nexusarchive

# 执行修复脚本
\i scripts/fix_fonds_scope_manual.sql
```

或者直接在数据库中执行脚本内容。

**方式B: 等待Flyway自动执行（需要重启应用）**

修复脚本 `V84__fix_user_fonds_scope_for_existing_data.sql` 会在应用重启时自动执行。

```bash
# 重启后端服务
cd nexusarchive-java
mvn spring-boot:run

# 或如果使用Docker
docker-compose restart backend
```

### 步骤2: 重新登录（重要！）

⚠️ **关键步骤**: 用户的权限信息是在登录时加载的，所以必须：

1. **退出当前登录**
2. **重新登录系统**

这样系统才会重新加载用户的 `allowedFonds` 权限列表。

### 步骤3: 验证修复

1. 登录系统
2. 访问 `/system/panorama`（全景视图）
3. 应该能看到档案目录中的数据

---

## 📋 修复原理

### 问题根源

系统改为基于 `fonds_no`（全宗号）进行数据隔离：

```java
// DataScopeService.applyArchiveScope()
Set<String> allowedFonds = context.allowedFonds();
if (!allowedFonds.isEmpty()) {
    wrapper.in("fonds_no", allowedFonds);
} else {
    wrapper.eq("1", "0"); // 如果没有权限，查询不到任何数据
}
```

如果用户没有对应的全宗权限记录，`allowedFonds` 为空，导致查询返回空结果。

### 修复方案

修复脚本会：

1. **为所有用户添加现有数据全宗号的权限**：
   - 收集所有现有数据的唯一 `fonds_no`（如 'DEMO', 'COMP001'）
   - 为每个用户添加这些全宗号的权限记录

2. **为有 org_code 的用户添加组织代码对应的全宗权限**：
   - 确保用户能访问与其组织代码匹配的全宗数据

---

## ⚠️ 常见问题

### Q1: 执行SQL后还是看不到数据？

**A**: 请确保：
1. ✅ SQL执行成功（检查是否有错误信息）
2. ✅ 已经重新登录（退出并重新登录）
3. ✅ 检查浏览器缓存（尝试硬刷新 Ctrl+Shift+R 或 Cmd+Shift+R）

### Q2: 如何验证SQL是否执行成功？

```sql
-- 检查权限记录数量（应该有增加）
SELECT COUNT(*) FROM sys_user_fonds_scope WHERE deleted = 0;

-- 检查每个用户的权限
SELECT u.username, s.fonds_no 
FROM sys_user u
JOIN sys_user_fonds_scope s ON u.id = s.user_id
WHERE s.deleted = 0
ORDER BY u.username, s.fonds_no;
```

### Q3: 修复后会影响数据隔离吗？

**A**: 不会。修复只是确保用户能看到现有数据，未来新创建的数据仍然遵循数据隔离规则。

### Q4: 如何为不同用户配置不同的权限范围？

**A**: 可以通过系统管理界面或直接修改 `sys_user_fonds_scope` 表：

```sql
-- 删除特定用户的所有权限
DELETE FROM sys_user_fonds_scope 
WHERE user_id = 'user-id' AND deleted = 0;

-- 为用户添加特定全宗号的权限
INSERT INTO sys_user_fonds_scope (id, user_id, fonds_no, scope_type, created_time, last_modified_time, deleted)
VALUES ('id', 'user-id', 'FONDS_NO', 'DIRECT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
```

---

## 📚 相关文档

- 修复脚本: `nexusarchive-java/src/main/resources/db/migration/V84__fix_user_fonds_scope_for_existing_data.sql`
- 详细报告: `docs/reports/fix-user-fonds-scope-for-existing-data.md`
- 数据隔离策略: `docs/reports/datascope-fonds-isolation-fix.md`

---

**最后更新**: 2025-01

