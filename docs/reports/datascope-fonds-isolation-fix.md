# DataScopeService 数据隔离逻辑修复报告

> **修复日期**: 2025-01  
> **修复类型**: P0级阻断问题修复  
> **修复依据**: 专家组评审报告（`docs/reports/expert-review-organization-isolation-strategy.md`）

---

## 🎯 修复目标

根据专家组评审结论，修复 `DataScopeService` 的数据隔离逻辑：
- **问题**：当前使用 `department_id` 进行数据隔离，违反档案法规要求
- **目标**：改为使用 `fonds_no`（全宗号）进行数据隔离

---

## 📋 修复内容

### 1. CustomUserDetails（用户认证详情）

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/security/CustomUserDetails.java`

**修改内容**:
- ✅ 添加 `allowedFonds` 字段（`List<String>`）：存储用户允许访问的全宗号列表
- ✅ 保留 `departmentId` 字段：仅用于用户归属标识，不参与数据隔离
- ✅ 更新构造函数，接收 `allowedFonds` 参数

**关键代码**:
```java
private final List<String> allowedFonds; // 允许访问的全宗号列表（数据隔离键）

public CustomUserDetails(..., List<String> allowedFonds, ...) {
    // ...
    this.allowedFonds = allowedFonds != null ? allowedFonds : new ArrayList<>();
}
```

---

### 2. CustomUserDetailsService（用户详情加载服务）

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/service/CustomUserDetailsService.java`

**修改内容**:
- ✅ 注入 `SysUserFondsScopeMapper`，用于查询用户全宗权限
- ✅ 在 `buildUserDetails()` 方法中，调用 `userFondsScopeMapper.findFondsNoByUserId()` 获取用户允许访问的全宗列表
- ✅ 将 `allowedFonds` 传递给 `CustomUserDetails` 构造函数

**关键代码**:
```java
private final SysUserFondsScopeMapper userFondsScopeMapper;

// 获取用户允许访问的全宗列表
List<String> allowedFonds = userFondsScopeMapper.findFondsNoByUserId(user.getId());

return new CustomUserDetails(
    // ...
    allowedFonds, // 传递全宗列表
    // ...
);
```

---

### 3. DataScopeService（数据权限服务）

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/service/DataScopeService.java`

**修改内容**:

#### 3.1 resolve() 方法
- ✅ 从 `CustomUserDetails` 获取 `allowedFonds` 列表
- ✅ 移除基于 `departmentId` 的权限计算逻辑
- ✅ 移除 `OrgMapper` 依赖（不再需要组织树查询）

**关键代码**:
```java
public DataScopeContext resolve() {
    // ...
    // 获取用户允许访问的全宗列表（数据隔离键）
    List<String> allowedFonds = userDetails.getAllowedFonds();
    
    return new DataScopeContext(scopeType, userId, 
        allowedFonds != null ? new LinkedHashSet<>(allowedFonds) : Collections.emptySet());
}
```

#### 3.2 applyArchiveScope() 方法
- ✅ 改为使用 `fonds_no` 进行数据过滤（`wrapper.in("fonds_no", allowedFonds)`）
- ✅ 移除所有基于 `department_id` 的过滤逻辑

**关键代码**:
```java
public void applyArchiveScope(QueryWrapper<Archive> wrapper, DataScopeContext context) {
    // ...
    // 基于 fonds_no（全宗号）进行数据隔离
    Set<String> allowedFonds = context.allowedFonds();
    if (!allowedFonds.isEmpty()) {
        wrapper.in("fonds_no", allowedFonds);
    } else {
        wrapper.eq("1", "0"); // 不允许访问任何数据
    }
}
```

#### 3.3 canAccessArchive() 方法
- ✅ 改为基于 `fonds_no` 进行访问控制检查
- ✅ 移除基于 `departmentId` 的访问控制逻辑

**关键代码**:
```java
public boolean canAccessArchive(Archive archive, DataScopeContext context) {
    // ...
    // 基于 fonds_no（全宗号）进行数据隔离
    String fondsNo = archive.getFondsNo();
    if (StringUtils.hasText(fondsNo) && context.allowedFonds().contains(fondsNo)) {
        return true;
    }
    return false;
}
```

#### 3.4 DataScopeContext 记录
- ✅ 简化为只包含 `type`, `userId`, `allowedFonds` 三个字段
- ✅ 移除 `departmentIds` 和 `departmentId` 字段

**关键代码**:
```java
public record DataScopeContext(
    DataScopeType type, 
    String userId, 
    Set<String> allowedFonds  // 允许访问的全宗号列表（数据隔离键）
) {
    // ...
}
```

#### 3.5 移除的方法
- ❌ 移除 `resolveDepartmentId()` 方法
- ❌ 移除 `computeDepartmentSet()` 方法
- ❌ 移除 `collectDepartmentIds()` 方法
- ❌ 移除 `collectChildren()` 方法
- ❌ 移除 `OrgMapper` 依赖

---

### 4. BorrowingScopePolicyImpl（借阅数据权限策略）

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/modules/borrowing/infra/BorrowingScopePolicyImpl.java`

**修改内容**:
- ✅ 改为使用 `fonds_no` 进行借阅数据过滤
- ✅ 移除对 `context.departmentIds()` 和 `context.departmentId()` 的调用
- ✅ 移除对 `ArchiveService.getArchiveIdsByDepartmentIds()` 的调用
- ✅ 移除 `ArchiveService` 依赖（不再需要）

**关键代码**:
```java
@Override
public void apply(QueryWrapper<Borrowing> wrapper, DataScopeContext context) {
    // ...
    // 基于 fonds_no（全宗号）进行数据隔离
    Set<String> allowedFonds = context.allowedFonds();
    if (!allowedFonds.isEmpty()) {
        wrapper.in("fonds_no", allowedFonds);
        return;
    }
    // 如果没有允许访问的全宗，则不允许访问任何数据
    wrapper.eq("1", "0");
}
```

---

## ✅ 修复验证

### 编译检查
- ✅ 所有 Java 文件编译通过
- ✅ 无 linter 错误

### 代码检查清单
- [x] `CustomUserDetails` 包含 `allowedFonds` 字段
- [x] `CustomUserDetailsService` 加载用户全宗权限
- [x] `DataScopeService.resolve()` 使用 `allowedFonds`
- [x] `DataScopeService.applyArchiveScope()` 使用 `fonds_no` 过滤
- [x] `DataScopeService.canAccessArchive()` 基于 `fonds_no` 检查
- [x] `BorrowingScopePolicyImpl` 使用 `fonds_no` 过滤
- [x] 所有 `department_id` 相关的数据隔离逻辑已移除

---

## 🔗 数据隔离策略总结

### 正确的数据隔离架构

```
┌─────────────────────────────────────┐
│  用户登录 (CustomUserDetails)        │
│  - allowedFonds: ["F001", "F002"]   │  ← 数据隔离键
│  - organizationId: "ORG-001"        │  ← 用户归属（不参与隔离）
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  DataScopeService.resolve()         │
│  - 从 CustomUserDetails 获取        │
│    allowedFonds 列表                │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  DataScopeContext                   │
│  - type: ALL/SELF/...               │
│  - userId: "user-001"               │
│  - allowedFonds: ["F001", "F002"]   │  ← 数据隔离键
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  数据查询过滤                        │
│  WHERE fonds_no IN ('F001', 'F002') │  ← 基于全宗号
└─────────────────────────────────────┘
```

### 关键原则

1. **数据隔离基于 `fonds_no`（全宗号）**：
   - 所有业务数据表必须有 `fonds_no` 字段
   - 查询时强制过滤：`WHERE fonds_no IN (user.allowed_fonds)`
   - 写入时自动绑定：`INSERT ... VALUES (..., current_fonds_no)`

2. **`organization_id` 仅用于用户归属**：
   - 用户表（User）的 `organization_id` 用于标识用户属于哪个组织
   - 用于审批流程中的组织归属判断
   - 用于统计报表的组织维度
   - **不参与数据查询过滤**

3. **用户全宗权限存储在 `sys_user_fonds_scope` 表**：
   - 表结构：`user_id`, `fonds_no`, `scope_type`
   - 查询方法：`SysUserFondsScopeMapper.findFondsNoByUserId()`

---

## ⚠️ 注意事项

### 数据库迁移

虽然代码已修复，但可能需要数据库迁移：

1. **确保所有业务数据表有 `fonds_no` 字段**：
   - `acc_archive` 表：已有 `fonds_no` 字段 ✅
   - `biz_borrowing` 表：已有 `fonds_no` 字段 ✅
   - 其他业务表：需要检查

2. **确保 `sys_user_fonds_scope` 表有数据**：
   - 迁移脚本 `V79__fonds_audit_borrow_alignment.sql` 已有初始化逻辑（基于用户的 `org_code`）
   - **修复脚本 `V84__fix_user_fonds_scope_for_existing_data.sql`** 已创建，用于确保所有用户都能访问现有数据的全宗号
   - 需要确保现有用户都有对应的全宗权限记录

### 其他可能需要更新的代码

虽然主要的数据隔离逻辑已修复，但可能还有其他地方需要检查：

- [ ] 其他使用 `DataScopeContext` 的地方
- [ ] 其他业务服务中的数据查询逻辑
- [ ] 前端 API 是否需要传递 `fonds_no` 参数

---

## 📊 修复影响

### 正面影响

1. ✅ **符合档案法规要求**：数据隔离基于全宗，符合《档案法》和 DA/T 94-2020 要求
2. ✅ **架构更清晰**：明确区分数据隔离（`fonds_no`）和用户归属（`organization_id`）
3. ✅ **代码更简洁**：移除了复杂的组织树遍历逻辑

### 潜在风险

1. ⚠️ **现有数据兼容性**：
   - 如果现有数据使用 `department_id` 作为隔离键，需要数据迁移
   - 需要确保所有用户都有对应的全宗权限记录
   - **已通过 V84 迁移脚本修复**：为所有用户添加现有数据全宗号的权限

2. ⚠️ **性能影响**：
   - 基于 `fonds_no` 的过滤可能需要确保索引存在
   - 建议检查 `acc_archive.fonds_no` 和 `biz_borrowing.fonds_no` 的索引

---

## ✅ 修复状态

- [x] CustomUserDetails 添加 allowedFonds 字段
- [x] CustomUserDetailsService 加载用户全宗权限
- [x] DataScopeService 改为使用 fonds_no 进行数据隔离
- [x] BorrowingScopePolicyImpl 改为使用 fonds_no 进行过滤
- [x] 移除所有基于 department_id 的数据隔离逻辑
- [x] 代码编译通过，无错误

---

**修复完成时间**: 2025-01  
**修复人员**: AI Assistant  
**审核状态**: 待代码审查和测试验证

