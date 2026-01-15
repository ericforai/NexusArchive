# 清除部门数据操作指南

**日期**: 2026-01-14  
**目的**: 清除数据库中已存在的部门数据，确保系统只显示法人实体

---

## 问题说明

数据库中已存在部门数据（如"国内销售部"、"海外销售部"等），这些数据不符合电子会计档案的业务逻辑。系统已添加过滤逻辑，但需要清除缓存和清理现有数据。

---

## 操作步骤

### 步骤 1: 清除后端缓存

后端使用了 Redis 缓存，需要清除 `entityTree` 缓存：

**方法 1: 通过 Redis CLI**
```bash
# 连接 Redis
redis-cli -h localhost -p 16379

# 清除 entityTree 缓存
KEYS entityTree:*
DEL entityTree:tree
```

**方法 2: 重启后端服务**
```bash
# 重启后端服务会自动清除缓存
# 如果使用 Docker
docker-compose restart backend

# 如果本地运行
# 停止并重新启动后端服务
```

### 步骤 2: 清理数据库中的部门数据（可选）

如果需要彻底删除数据库中的部门数据，可以执行以下 SQL：

```sql
-- ⚠️ 警告：执行前请备份数据库！

-- 1. 查看将要删除的部门数据
SELECT id, name, tax_id, parent_id 
FROM sys_entity 
WHERE (name LIKE '%部' AND (tax_id IS NULL OR tax_id = ''))
   OR name LIKE '%部门%'
ORDER BY name;

-- 2. 删除部门数据（确认无误后执行）
DELETE FROM sys_entity 
WHERE (name LIKE '%部' AND (tax_id IS NULL OR tax_id = ''))
   OR name LIKE '%部门%';

-- 3. 验证删除结果
SELECT COUNT(*) FROM sys_entity;
```

### 步骤 3: 刷新前端页面

清除浏览器缓存并刷新页面：
- 按 `Ctrl+Shift+R` (Windows/Linux) 或 `Cmd+Shift+R` (Mac) 强制刷新
- 或清除浏览器缓存后重新访问页面

---

## 验证方法

1. **访问法人管理页面**: `http://localhost:15175/system/settings/org`
2. **检查显示内容**:
   - ✅ 应该只显示法人实体（如"泊冉集团有限公司"）
   - ❌ 不应该显示部门（如"国内销售部"、"海外销售部"等）
3. **检查业务说明**: 页面应该显示"电子会计档案架构：法人 → 全宗 → 档案"

---

## 注意事项

1. **数据备份**: 删除数据前请务必备份数据库
2. **关联数据**: 如果部门数据有关联的全宗或其他数据，需要先处理关联关系
3. **缓存清除**: 清除缓存后，首次访问可能会稍慢（需要重新构建树形结构）

---

## 后续维护

- 新的 ERP 同步会自动过滤部门数据
- 后端和前端都有过滤逻辑，确保不会显示部门数据
- 如果发现仍有部门数据，请检查：
  1. 缓存是否已清除
  2. 过滤逻辑是否正确执行
  3. 数据库中是否还有部门数据
