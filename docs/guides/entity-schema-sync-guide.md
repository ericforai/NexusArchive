# Entity-Database Schema 同步问题根本解决方案

## 问题根源

每次升级都遇到 `column xxx does not exist` 错误，根本原因：

### 1. Schema验证器未生效
```yaml
# application.yml 当前配置
schema:
  validation:
    fail-on-error: false  # 只警告，不阻止启动
```

### 2. 硬编码字符串列名
```java
// ❌ 无法被编译器检查
wrapper.orderByDesc("created_time")

// ✅ 应该使用 Lambda（类型安全）
wrapper.orderByDesc(Archive::getCreatedTime)
```

### 3. 命名约定不统一
- Java: `createdTime` (驼峰)
- DB: `created_at` (下划线)
- 代码字符串: `created_time` (错误的下划线)

---

## 立即修复（防止再次发生）

### Step 1: 修改验证器配置

```yaml
# application.yml
schema:
  validation:
    enabled: true
    fail-on-error: true  # 改为 true，发现不一致立即阻止启动
```

### Step 2: 添加强类型检查

```java
// ❌ 旧代码 - 字符串字面量
wrapper.orderByDesc("created_time")
wrapper.eq("status", "active")

// ✅ 新代码 - Lambda 类型安全
wrapper.orderByDesc(Archive::getCreatedTime)
wrapper.eq(Archive::getStatus, "active")
```

### Step 3: 统一命名约定

创建 `FieldNames` 常量类：

```java
public class ArchiveFields {
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String STATUS = "status";
    public static final String MATCH_SCORE = "match_score";
}

// 使用
wrapper.orderByDesc(ArchiveFields.CREATED_AT);
```

---

## 开发流程规范

### 添加新字段的正确流程

1. **修改 Entity**
```java
@Data
@TableName("acc_archive")
public class Archive {
    @TableField("match_score")
    private Integer matchScore;
}
```

2. **创建 Flyway Migration**
```sql
-- V3__add_match_score.sql
ALTER TABLE acc_archive ADD COLUMN match_score INTEGER DEFAULT 0;
```

3. **启动应用验证**
```bash
# fail-on-error=true 时，启动会失败并显示：
# [Schema Validator] MISSING: Entity 'Archive' field 'matchScore' -> Column 'match_score' not found
```

4. **先运行 Migration，再启动应用**
```bash
mvn flyway:migrate
mvn spring-boot:run
```

---

## 代码审查清单

每次代码变更必须检查：

- [ ] Entity 新增字段 → 创建对应的 Flyway migration
- [ ] 避免硬编码字符串 → 使用 Lambda 或常量
- [ ] `schema.validation.fail-on-error=true` 确保启用
- [ ] 本地测试：先 migration，再启动

---

## 工具脚本

### 自动检查脚本

```bash
#!/bin/bash
# scripts/check-schema-sync.sh

echo "Checking Entity-DB schema consistency..."

# 启动应用（会自动运行验证器）
mvn spring-boot:run -Dspring-boot.run.profiles=dev 2>&1 | \
  grep -A 5 "Schema Validator" || echo "Schema validation passed"

# 如果 fail-on-error=true，有不一致会直接失败退出
```

### 修复所有硬编码字符串

```bash
# 查找所有可能的硬编码列名
grep -r "orderByDesc\|orderByAsc\|\.eq(" \
  --include="*.java" \
  src/main/java/com/nexusarchive/service | \
  grep -v "Lambda\|::"
```
