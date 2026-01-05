# NexusArchive Schema Validation Guide

## 概述

NexusArchive 使用 Entity-Database Schema Validation（实体-数据库模式验证）来确保 MyBatis-Plus 实体类与 PostgreSQL 数据库表结构保持一致。

## 工作原理

`EntitySchemaValidator` 是一个 Spring 组件，在应用启动时自动执行以下操作：

1. **扫描实体**: 扫描 `com.nexusarchive.entity` 包下所有带 `@TableName` 注解的实体类
2. **查询数据库**: 获取每个实体对应的表结构信息（列名、类型）
3. **对比验证**: 将实体字段与数据库列进行对比
4. **报告问题**: 发现不一致时记录警告或错误

## 配置

### 环境变量

| 变量 | 默认值 | 说明 |
|-----|-------|------|
| `SCHEMA_VALIDATION_ENABLED` | `true` | 是否启用验证 |
| `SCHEMA_VALIDATION_FAIL` | `true` | 发现问题时是否阻止启动 |

### 配置方式

#### 开发环境（.env.local）

```env
# 开发环境：启用验证，发现问题时只警告
SCHEMA_VALIDATION_ENABLED=true
SCHEMA_VALIDATION_FAIL=false
```

#### 生产环境（.env.server）

```env
# 生产环境：启用验证，发现问题时阻止启动
SCHEMA_VALIDATION_ENABLED=true
SCHEMA_VALIDATION_FAIL=true
```

### application.yml

```yaml
schema:
  validation:
    enabled: ${SCHEMA_VALIDATION_ENABLED:true}
    fail-on-error: ${SCHEMA_VALIDATION_FAIL:true}
    entity-package: com.nexusarchive.entity
```

## 验证输出示例

### 成功输出

```
[Schema Validator] Starting entity-database consistency check...
[Schema Validator] Checked 53 entities
[Schema Validator] ✓ All entity fields have matching database columns
```

### 发现问题

```
[Schema Validator] Found 1 warnings:
[Schema Validator]   - Table 'sys_position' not found in database (Entity: Position)
[Schema Validator] Found 2 MISSING COLUMNS:
MISSING: Entity 'ArchiveSubmitBatch' field 'createdTime' -> Column 'created_time' not found in table 'archive_batch'
MISSING: Entity 'User' field 'organizationId' -> Column 'organization_id' not found in table 'sys_user'
```

## 常见问题及解决方案

### 1. 列名不匹配

**错误**:
```
MISSING: Entity 'Archive' field 'createdTime' -> Column 'created_time' not found in table 'acc_archive'
```

**原因**: 实体字段使用 `@TableField(value = "created_time")` 指定列名，但数据库表中的列名不同（如 `created_at`）。

**解决方案**:

**方案 A - 修改实体映射**（推荐，当表已存在且有正确列名时）:
```java
@TableField(value = "created_at", fill = FieldFill.INSERT)
private LocalDateTime createdTime;
```

**方案 B - 添加数据库列**（当需要新列时）:
创建 Flyway 迁移添加缺失的列：
```sql
-- VXX__add_column.sql
ALTER TABLE acc_archive ADD COLUMN created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
```

### 2. 表不存在

**错误**:
```
Table 'sys_position' not found in database (Entity: Position)
```

**解决方案**: 创建 Flyway 迁移添加缺失的表：
```sql
-- VXX__create_sys_position_table.sql
CREATE TABLE IF NOT EXISTS sys_position (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    -- ... 其他列
);
```

### 3. 不需要的数据库字段

**场景**: 实体有一个字段仅用于业务逻辑，不需要持久化到数据库。

**解决方案**: 使用 `@TableField(exist = false)` 注解：
```java
/**
 * 组织ID（已替换 departmentId）
 * 注意：数据库暂无此列，标记为不存在
 */
@TableField(exist = false)
private String organizationId;
```

**注意**: 这应该是临时解决方案。长期来看应决定：
- 删除该字段（如果不需要）
- 创建迁移添加数据库列（如果需要持久化）

## 开发工作流程

### 添加新字段时

**正确流程**（单次提交包含实体和迁移）:

1. **编写 Flyway 迁移**（先写数据库变更）:
   ```sql
   -- V86__add_archive_retention_reason.sql
   ALTER TABLE acc_archive ADD COLUMN retention_reason VARCHAR(500);
   COMMENT ON COLUMN acc_archive.retention_reason IS '保管期限变更原因';
   ```

2. **更新实体类**（后写代码变更）:
   ```java
   @TableName("acc_archive")
   public class Archive {
       @TableField("retention_reason")
       private String retentionReason;
   }
   ```

3. **提交**:
   ```bash
   git add src/main/resources/db/migration/V86__*.sql
   git add src/main/java/com/nexusarchive/entity/Archive.java
   git commit -m "feat: 添加保管期限变更原因字段"
   ```

### 重构字段时

**场景**: 将 `department_id` 重命名为 `organization_id`

**正确流程**:

1. **编写迁移**：
   ```sql
   -- V87__rename_dept_to_org.sql
   ALTER TABLE sys_user RENAME COLUMN department_id TO organization_id;
   ```

2. **更新实体**：
   ```java
   @TableField(value = "organization_id")
   private String organizationId;
   ```

3. **测试验证**：重启应用，确认 schema validation 通过

## 调试技巧

### 查看 Schema Validator 日志

```bash
# 查看完整验证日志
docker logs nexus-backend-dev | grep -A 20 "Schema Validator"

# 只看错误
docker logs nexus-backend-dev | grep "Schema Validator.*MISSING"
```

### 手动检查表结构

```bash
# 连接数据库
docker exec -it nexus-db-dev psql -U postgres -d nexusarchive

# 查看表结构
\d acc_archive

# 查看所有表
\dt

# 退出
\q
```

### 临时禁用验证（仅调试用）

```bash
# 在 .env.local 中临时设置
SCHEMA_VALIDATION_ENABLED=false
```

然后重启后端：
```bash
pkill -f "spring-boot:run"
cd nexusarchive-java
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**警告**: 生产环境必须启用验证！

## 最佳实践

1. **始终在开发环境启用验证**: 设置 `SCHEMA_VALIDATION_ENABLED: "true"`
2. **单次提交包含完整变更**: Entity + Migration 在同一个 commit
3. **优先使用迁移修复**: 而非 `@TableField(exist = false)` 权宜之计
4. **CI/CD 中阻止部署**: 生产环境 `SCHEMA_VALIDATION_FAIL: "true"`
5. **定期检查警告**: 即使是警告也要及时处理

## 相关文件

| 文件 | 用途 |
|------|------|
| `EntitySchemaValidator.java` | Schema 验证器实现 |
| `.env.local` | 本地开发环境变量 |
| `.env.server` | 生产环境变量 |
| `application.yml` | Spring 配置 |
| `db/migration/V*.sql` | Flyway 数据库迁移 |

## 快速参考

```bash
# 重启后端（应用配置变更）
pkill -f "spring-boot:run"
cd nexusarchive-java
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 查看验证结果
tail -f backend.log | grep -i "schema validator"

# 连接数据库检查表结构
PGPASSWORD=postgres psql -h localhost -p 54321 -U postgres -d nexusarchive -c "\d table_name"
```
