# NexusArchive 电子会计档案系统 - 集团版成熟度评估报告

> **评估日期**: 2025-12-17
> **评估人**: AI 架构评审
> **评估标准**: 集团级可交付 + 审计可通过

---

## 1. 集团版支持成熟度评估：1.5 级 / 5 级

| 等级 | 定义 | 当前系统状态 |
|------|------|-------------|
| 0 级 | 完全单租户 | ❌ |
| **1 级** | **有组织概念，但混乱** | **✅ 当前水平** |
| 2 级 | 逻辑隔离基本可用 | 部分具备 |
| 3 级 | 集团-子公司模式可交付 | ❌ |
| 4 级 | 支持复杂集团架构 | ❌ |
| 5 级 | 企业级多法人完整支持 | ❌ |

---

## 2. "伪集团版 vs 真集团版" 对照表

| 维度 | 伪集团版特征 | 真集团版要求 | 当前系统状态 | 差距评级 |
|------|-------------|-------------|--------------|---------|
| **组织模型** | 只有 Org 表，type 混用 | 明确区分 Group/LegalEntity/FinancialOrg/BizUnit | `Org.type` 仅区分 COMPANY/DEPARTMENT，无集团/法人概念 | 🔴 严重 |
| **法人主体** | 无法人实体，或混在 Org 里 | 独立 LegalEntity 表，含统一社会信用代码 | 完全缺失，`BasFonds` 承担了部分职责但不完整 | 🔴 严重 |
| **数据隔离** | 查询时靠前端传 orgId | 强制后端拦截，所有 SQL 自动注入组织条件 | `DataScopeService` 手动调用，非强制拦截 | 🟠 高危 |
| **权限体系** | Role 不绑组织，全局共享 | Role 绑定组织 + DataScope 双维度 | `Role` 无 `orgId`，全局角色 | 🔴 严重 |
| **用户归属** | 用户只能属于一个部门 | 用户可多组织任职 | `User.departmentId` 单值 | 🟠 高危 |
| **跨组织角色** | 不支持或 hack 实现 | 明确的跨组织角色模型 | 完全不支持 | 🔴 严重 |
| **配置继承** | 全局唯一配置 | 集团配置 + 子公司覆盖 | `SystemSetting` 无 orgId | 🔴 严重 |
| **档案归属** | 档案属于部门 | 档案属于法人主体/全宗 | `Archive.departmentId` 而非法人 | 🟠 高危 |
| **保管策略** | 全局统一 | 集团统一 + 子公司差异化 | 无组织级策略机制 | 🟠 高危 |
| **销毁流程** | 单层审批 | 集团级审批 + 子公司执行 | `Destruction` 无组织字段 | 🔴 严重 |
| **集团查询** | 查全表，性能爆炸 | 按组织树筛选 + 索引优化 | 无组织维度索引设计 | 🟠 高危 |
| **审计穿透** | 无法实现或权限失控 | 集团审计员只读跨组织访问 | 完全不支持 | 🔴 严重 |

---

## 3. 关键发现详情

### 3.1 组织模型缺陷 (致命)

**现状** (`entity/Org.java`):
```java
// Org.java - 组织层级混乱
private String type; // 仅 COMPANY/DEPARTMENT，无法区分：
                     // - 集团总部
                     // - 法人子公司
                     // - 财务核算主体
                     // - 业务事业部
```

**问题**:
- **无集团概念**: 无法表达"A集团下辖B公司、C公司"
- **法人主体缺失**: 统一社会信用代码、注册资本等法人属性无处存放
- **全宗与法人脱节**: `BasFonds.orgId` 关联的是 Org，但 Org 本身没有法人属性

**涉及文件**:
- `src/main/java/com/nexusarchive/entity/Org.java`
- `src/main/java/com/nexusarchive/entity/BasFonds.java`

### 3.2 数据隔离机制缺陷 (高危)

**现状** (`service/DataScopeService.java`):
```java
// DataScopeService.java - 手动调用模式
public void applyArchiveScope(QueryWrapper<Archive> wrapper, DataScopeContext context) {
    // 问题1: 需要开发者主动调用，容易遗漏
    // 问题2: 基于 departmentId 而非法人/全宗
    // 问题3: 无全局拦截器强制执行
}
```

**致命漏洞**:
- `Destruction` (销毁)、`OpenAppraisal` (鉴定)、`Borrowing` (借阅) 等核心业务实体 **完全没有组织字段**
- 销毁一条档案记录，系统无法判断"这是哪个子公司发起的销毁"

**涉及文件**:
- `src/main/java/com/nexusarchive/service/DataScopeService.java`
- `src/main/java/com/nexusarchive/common/enums/DataScopeType.java`

### 3.3 权限体系缺陷 (致命)

**现状**:
```java
// Role.java - 角色无组织绑定
public class Role {
    private String id;
    private String name;
    private String dataScope;  // 有数据范围
    // ❌ 没有 orgId - 角色是全局的！
}

// User.java - 用户单组织
public class User {
    private String departmentId;  // 单值！用户只能属于一个部门
}
```

**集团场景灾难**:
- 子公司 A 的"档案管理员"角色会被子公司 B 看到
- 无法实现"张三在 A 公司是管理员，在 B 公司是普通员工"
- 集团审计员无法配置为"可访问所有子公司但只读"

**涉及文件**:
- `src/main/java/com/nexusarchive/entity/Role.java`
- `src/main/java/com/nexusarchive/entity/User.java`
- `src/main/java/com/nexusarchive/entity/Permission.java`

### 3.4 档案生命周期缺陷 (高危)

**销毁流程** (`entity/Destruction.java`):
```java
// Destruction.java - 无组织隔离
public class Destruction {
    private String applicantId;
    private String archiveIds;  // JSON
    // ❌ 无 orgId - 无法区分哪个子公司的销毁申请
    // ❌ 无集团级审批字段
}
```

**集团场景问题**:
- 无法实现"子公司发起销毁 → 集团档案管理部审批 → 子公司执行"
- 子公司注销后档案如何转移？完全没有设计

**涉及文件**:
- `src/main/java/com/nexusarchive/entity/Destruction.java`
- `src/main/java/com/nexusarchive/entity/OpenAppraisal.java`
- `src/main/java/com/nexusarchive/entity/Borrowing.java`

### 3.5 配置管理缺陷 (致命)

**现状** (`entity/SystemSetting.java`):
```java
// SystemSetting.java - 全局配置
public class SystemSetting {
    private String configKey;
    private String configValue;
    // ❌ 无 orgId - 所有子公司共用一套配置！
}
```

**集团场景灾难**:
- 保管期限策略：集团要求"合同类30年"，但某子公司因行业特殊要求"合同类永久"——无法实现
- 档案分类编码：子公司 A 用"KJ-01"，子公司 B 用"CW-01"——会冲突
- 元数据模板：无法按子公司定制

**涉及文件**:
- `src/main/java/com/nexusarchive/entity/SystemSetting.java`

---

## 4. 改造清单

### 4.1 最小但不可省略的改造 (MVP)

| 序号 | 改造项 | 涉及实体/表 | 工作量 | 不改后果 |
|------|--------|------------|--------|---------|
| 1 | **新增 LegalEntity 法人实体表** | 新建表 `sys_legal_entity` | 中 | 无法区分法人主体，审计不过 |
| 2 | **Org 增加 legalEntityId** | `sys_org` | 小 | 组织与法人无法关联 |
| 3 | **Role 增加 orgId** | `sys_role` | 中 | 角色无法按组织隔离，权限混乱 |
| 4 | **新增 UserOrgRole 三元关系表** | 新建表 `sys_user_org_role` | 中 | 用户无法多组织任职 |
| 5 | **Destruction 增加 orgId + groupApproverId** | `biz_destruction` | 小 | 销毁流程无法集团管控 |
| 6 | **SystemSetting 增加 orgId** | `sys_setting` | 小 | 配置无法按组织差异化 |
| 7 | **DataScope 强制拦截器** | 新建 `OrgDataScopeInterceptor` | 大 | 数据泄露风险 |
| 8 | **Archive 增加 legalEntityId** | `acc_archive` | 中 | 档案无法归属法人 |

### 4.2 不改一定会出事故

| 问题 | 事故场景 | 严重程度 |
|------|---------|---------|
| Role 无 orgId | A 公司管理员能看到 B 公司的所有角色，甚至能给 B 公司员工分配角色 | 🔴 P0 |
| Destruction 无组织隔离 | A 公司员工能发起销毁 B 公司档案的申请 | 🔴 P0 |
| DataScope 非强制 | 开发新功能时忘记调用 `applyScope()`，数据直接全量返回 | 🔴 P0 |
| User 单组织 | 集团财务总监需要 10 个账号才能管理 10 个子公司 | 🟠 P1 |
| SystemSetting 全局 | 修改保管期限策略影响所有子公司，无法回滚 | 🟠 P1 |

### 4.3 可以 V2 再补的功能

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 档案继承/转移 | 子公司注销后档案自动转移到母公司 | P2 |
| 集团统一报表 | 跨子公司汇总统计 | P2 |
| 合并视图 | 多个法人合并展示（用于审计） | P2 |
| 组织树性能优化 | 大型集团 (100+ 子公司) 的查询优化 | P2 |
| 跨组织借阅审批 | A 公司员工借阅 B 公司档案的流程 | P3 |

---

## 5. 一句话结论

> **这个系统现在能不能卖给集团客户？**
>
> **不能。**
>
> **原因**: 当前系统是典型的 **"单法人系统 + 加了个 Org 表"** 的伪集团版架构。核心缺陷包括：
> 1. **无法人实体模型** - 无法表达"哪个公司"
> 2. **角色全局共享** - 权限必然混乱
> 3. **数据隔离靠自觉** - 必然出数据泄露
> 4. **销毁/借阅无组织** - 审计必不通过
>
> 如果强行部署给集团客户，最乐观的情况是客户投诉"用不了"，最悲观的情况是 **A 子公司看到/删除了 B 子公司的档案**，引发合规事故甚至法律纠纷。
>
> **建议**: 完成 4.1 节的 8 项 MVP 改造后，可以开始接触 **简单集团客户（3-5 个子公司，组织结构扁平）**。复杂集团（事业部制、矩阵式、多级法人嵌套）需要在 V2 补齐后再考虑。

---

## 6. 改造优先级路线图

```
Phase 1 (必须完成才能接集团客户)
├── 1. 新增 sys_legal_entity 表
├── 2. sys_role 增加 org_id 字段
├── 3. 新增 sys_user_org_role 表
├── 4. 实现 DataScope 强制拦截器
└── 5. biz_destruction 增加 org_id

Phase 2 (可边部署边迭代)
├── 6. sys_setting 增加 org_id + 继承机制
├── 7. acc_archive 增加 legal_entity_id
└── 8. 集团审计员角色支持

Phase 3 (复杂集团场景)
├── 9. 档案继承/转移功能
├── 10. 跨组织借阅审批流
└── 11. 集团合并报表
```

---

## 附录 A: 推荐的法人实体表结构

```sql
CREATE TABLE sys_legal_entity (
    id              VARCHAR(32) PRIMARY KEY,
    name            VARCHAR(200) NOT NULL COMMENT '法人名称',
    short_name      VARCHAR(50) COMMENT '简称',
    uscc            VARCHAR(18) UNIQUE COMMENT '统一社会信用代码',
    legal_rep       VARCHAR(50) COMMENT '法定代表人',
    reg_capital     DECIMAL(18,2) COMMENT '注册资本',
    reg_address     VARCHAR(500) COMMENT '注册地址',
    parent_id       VARCHAR(32) COMMENT '母公司ID (集团架构)',
    level           INT DEFAULT 1 COMMENT '层级 (1=集团, 2=一级子公司...)',
    status          VARCHAR(20) DEFAULT 'active' COMMENT '状态',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INT DEFAULT 0
);

-- 索引
CREATE INDEX idx_legal_entity_parent ON sys_legal_entity(parent_id);
CREATE INDEX idx_legal_entity_uscc ON sys_legal_entity(uscc);
```

## 附录 B: 推荐的用户-组织-角色三元关系表

```sql
CREATE TABLE sys_user_org_role (
    id              VARCHAR(32) PRIMARY KEY,
    user_id         VARCHAR(32) NOT NULL,
    org_id          VARCHAR(32) NOT NULL COMMENT '组织ID (可以是法人或部门)',
    role_id         VARCHAR(32) NOT NULL,
    is_primary      BOOLEAN DEFAULT FALSE COMMENT '是否主要任职组织',
    effective_from  DATE COMMENT '生效日期',
    effective_to    DATE COMMENT '失效日期',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(user_id, org_id, role_id)
);

-- 索引
CREATE INDEX idx_uor_user ON sys_user_org_role(user_id);
CREATE INDEX idx_uor_org ON sys_user_org_role(org_id);
CREATE INDEX idx_uor_role ON sys_user_org_role(role_id);
```

## 附录 C: DataScope 强制拦截器伪代码

```java
@Intercepts({
    @Signature(type = Executor.class, method = "query", args = {...})
})
public class OrgDataScopeInterceptor implements Interceptor {

    // 需要强制组织隔离的表
    private static final Set<String> SCOPED_TABLES = Set.of(
        "acc_archive", "biz_destruction", "biz_borrowing",
        "biz_open_appraisal", "sys_role"
    );

    @Override
    public Object intercept(Invocation invocation) {
        // 1. 获取当前用户的组织上下文
        OrgContext ctx = OrgContextHolder.get();

        // 2. 解析 SQL，判断是否涉及需隔离的表
        String sql = extractSql(invocation);
        if (needsOrgFilter(sql, SCOPED_TABLES)) {
            // 3. 强制注入 org_id 条件
            sql = injectOrgCondition(sql, ctx.getOrgIds());
        }

        // 4. 执行
        return invocation.proceed();
    }
}
```

---

## 修订历史

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| 1.0 | 2025-12-17 | 初始评估报告 | AI 架构评审 |
