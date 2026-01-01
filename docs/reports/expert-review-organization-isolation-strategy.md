# 专家组评审：集团型架构下的组织隔离策略厘清

> **评审日期**: 2025-01  
> **评审模式**: 虚拟专家组联合会诊  
> **评审议题**: 集团型架构下是否所有数据都需要 organization_id，还是通过权限隔离？

---

## 🔍 问题背景

用户疑问：
1. 集团型架构下，是否**所有功能和数据、表单**都需要加上 `organization_id`？
2. 还是根据**用户权限**来区分，用户只能看到他所在组织的数据？
3. 对当前设计思路感到困惑，需要厘清架构策略。

---

## 📋 三位专家联合评审

### 1️⃣ 合规专家（Compliance Authority）

**核心观点**：**数据隔离必须基于"全宗（Fonds）"，而非"组织（Organization）"**

**法规依据**：
- 《档案法》明确要求：档案管理以"全宗"为基本单位
- DA/T 94-2020《电子会计档案管理规范》：归档数据必须绑定全宗号
- **财会〔2020〕6号**：电子凭证归档需按全宗管理

**关键发现**：
1. **PRD明确说明**（`docs/product/prd-v1.0.md:10`）：
   ```
   全宗隔离（硬约束）：所有业务数据必须绑定 fonds_no；
   后端从登录态/授权票据解析 allowed_fonds 并校验 current_fonds_no
   ```

2. **法人仅管理维度**（PRD:12）：
   ```
   entity_id 仅用于治理、统计与合规台账，不作为数据隔离键
   ```

3. **组织边界仅法人**（PRD:24）：
   ```
   现有文档中涉及"组织/部门"的表述仅用于业务审批角色，不作为数据隔离维度
   ```

**合规风险评估**：
- 🛑 **P0级风险**：如果使用 `organization_id` 作为数据隔离键，违反档案法规要求
- ✅ **正确做法**：必须使用 `fonds_no`（全宗号）作为数据隔离键

**建议**：
- 立即检查所有业务数据表，确保使用 `fonds_no` 而非 `organization_id` 作为隔离键
- `organization_id` 仅用于：
  - 用户归属标识（哪个组织的员工）
  - 业务审批流程（审批角色归属）
  - 统计报表维度
  - **不参与数据访问控制**

---

### 2️⃣ 信创架构师（Xinchuang Architect）

**核心观点**：**两层隔离机制：逻辑隔离（fonds_no）+ 权限控制（organization）**

**架构分析**：

#### 当前代码现状

1. **数据隔离层（DataScopeService）**：
   - 当前使用 `departmentId` 进行数据过滤
   - `applyArchiveScope()` 方法基于 `department_id` 字段过滤
   - **问题**：与PRD要求的 `fonds_no` 隔离不一致

2. **实体层**：
   - `User` 实体：已有 `organizationId` 字段（✅ 正确，用于用户归属）
   - `Archive` 实体：需要检查是否有 `fonds_no` 字段
   - `EmployeeLifecycleEvent`：有 `organizationId`（✅ 正确，用于记录事件）

3. **数据模型设计**：
   ```sql
   -- 正确的隔离键应该是：
   Archive.fonds_no  -- ✅ 数据隔离键
   User.organization_id  -- ✅ 用户归属标识（不参与数据隔离）
   ```

**架构建议**：

#### 方案A：双键隔离（推荐）

```
数据隔离 = fonds_no（全宗隔离，硬约束）
          + organization_id（权限过滤，软约束）
```

**实现逻辑**：
1. **第一层：全宗隔离（硬约束）**
   - 所有业务数据表必须有 `fonds_no` 字段
   - 查询时强制过滤：`WHERE fonds_no IN (user.allowed_fonds)`
   - 写入时强制绑定：`INSERT ... VALUES (..., current_fonds_no)`

2. **第二层：组织权限（软约束）**
   - 用户表 `organization_id` 用于：
     - 用户生命周期管理（入职、调岗、离职）
     - 审批流程中的组织归属
     - 统计报表的组织维度
   - **不参与数据查询过滤**（因为数据隔离已由 `fonds_no` 保证）

#### 方案B：纯权限隔离（不推荐）

```
数据隔离 = organization_id（组织隔离）
```

**问题**：
- ❌ 违反档案法规（必须按全宗管理）
- ❌ 无法满足审计要求（审计按全宗出具报告）

**风险评估**：
- 🛑 **架构风险**：违反领域模型设计
- 🛑 **合规风险**：不符合档案管理规范

---

### 3️⃣ 交付专家（Delivery Strategist）

**核心观点**：**清晰的定义和文档化是交付成功的关键**

**交付风险评估**：

1. **概念混淆风险**：
   - "组织（Organization）" vs "全宗（Fonds）"
   - "部门（Department）" vs "全宗（Fonds）"
   - 开发团队可能混淆这些概念，导致实现错误

2. **数据迁移风险**：
   - 如果现有数据使用 `department_id` 或 `organization_id` 作为隔离键
   - 需要迁移到 `fonds_no`，工作量巨大

3. **用户培训风险**：
   - 业务用户需要理解"全宗"概念
   - 需要清晰的用户手册和培训材料

**交付建议**：

#### 1. 立即行动项

1. **代码审计**：
   ```bash
   # 检查所有业务数据表是否使用 fonds_no
   grep -r "fonds_no\|fondsNo" nexusarchive-java/src/main/java/com/nexusarchive/entity/
   
   # 检查数据隔离服务是否正确使用 fonds_no
   grep -r "applyArchiveScope\|DataScopeService" --include="*.java"
   ```

2. **数据库表审计**：
   - 列出所有业务数据表
   - 检查每个表是否有 `fonds_no` 字段
   - 检查是否有 `department_id` 或 `organization_id` 作为隔离键

3. **修复数据隔离逻辑**：
   - `DataScopeService.applyArchiveScope()` 必须改为使用 `fonds_no`
   - 移除基于 `department_id` 的过滤逻辑

#### 2. 文档化要求

1. **架构设计文档**：
   - 明确"全宗隔离"vs"组织归属"的区别
   - 画出数据隔离流程图
   - 标注每个字段的用途

2. **开发规范**：
   - 所有业务数据表必须有 `fonds_no` 字段
   - `organization_id` 仅用于用户归属和统计，不参与数据隔离
   - 代码审查清单：检查数据隔离逻辑

---

## 🔗 全宗（Fonds）vs 组织（Organization）关系厘清

### 概念定义

| 概念 | 定义 | 用途 | 隔离作用 |
|------|------|------|----------|
| **全宗（Fonds）** | 档案管理的基本单位，一个全宗代表一个独立的档案集合 | **数据隔离键**（硬约束） | ✅ **必须参与数据隔离** |
| **组织（Organization）** | 企业组织架构中的单位（法人、子公司、部门等） | 用户归属、审批流程、统计维度 | ❌ **不参与数据隔离** |
| **法人（Entity）** | 法人实体，用于治理和合规台账 | 合规台账、统计报表 | ❌ **不参与数据隔离** |

### 关系图

```
┌─────────────────────────────────────┐
│  组织架构（Organization Tree）       │
│  ┌─────────┐  ┌─────────┐          │
│  │ 集团总部 │  │ 子公司A │          │
│  └────┬────┘  └────┬────┘          │
│       │            │                │
│  ┌────▼────┐  ┌────▼────┐          │
│  │ 财务部  │  │ 财务部  │          │
│  └─────────┘  └─────────┘          │
└─────────────────────────────────────┘
            │              │
            │              │（用户归属）
            │              │
            ▼              ▼
┌─────────────────────────────────────┐
│  用户（User）                        │
│  - organization_id: "财务部A"        │  ← 仅用于用户归属
│  - allowed_fonds: ["F001", "F002"]  │  ← 允许访问的全宗列表
│  - current_fonds: "F001"            │  ← 当前操作的全宗
└─────────────────────────────────────┘
            │
            │（数据隔离）
            │
            ▼
┌─────────────────────────────────────┐
│  档案数据（Archive）                 │
│  - fonds_no: "F001"                 │  ← 数据隔离键（硬约束）
│  - archive_year: 2024               │
│  - title: "2024年1月记账凭证"        │
└─────────────────────────────────────┘
```

### 关键原则

1. **一个法人（Entity）可以对应多个全宗**
   - 根据PRD：`sys_fonds` 为最高档案容器，`fonds_no` 为逻辑隔离键；一个法人可对应多个全宗
   - 例如：某法人实体（如集团公司）下可能有多个全宗（F001、F002），分别对应不同时期或不同业务线的档案
   - **注意**：档案学标准中，全宗通常对应一个立档单位；但在集团型架构中，一个法人实体可能管理多个全宗（如合并、分立、历史沿革等情况）

2. **一个用户可以访问多个全宗**
   - 通过 `allowed_fonds` 列表控制
   - 跨全宗访问需要授权票据（AuthTicket）

3. **数据隔离基于全宗，而非组织**
   - 查询时：`WHERE fonds_no IN (user.allowed_fonds)`
   - 写入时：自动绑定 `current_fonds_no`
   - **组织（Organization）** 仅用于用户归属、审批流程、统计维度，不参与数据隔离

---

## ⚖️ 专家组联合建议

### 🛑 阻断点（Showstoppers）

1. **合规专家**：
   - 🛑 **P0级风险**：`DataScopeService` 当前使用 `department_id` 进行数据隔离，违反档案法规
   - **必须立即修复**：改为使用 `fonds_no` 进行数据隔离

2. **架构师**：
   - 🛑 **架构风险**：如果所有表都加 `organization_id` 作为隔离键，会导致：
     - 违反领域模型设计（档案管理以全宗为单位）
     - 数据冗余（一个全宗可能属于多个组织）
     - 查询性能下降（需要多表关联）

### ✅ 正确做法

#### 数据表设计原则

1. **业务数据表（Archive, BorrowRecord等）**：
   ```sql
   CREATE TABLE archive (
       id VARCHAR(64) PRIMARY KEY,
       fonds_no VARCHAR(50) NOT NULL,  -- ✅ 数据隔离键（必须）
       archive_year INT NOT NULL,
       -- ... 其他字段
       INDEX idx_fonds_year (fonds_no, archive_year)  -- ✅ 索引
   );
   ```
   - ✅ **必须有 `fonds_no` 字段**
   - ❌ **不需要 `organization_id` 字段**（组织信息通过全宗关联获取）

2. **用户表（User）**：
   ```sql
   CREATE TABLE sys_user (
       id VARCHAR(64) PRIMARY KEY,
       organization_id VARCHAR(64),  -- ✅ 用户归属（不参与数据隔离）
       allowed_fonds TEXT,  -- ✅ JSON数组，允许访问的全宗列表
       current_fonds VARCHAR(50),  -- ✅ 当前操作的全宗
       -- ... 其他字段
   );
   ```
   - ✅ `organization_id` 用于用户归属、审批流程
   - ✅ `allowed_fonds` 用于数据访问控制
   - ✅ `current_fonds` 用于当前操作上下文

3. **全宗表（Fonds）**：
   ```sql
   CREATE TABLE sys_fonds (
       id VARCHAR(64) PRIMARY KEY,
       fonds_no VARCHAR(50) UNIQUE NOT NULL,  -- ✅ 全宗号
       fonds_name VARCHAR(100) NOT NULL,
       entity_id VARCHAR(64),  -- ✅ 关联法人（仅用于统计）
       -- ... 其他字段
   );
   ```

#### 数据隔离实现

1. **查询时的隔离**：
   ```java
   // ✅ 正确：基于 fonds_no 过滤
   public void applyArchiveScope(QueryWrapper<Archive> wrapper, DataScopeContext context) {
       if (context == null || context.isAll()) {
           return;
       }
       // 从用户上下文获取允许的全宗列表
       List<String> allowedFonds = context.getAllowedFonds();
       if (!allowedFonds.isEmpty()) {
           wrapper.in("fonds_no", allowedFonds);  // ✅ 使用 fonds_no
       }
   }
   ```

2. **写入时的隔离**：
   ```java
   // ✅ 正确：自动绑定当前全宗
   public void createArchive(CreateArchiveRequest request) {
       Archive archive = new Archive();
       // ... 设置其他字段
       archive.setFondsNo(getCurrentFondsNo());  // ✅ 从登录态获取
       archiveMapper.insert(archive);
   }
   ```

---

## 📋 检查清单

### 立即检查项

- [ ] **检查所有业务数据表是否有 `fonds_no` 字段**
- [ ] **检查 `DataScopeService` 是否使用 `fonds_no` 进行数据隔离**
- [ ] **检查是否有表使用 `department_id` 或 `organization_id` 作为隔离键**
- [ ] **检查用户表是否有 `allowed_fonds` 和 `current_fonds` 字段**
- [ ] **检查全宗表是否关联 `entity_id`（法人）**

### 修复项

- [ ] **修复 `DataScopeService.applyArchiveScope()` 使用 `fonds_no`**
- [ ] **移除 `Archive` 表中的 `department_id` 字段（如果存在）**
- [ ] **确保所有业务数据表都有 `fonds_no` 字段**
- [ ] **添加 MyBatis 拦截器，自动注入 `fonds_no` 过滤条件**

---

## 🎯 总结

### 核心答案

**问题1：是否所有功能和数据、表单都需要加上 organization_id？**

**答案**：❌ **不需要**
- 业务数据表（Archive等）**不需要** `organization_id` 字段
- 只需要 `fonds_no`（全宗号）作为数据隔离键
- `organization_id` 仅用于：
  - 用户表（User）的用户归属
  - 审批流程中的组织归属
  - 统计报表的组织维度

**问题2：还是根据用户权限来区分，用户只能看到他所在组织的数据？**

**答案**：✅ **部分正确，但不完整**
- 数据隔离基于**全宗（fonds_no）**，而非组织
- 用户通过 `allowed_fonds` 列表控制可以访问哪些全宗
- 用户的 `organization_id` **不参与数据隔离**，仅用于用户归属标识

**问题3：厘清架构策略**

**架构策略**：
1. **数据隔离层**：基于 `fonds_no`（全宗隔离，硬约束）
2. **用户归属层**：基于 `organization_id`（组织归属，不参与数据隔离）
3. **权限控制层**：基于 `allowed_fonds`（允许访问的全宗列表）

---

**评审状态**: ✅ **完成**  
**优先级**: 🛑 **P0（阻断级别）**  
**下一步**: 立即修复 `DataScopeService` 的数据隔离逻辑

