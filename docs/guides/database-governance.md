# 数据库 Schema 治理规范

> **适用范围**: NexusArchive 全体开发人员  
> **生效日期**: 2025-12-09  
> **更新历史**: 基于 Entity-Schema 不同步问题分析制定

---

## 核心规则

> [!CAUTION]
> **Entity 修改必须配套迁移脚本**  
> 任何对 Entity 类字段的增删改，都必须同步创建 Flyway 迁移脚本。

---

## 开发流程检查清单

修改 Entity 类时，请逐项确认：

- [ ] Entity 添加/修改/删除字段
- [ ] 创建 `V{n}__描述.sql` 迁移脚本
- [ ] 使用 `IF NOT EXISTS` / `IF EXISTS` 确保幂等性
- [ ] 本地启动验证迁移成功（无 Schema Validator 警告）
- [ ] 测试相关 API 正常

---

## 迁移脚本规范

### 命名规则

```
V{版本号}__{描述}.sql

示例:
V32__add_user_avatar_column.sql
V33__rename_status_to_state.sql
```

### 编写模板

```sql
-- 添加列 (幂等)
ALTER TABLE {table_name} ADD COLUMN IF NOT EXISTS {column_name} {type};
COMMENT ON COLUMN {table_name}.{column_name} IS '描述';

-- 删除列 (幂等)
ALTER TABLE {table_name} DROP COLUMN IF EXISTS {column_name};

-- 修改列类型
ALTER TABLE {table_name} ALTER COLUMN {column_name} TYPE {new_type};
```

---

## SM4 加密字段长度规范

> [!WARNING]
> **加密字段需要更大的存储空间**  
> 使用 SM4 加密的字段，存储长度会显著膨胀（约 1.5-2 倍）。

### 长度计算公式

```
存储长度 = ceil(明文长度 / 16) * 16 * 4 / 3 + 16
         = Base64(SM4加密(明文) + IV)
```

### 推荐字段长度

| 字段用途 | 明文最大长度 | 推荐存储长度 |
|----------|-------------|-------------|
| 标题 (title) | 255 字符 | `VARCHAR(1000)` |
| 摘要 (summary) | 500 字符 | `VARCHAR(2000)` |
| 姓名 (creator) | 100 字符 | `VARCHAR(500)` |
| 通用字段 | N 字符 | `VARCHAR(N * 3)` |

### 相关实体

以下实体的加密字段已按规范调整（V33 迁移）：

- `Archive.title` → VARCHAR(1000)
- `Archive.summary` → VARCHAR(2000)
- `Archive.creator` → VARCHAR(500)

---

## 防护机制

### EntitySchemaValidator

系统内置 Schema 验证器，在应用启动时自动检测 Entity 与数据库的一致性。

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `schema.validation.enabled` | `true` | 是否启用验证 |
| `schema.validation.fail-on-error` | `true` | 不一致时阻止启动 |

> [!IMPORTANT]
> 从 2025-12-09 起，`fail-on-error` 默认为 `true`。  
> Schema 不一致时应用将**拒绝启动**。

### 开发环境调试

如需临时关闭（仅限调试）：

```bash
export SCHEMA_VALIDATION_FAIL=false
mvn spring-boot:run
```

---

## 历史教训

| 日期 | 问题 | 影响 | 教训 |
|------|------|------|------|
| 2025-12-10 | `title` 列长度不足 | 审批 500 错误 | SM4 加密后数据膨胀超出 VARCHAR(255) 限制 |
| 2025-12-09 | `certificate` 列缺失 | 查询失败 | Entity 有字段但迁移脚本遗漏 |
| 2025-12-09 | `pre_archive_status` 列缺失 | 500 错误 | 新功能开发未同步更新数据库 |
| 2025-12-09 | `sys_setting` 多列缺失 | 启动警告 | 批量字段添加后未核对 |

---

## 故障排查

### 启动时报 Schema 验证失败

```
[Schema Validator] Found X MISSING COLUMNS!
```

**解决方法**:
1. 查看日志中具体缺失的列
2. 创建迁移脚本添加这些列
3. 重启应用验证

### 查询当前迁移版本

```bash
psql -U postgres -d nexusarchive -c \
  "SELECT version, description FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
```

---

## 参考资料

- [Flyway 官方文档](https://flywaydb.org/documentation/)
- [DA/T 94-2022 会计档案数据标准](file:///Users/user/nexusarchive/docs/references/)
