# 数据库 Schema 同步规范

## 核心原则

> **规则**：每次修改 Entity 类的字段时，**必须同时创建 Flyway 迁移脚本**。

---

## 开发检查清单

修改 Entity 时必须完成以下步骤：

- [ ] 在 Entity 类中添加/修改字段
- [ ] 创建 Flyway 迁移脚本 `V{n}__描述.sql`
- [ ] 迁移脚本使用 `IF NOT EXISTS` / `IF EXISTS` 确保幂等性
- [ ] 本地执行 `mvn spring-boot:run` 验证迁移成功
- [ ] 测试相关 API 功能正常

---

## Flyway 迁移脚本规范

### 命名规则
```
V{版本号}__{描述}.sql
```
示例：`V28__add_certificate_to_arc_file_content.sql`

### 内容规范
```sql
-- 添加列 (幂等)
ALTER TABLE {table_name} ADD COLUMN IF NOT EXISTS {column_name} {type};

-- 删除列 (幂等)
ALTER TABLE {table_name} DROP COLUMN IF EXISTS {column_name};

-- 添加注释
COMMENT ON COLUMN {table_name}.{column_name} IS '描述';
```

---

## 常见问题及解决

### 问题：Entity 有字段但数据库没有列
**原因**：添加 Entity 字段时忘记创建迁移脚本

**解决**：
1. 创建补偿迁移脚本
2. 重启服务或执行 `mvn flyway:migrate`

### 问题：Flyway 迁移版本冲突
**原因**：多人开发时版本号重复

**解决**：
1. 使用 `out-of-order: true` 配置
2. 检查 `flyway_schema_history` 表确认已执行的迁移

---

## 参考案例

### 2025-12-09 certificate 列缺失事件

**问题**：`ArcFileContent.java` 包含 `certificate` 字段，但 V10/V11 迁移脚本遗漏了此列。

**教训**：
- Entity 修改必须伴随迁移脚本
- 使用 IDE 插件或脚本自动检查 Entity-DB 一致性
