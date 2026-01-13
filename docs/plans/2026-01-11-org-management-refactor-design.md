# 组织管理重构设计文档

**日期**: 2026-01-11
**版本**: 1.1
**状态**: 评审中 - 根据虚拟专家组反馈修订

---

## 1. 背景与问题

### 1.1 当前状态

系统中存在三个组织管理相关页面，功能重叠，用户困惑：

| 页面 | 路由 | 数据源 | 展示内容 | 问题 |
|------|------|--------|----------|------|
| 法人管理 | `/system/settings/org/entity` | `sys_entity` | 法人卡片列表 | 无法体现母子关系 |
| 子公司管理 | `/system/settings/org/company` | `sys_org` | 子公司树 + 列表 | 与法人概念重复 |
| 集团架构 | `/system/settings/org/architecture` | `sys_entity` + `bas_fonds` | 法人 → 全宗 → 档案 | 功能完整但定位不清 |

### 1.2 核心问题

1. **概念重复**：`sys_entity`（法人）和 `sys_org`（子公司）本质上都是"法人"概念
2. **数据分离**：两个表管理同一类实体，导致数据不一致风险
3. **用户困惑**：不清楚在哪个页面管理公司信息
4. **维护成本**：两套代码、两套 API、两套前端组件

### 1.3 业务需求

根据 DA/T 94-2022《电子会计档案管理规范》：
- **全宗**是同一法人单位形成的档案集合
- 一个法人可以有多个全宗（不同年度、不同业务线）
- 全宗应体现其所属法人信息
- 系统应支持组织机构的多级管理

---

## 2. 设计方案

### 2.1 核心原则

**单一数据源原则**：只使用 `sys_entity` 表管理所有法人实体（包括母公司和子公司），通过 `parent_id` 字段建立层级关系。

### 2.2 数据模型变更

#### 2.2.1 删除 `sys_org` 表

```sql
-- 迁移脚本：删除 sys_org 表
DROP TABLE IF EXISTS sys_org CASCADE;
```

#### 2.2.2 修改 `sys_entity` 表

```sql
-- 添加 parent_id 字段，建立法人层级关系
ALTER TABLE sys_entity ADD COLUMN IF NOT EXISTS parent_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_entity_parent_id ON sys_entity(parent_id);

-- 添加 order_num 字段，支持排序
ALTER TABLE sys_entity ADD COLUMN IF NOT EXISTS order_num INTEGER DEFAULT 0;

-- 添加注释
COMMENT ON COLUMN sys_entity.parent_id IS '父法人ID（用于集团层级：母公司-子公司）';
COMMENT ON COLUMN sys_entity.order_num IS '排序号';
```

#### 2.2.3 新数据模型

```
sys_entity (法人实体)
├── id                  -- 主键
├── name                -- 法人名称（必填）
├── tax_id              -- 统一社会信用代码
├── parent_id           -- 父法人ID（用于建立母子公司关系）
├── order_num           -- 排序号
├── address             -- 注册地址
├── contact_person      -- 联系人
├── contact_phone       -- 联系电话
├── contact_email       -- 联系邮箱
├── status              -- 状态：ACTIVE/INACTIVE
├── description         -- 描述
└── created_time / updated_time / deleted
```

### 2.3 页面设计

#### 2.3.1 最终页面结构

| 页面 | 路由 | 功能 | 定位 |
|------|------|------|------|
| 法人管理 | `/system/settings/org/entity` | 树形管理所有法人 | 维护视角 |
| 集团架构 | `/system/settings/org/architecture` | 法人 → 全宗 → 档案 | 业务统计视角 |
| 全宗管理 | `/system/settings/org/fonds` | 管理全宗 | 全宗CRUD |
| 岗位管理 | `/system/settings/org/positions` | 管理岗位 | 岗位CRUD |
| 全宗沿革 | `/system/settings/org/fonds-history` | 全宗历史记录 | 历史记录 |

#### 2.3.2 删除的页面

| 页面 | 路由 | 删除原因 |
|------|------|----------|
| 子公司管理 | `/system/settings/org/company` | 功能合并到法人管理 |

#### 2.3.3 法人管理页面设计

**布局**：左侧树形结构 + 右侧详情面板

```
┌─────────────────────────────────────────────────────────────┐
│  法人管理                                    [新建法人] [导入] │
├─────────────────┬───────────────────────────────────────────┤
│                 │                                           │
│  🏢 XX集团       │  📋 法人详情                              │
│    🏢 北京子公司 │  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│    🏢 上海子公司 │  名称: XX集团                             │
│       🏢 浦东分公司│  统一社会信用代码: 91XXXXXXXXXXXXXX     │
│                 │  状态: 活跃                               │
│  🏢 YY公司       │  下属法人: 2 个                           │
│                 │  关联全宗: 3 个                           │
│                 │                                           │
│                 │  [编辑] [删除] [添加子公司]               │
│                 │                                           │
│                 │  📊 下属法人列表                          │
│                 │  • 北京子公司 (91XXX...)                  │
│                 │  • 上海子公司 (91XXX...)                  │
│                 │     └─ 浦东分公司 (91XXX...)               │
└─────────────────┴───────────────────────────────────────────┘
```

**交互规则**：
- 点击树节点 → 右侧显示法人详情
- 拖拽节点 → 调整层级关系
- 展开/折叠 → 显示或隐藏下级法人

#### 2.3.4 集团架构页面保持不变

继续展示"法人 → 全宗 → 档案"三层结构，带统计信息。

---

## 3. 实施计划

### 3.1 后端改动

| 序号 | 任务 | 文件 | 工作量 |
|------|------|------|--------|
| 1 | 创建数据库迁移脚本（添加字段） | `Vxxx__sys_entity_add_parent_id.sql` | 小 |
| 2 | 创建数据库迁移脚本（删除表） | `Vxxx__drop_sys_org.sql` | 小 |
| 3 | 删除 `Org.java` 实体 | `entity/Org.java` | 小 |
| 4 | 删除 `OrgService.java` | `service/OrgService.java` | 小 |
| 5 | 删除 `OrgMapper.java` | `mapper/OrgMapper.java` | 小 |
| 6 | 删除 `AdminOrgController.java` | `controller/AdminOrgController.java` | 小 |
| 7 | 删除 `ErpOrgSyncService.java` | `service/ErpOrgSyncService.java` | 中 |
| 8 | 修改 `SysEntity.java` 添加 `parentId` 字段 | `entity/SysEntity.java` | 小 |
| 9 | 扩展 `EntityService` 支持树形操作 | `service/EntityService.java` | 中 |
| 10 | 修改 `EntityController` 添加树形 API | `controller/EntityController.java` | 中 |
| 11 | 更新 `EnterpriseArchitectureService` | `service/EnterpriseArchitectureService.java` | 小 |

### 3.2 前端改动

| 序号 | 任务 | 文件 | 工作量 |
|------|------|------|--------|
| 1 | 删除 `OrgSettings.tsx` 组件 | `components/settings/OrgSettings.tsx` | 小 |
| 2 | 删除 `OrgSettingsPage.tsx` | `pages/settings/OrgSettingsPage.tsx` | 小 |
| 3 | 修改 `OrgSettingsLayout.tsx` 移除子公司 Tab | `pages/settings/OrgSettingsLayout.tsx` | 小 |
| 4 | 重构 `EntityManagementPage.tsx` 为树形结构 | `pages/admin/EntityManagementPage.tsx` | 大 |
| 5 | 创建 `EntityTree.tsx` 树形组件 | `components/org/EntityTree.tsx` | 中 |
| 6 | 更新 API 调用 | `api/entity.ts` | 中 |
| 7 | 修改路由配置 | `routes/index.tsx`, `routes/paths.ts` | 小 |
| 8 | 更新类型定义 | `types/index.ts` | 小 |

### 3.3 实施顺序

**阶段 1：后端数据迁移**
1. 创建迁移脚本
2. 添加 `sys_entity.parent_id` 字段
3. 数据迁移（如有需要）
4. 删除 `sys_org` 表

**阶段 2：后端代码重构**
1. 删除 `sys_org` 相关代码
2. 扩展 `EntityService` 支持树形操作
3. 修改 API 接口
4. 运行测试验证

**阶段 3：前端重构**
1. 删除子公司管理页面
2. 更新 Tab 导航
3. 重构法人管理页面为树形结构
4. 运行测试验证

---

## 4. API 变更

### 4.1 删除的 API

```
DELETE /admin/org/tree
DELETE /admin/org
DELETE /admin/org/{id}
DELETE /admin/org/bulk
DELETE /admin/org/import
DELETE /admin/org/sync
DELETE /admin/org/{id}/order
DELETE /admin/org/import/template
```

### 4.2 新增/修改的 API

```
# 法人树形结构
GET /api/entity/tree
Response: {
  code: 200,
  data: [
    {
      id: "xxx",
      name: "XX集团",
      taxId: "91XXX...",
      children: [...]
    }
  ]
}

# 更新法人层级
PUT /api/entity/{id}/parent
Body: { parentId: "xxx" }

# 更新法人排序
PUT /api/entity/{id}/order
Body: { orderNum: 1 }
```

---

## 5. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 数据丢失 | 高 | 迁移前备份 `sys_org` 数据 |
| API 兼容性 | 中 | 前端同步修改，保持版本一致 |
| 用户习惯 | 低 | 提前沟通，新页面更直观 |

---

## 6. 测试计划

### 6.1 单元测试
- `EntityService` 树形操作测试
- 树形构建逻辑测试

### 6.2 集成测试
- API 端到端测试
- 前后端联调测试

### 6.3 用户验收测试
- 法人创建/编辑/删除
- 母子公司关系维护
- 树形拖拽排序
- 数据导入导出

---

## 7. 参考资料

- [DA/T 94-2022 电子会计档案管理规范](https://www.saac.gov.cn/daj/hybz/202206/c8e779ca63234607bb88186293a7ff95.shtml)
- [企业档案管理规定](https://www.moj.gov.cn/pub/sfbgw/flfggz/flfggzbmgz/202401/t20240108_493038.html)

---

## 附录 A：代码变更清单

### 后端删除文件清单
```
nexusarchive-java/src/main/java/com/nexusarchive/entity/Org.java
nexusarchive-java/src/main/java/com/nexusarchive/service/OrgService.java
nexusarchive-java/src/main/java/com/nexusarchive/mapper/OrgMapper.java
nexusarchive-java/src/main/java/com/nexusarchive/controller/AdminOrgController.java
nexusarchive-java/src/main/java/com/nexusarchive/service/ErpOrgSyncService.java
```

### 前端删除文件清单
```
src/components/settings/OrgSettings.tsx
src/pages/settings/OrgSettingsPage.tsx
```

### 前端修改文件清单
```
src/pages/settings/OrgSettingsLayout.tsx
src/pages/admin/EntityManagementPage.tsx
src/routes/index.tsx
src/routes/paths.ts
src/api/entity.ts
src/types/index.ts
```

---

## 附录 B：虚拟专家组评审反馈

### B.1 评审结论

**决议**: ⚠️ 有条件通过 - 需补充以下材料后进入实施

### B.2 外键依赖处理方案

#### B.2.1 sys_erp_config.org_id 分析

经过代码审查，`ErpConfig.orgId` 字段**未被任何业务代码使用**：

```java
// 搜索结果：无任何代码调用 erpConfig.getOrgId() 或 erpConfig.orgId
```

**处理方案**：直接删除该字段

```sql
-- 迁移脚本：删除 sys_erp_config.org_id 字段
ALTER TABLE sys_erp_config DROP COLUMN IF EXISTS org_id;
```

**影响评估**：
- 无业务代码依赖
- 仅在 `ErpConfig` 实体中声明，未实际使用
- 删除安全

#### B.2.2 外键依赖检查

| 表名 | 外键字段 | 引用表 | 处理方式 |
|------|---------|--------|----------|
| `sys_erp_config` | `org_id` | `sys_org` | 直接删除（未使用） |
| `bas_fonds` | `entity_id` | `sys_entity` | 不变，保持现有关系 |

### B.3 数据迁移脚本

#### B.3.1 迁移策略

将 `sys_org` 表中的有效数据迁移到 `sys_entity` 表：

```sql
-- ================================================================
-- Migration: V101__migrate_sys_org_to_sys_entity.sql
-- Purpose: 将 sys_org 数据迁移到 sys_entity
-- Author: System
-- Date: 2026-01-11
-- ================================================================

-- 1. 备份 sys_org 数据（可选，建议手动执行前备份）
-- pg_dump -t sys_org > backup_sys_org_$(date +%Y%m%d).sql

-- 2. 添加 parent_id 和 order_num 字段到 sys_entity
ALTER TABLE sys_entity ADD COLUMN IF NOT EXISTS parent_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_entity_parent_id ON sys_entity(parent_id);
ALTER TABLE sys_entity ADD COLUMN IF NOT EXISTS order_num INTEGER DEFAULT 0;

-- 添加注释
COMMENT ON COLUMN sys_entity.parent_id IS '父法人ID（用于集团层级：母公司-子公司）';
COMMENT ON COLUMN sys_entity.order_num IS '排序号';

-- 3. 迁移 sys_org 数据到 sys_entity
-- 使用 INSERT ... ON CONFLICT 避免主键冲突
INSERT INTO sys_entity (
    id,
    name,
    parent_id,
    order_num,
    status,
    description,
    created_time,
    updated_time,
    deleted
)
SELECT
    id,
    name,
    parent_id,
    order_num,
    'ACTIVE'::VARCHAR(20) AS status,
    name || '（从组织迁移）' AS description,
    created_time,
    updated_time,
    deleted
FROM sys_org
WHERE deleted = 0
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    order_num = EXCLUDED.order_num;

-- 4. 记录迁移结果到审计日志
INSERT INTO sys_audit_log (
    operation_type,
    table_name,
    record_id,
    data_before,
    data_after,
    operator_id,
    ip_address
)
SELECT
    'MIGRATION'::VARCHAR(50),
    'sys_entity'::VARCHAR(100),
    id,
    'sys_org:' || name,
    'Migrated from sys_org',
    'SYSTEM',
    '127.0.0.1'
FROM sys_org
WHERE deleted = 0;
```

### B.4 回滚脚本

#### B.4.1 回滚方案

如果迁移失败，可以使用以下脚本恢复：

```sql
-- ================================================================
-- Rollback: V101__rollback_sys_org_migration.sql
-- Purpose: 回滚 sys_org 到 sys_entity 的迁移
-- Author: System
-- Date: 2026-01-11
-- ================================================================

-- ⚠️ 警告：执行此脚本前请确保有完整的数据备份！

-- 1. 删除从 sys_org 迁移过来的数据
-- 注意：这会删除所有 parent_id 不为空且创建时间在迁移期间的记录
DELETE FROM sys_entity
WHERE parent_id IS NOT NULL
  AND id IN (SELECT id FROM sys_org WHERE deleted = 0);

-- 2. 删除添加的字段
ALTER TABLE sys_entity DROP COLUMN IF EXISTS parent_id;
ALTER TABLE sys_entity DROP COLUMN IF EXISTS order_num;

-- 3. 恢复 sys_org 表（如果已被删除）
-- 如果表未被删除，此步骤可跳过
-- CREATE TABLE sys_org (...); -- 从备份恢复 DDL
```

#### B.4.2 回滚前提条件

1. **必须有完整的数据备份**
2. **确认迁移后未进行其他数据操作**
3. **获得系统管理员批准**

### B.5 升级指南

#### B.5.1 升级前检查清单

- [ ] 确认 `sys_org` 表数据量
- [ ] 执行数据备份：`pg_dump -t sys_org > backup_sys_org_YYYYMMDD.sql`
- [ ] 确认当前版本：`SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;`
- [ ] 通知用户系统维护时间

#### B.5.2 升级步骤

1. **停止应用服务**
   ```bash
   # 停止 Java 后端
   systemctl stop nexusarchive-backend

   # 停止前端（如需要）
   systemctl stop nexusarchive-frontend
   ```

2. **执行数据库迁移**
   ```bash
   # Flyway 会自动执行迁移脚本
   cd nexusarchive-java
   mvn flyway:migrate
   ```

3. **验证迁移结果**
   ```sql
   -- 检查 sys_entity 是否有 parent_id 字段
   \d sys_entity

   -- 检查数据是否正确迁移
   SELECT COUNT(*) FROM sys_entity WHERE parent_id IS NOT NULL;

   -- 检查 sys_org 表是否已删除
   SELECT COUNT(*) FROM sys_org;  -- 应该报错 "表不存在"
   ```

4. **启动应用服务**
   ```bash
   systemctl start nexusarchive-backend
   systemctl start nexusarchive-frontend
   ```

5. **功能验证**
   - 登录系统，进入"法人管理"页面
   - 验证树形结构正常显示
   - 验证法人增删改功能正常

#### B.5.3 回滚步骤

如果升级失败：

1. **停止应用服务**
2. **执行回滚脚本**（见 B.4.1）
3. **恢复备份**（如需要）
   ```bash
   psql -U postgres -d nexusarchive < backup_sys_org_YYYYMMDD.sql
   ```
4. **启动应用服务**
5. **通知相关人员**

### B.6 审计日志增强

#### B.6.1 层级变更审计

为确保法人层级关系的变更可追溯，需要在修改 `parent_id` 时记录审计日志：

```java
/**
 * 更新法人父节点（带审计）
 */
@ArchivalAudit(operationType = "ENTITY_PARENT_UPDATE",
                tableType = "SYS_ENTITY")
public void updateParent(String entityId, String parentId) {
    SysEntity entity = getById(entityId);

    // 记录变更前状态
    String beforeData = String.format("{\"id\":\"%s\",\"name\":\"%s\",\"parentId\":\"%s\"}",
        entity.getId(), entity.getName(), entity.getParentId());

    // 执行更新
    entity.setParentId(parentId);
    updateById(entity);

    // 审计日志由 @ArchivalAudit AOP 自动记录
}
```

#### B.6.2 审计日志字段

| 字段 | 说明 | 示例 |
|------|------|------|
| operation_type | 操作类型 | ENTITY_PARENT_UPDATE |
| table_name | 表名 | sys_entity |
| record_id | 记录ID | 法人ID |
| data_before | 变更前数据 | {"id":"xxx","parentId":"yyy"} |
| data_after | 变更后数据 | {"id":"xxx","parentId":"zzz"} |

### B.7 风险缓解措施更新

| 风险 | 影响 | 缓解措施 | 状态 |
|------|------|----------|------|
| 数据丢失 | 高 | 迁移前备份，提供回滚脚本 | ✅ 已补充 |
| API 兼容性 | 中 | 前端同步修改，保持版本一致 | ✅ 已确认 |
| 外键依赖 | 高 | org_id 未被使用，直接删除 | ✅ 已解决 |
| 用户习惯 | 低 | 提前沟通，新页面更直观 | ✅ 已规划 |
| 层级变更审计 | 中 | 使用 @ArchivalAudit 注解 | ✅ 已补充 |

---

## 附录 C：专家评审决议

| 专家 | 建议 | 状态 |
|------|------|------|
| 合规专家 | 设计合理，无合规阻断。建议增强层级变更审计日志。 | ✅ 已采纳 |
| 架构专家 | 单一数据源原则正确。必须处理外键依赖。 | ✅ 已解决 |
| 交付专家 | 补充回滚方案和升级指南。 | ✅ 已补充 |

**最终决议**: ✅ 通过 - 可进入实施阶段
