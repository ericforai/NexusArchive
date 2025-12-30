# PRD v1.0 多法人架构评估报告

| 日期 | 版本 | 作者 |
|------|------|------|
| 2025-12-30 | v1.0 | Claude Code |

---

## 执行摘要

**评估结论**: PRD 方案整体可行，可以开发，但需要修正几个技术问题。

```
整体可行性: ███████░░ 75%
```

---

## 一、必须修正的问题

### 1.1 分区语法错误

**位置**: PRD 1.4 节、4.1 节

**问题**:
```sql
-- PRD 中的写法（错误）
CREATE TABLE archive_object_e001 PARTITION OF archive_object
    FOR VALUES IN ('E001') PARTITION BY RANGE (archive_year);
```

PostgreSQL 的 LIST 分区不支持在子分区定义中直接嵌套 `PARTITION BY RANGE`。

**修正方案**:
```sql
-- 正确写法：先创建 LIST 分区表
CREATE TABLE archive_object (...) PARTITION BY LIST (entity_id);

-- 为每个法人创建分区模板
CREATE TABLE archive_object_FOR_E001 PARTITION OF archive_object
    FOR VALUES IN ('E001');

-- 然后手动为每个法人创建年度子分区
CREATE TABLE archive_object_E001_2024 PARTITION OF archive_object_FOR_E001
    FOR VALUES FROM (2024) TO (2025);
```

**推荐替代方案**: 初期使用复合索引替代分区表
```sql
CREATE INDEX idx_archive_entity_year_doc
ON archive_object (entity_id, archive_year, doc_type);
```

---

### 1.2 主键与分区键冲突

**位置**: PRD 4.1 节

**问题**:
- PRD 定义 `archive_object` 主键为 `id`
- 但复合分区要求主键必须包含所有分区键 `(id, entity_id, archive_year)`

**修正方案**:
```sql
-- 修正后的主键定义
PRIMARY KEY (id, entity_id, archive_year)

-- 所有外键引用也需同步调整
FOREIGN KEY (id, entity_id, archive_year)
    REFERENCES archive_object(id, entity_id, archive_year)
```

---

### 1.3 外键引用与现有表不匹配

**位置**: PRD 4.1 节、现有代码

**问题**:
- PRD 中 `borrow_record` 外键引用 `archive_object(id, entity_id, archive_year)`
- 现有 `acc_archive` 表主键只是 `id`
- 迁移时需要同步修改主键结构

**影响范围**: 所有包含 `archive_object_id` 外键的表

---

### 1.4 字段类型不一致

**位置**: PRD 1.2 节、现有 `acc_archive` 表

| 字段 | 现有类型 | PRD 要求 | 迁移风险 |
|------|----------|----------|----------|
| `archive_year` | `VARCHAR` (fiscal_year) | `INT` | 需类型转换 + 数据清洗 |
| `entity_id` | 不存在 | `VARCHAR(32)` | 新增字段，需回填数据 |

---

### 1.5 fonds_id 字段不存在

**位置**: 现有 `Archive.java` 实体类

```java
// 现有代码
@TableField(exist = false)
private String fondsId;  // 数据库实际没有这个列
```

**问题**: PRD 假设 `fonds_id` 已存在，但实际数据库中没有。

**修正方案**:
1. 新增数据库列 `fonds_id`
2. 或使用现有的 `fonds_no` 直接映射到 `entity_id`

---

## 二、架构合理性分析

### 2.1 合理的设计 ✅

| 设计点 | 评价 | 说明 |
|--------|------|------|
| **entity_id 隔离** | ✅ 合理 | 标准多租户方案，与现有 fonds 概念对齐 |
| **三员分立** | ✅ 合理 | 符合等保要求，现有 Role 系统可扩展 |
| **跨法人票据 (auth_ticket)** | ✅ 合理 | 审计追溯完整，双人复核满足合规 |
| **RLS + 应用层双过滤** | ✅ 合理 | 防御深度策略，降低单点失效风险 |
| **状态机销毁流程** | ✅ 合理 | 符合档案管理规范 |
| **实物管理** | ✅ 合理 | 装盒、借阅、盘点功能完整 |

---

### 2.2 过度设计的部分 ⚠️

| 设计点 | 问题 | 建议 |
|--------|------|------|
| **哈希链 + SM2 签名** | 增加复杂度和依赖，初期非必须 | 先用 SHA256，签名作为 P2 功能延后 |
| **复合分区表** | 单法人数据量不大时收益不明显 | 初期用复合索引，数据量增长后再分区 |
| **双人双控密钥托管** | 运维流程复杂，需要配套管理制度 | P2 阶段实施，P0/P1 用简单加密即可 |

---

## 三、与现有系统的兼容性

### 3.1 技术栈兼容性

| 组件 | 现有版本 | PRD 要求 | 兼容性 |
|------|----------|----------|--------|
| **Spring Boot** | 3.1.6 | 3.1.6 | ✅ 完全兼容 |
| **MyBatis-Plus** | 3.5.7 | 3.5.7 | ✅ 完全兼容 |
| **PostgreSQL** | 存量版本 | 分区语法 | ⚠️ 需确认版本 >= 10 |
| **JWT (jjwt)** | 0.12.3 | 0.12.3 | ✅ 完全兼容 |
| **前端 React** | 19 | 19 | ✅ 完全兼容 |
| **前端 Ant Design** | 6 | 6 | ✅ 完全兼容 |

---

### 3.2 数据结构改造工作量

| 表 | 改造内容 | 工作量 |
|---|----------|--------|
| **acc_archive** | 新增 `entity_id`，调整主键 | 中 |
| **sys_user** | 新增 `entity_id`，关联调整 | 小 |
| **sys_role** | 扩展三员分立约束 | 中 |
| **bas_fonds** | 同步到 `sys_entity` | 小 |
| **所有业务表** | 新增 `entity_id` 列 | 大 |

---

### 3.3 代码改造影响范围

| 模块 | 改造内容 | 风险 |
|------|----------|------|
| **JWT Token** | 增加 `allowed_entity_ids` 声明 | 低 |
| **Security Filter** | 解析实体上下文 | 低 |
| **MyBatis Mapper** | 所有查询增加 `entity_id` 条件 | 高 |
| **Controller** | 跨法人访问校验 | 中 |
| **前端** | 法人切换组件 | 低 |

---

## 四、开发可行性评估

### 4.1 分阶段实施建议

```
整体工作量估算: 12-17 周
```

| 阶段 | 内容 | 工作量 | 风险等级 |
|------|------|--------|----------|
| **P0: 核心隔离** | entity_id 隔离、MyBatis 拦截器、JWT 扩展、数据迁移 | 2-3 周 | 🟡 中 |
| **P1: 业务功能** | 实物装盒、借阅审批、盘点、检索脱敏 | 4-6 周 | 🟢 低 |
| **P2: 高级特性** | 分区表、RLS、销毁流程、哈希链、SM2 签名 | 6-8 周 | 🟠 高 |

---

### 4.2 技术验证项（PoC）

开发前建议先验证以下技术点：

| 验证项 | 验证内容 | 预期结果 |
|--------|----------|----------|
| **分区语法** | PostgreSQL LIST+RANGE 复合分区 | 确认正确语法 |
| **MyBatis 拦截器** | EntitySecurityInterceptor 自动注入 WHERE 条件 | 性能损耗 < 5% |
| **JWT 扩展** | Token 增加 `allowed_entity_ids` | 兼容现有登录流程 |
| **数据迁移** | entity_id 回填脚本 | 数据一致性校验通过 |

---

## 五、明确建议

### 5.1 必须做的事（开发前）

1. ✅ **修正分区语法** - 参考 1.1 节修正方案
2. ✅ **调整主键定义** - 包含分区键 `(id, entity_id, archive_year)`
3. ✅ **设计数据迁移方案** - 双写 + 灰度切换
4. ✅ **技术验证 PoC** - 验证拦截器和分区表性能
5. ✅ **保留 department_id** - 仅用于展示筛选，不参与权限判断

---

### 5.2 建议延后的功能（P2 阶段）

1. ⏸️ **哈希链 + SM2 签名** - 先用 SHA256
2. ⏸️ **分区表** - 初期用复合索引
3. ⏸️ **双人双控密钥托管** - 简化加密方案
4. ⏸️ **RLS 行级安全** - 应用层过滤已足够

---

### 5.3 与现有代码的对齐

| 现有概念 | PRD 对应 | 映射关系 |
|----------|----------|----------|
| `bas_fonds` | `sys_entity` | 1:1 同步，Fonds 作为 Entity 别名 |
| `fonds_no` | `entity_id` | 新增 `entity_id` 列，`fonds_no` 保留用于展示 |
| `fiscal_year` (VARCHAR) | `archive_year` (INT) | 数据迁移时类型转换 |
| `department_id` | N/A | 保留用于展示/筛选，不参与权限 |

---

## 六、风险评估与缓解

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 分区语法不兼容现有 PG 版本 | 中 | 高 | 升级 PostgreSQL 或改用索引优化 |
| 现有数据迁移量大 | 高 | 中 | 双写 + 异步迁移 + 灰度切换 |
| MyBatis 拦截器性能问题 | 中 | 中 | PoC 验证，必要时改用 AOP |
| 三员分立权限重构影响现有用户 | 低 | 高 | 新旧角色并行，逐步迁移 |

---

## 七、结论

**PRD v1.0 方案整体可行，可以开发。**

**前提条件**:
1. 修正分区语法和主键定义
2. 完成技术验证 PoC
3. 设计好数据迁移方案

**推荐实施路线**:
1. **Week 1-3**: P0 核心隔离 + 数据迁移
2. **Week 4-9**: P1 业务功能（实物装盒、借阅、盘点）
3. **Week 10-17**: P2 高级特性（分区、RLS、销毁、签名）

**关键里程碑**:
- Week 3: 跨法人查询返回空数据，不再泄露
- Week 9: P0/P1 功能可用，单法人场景完全跑通
- Week 17: 完整合规架构，通过等保审计
