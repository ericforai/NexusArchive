# Query User Borrowing Design (查询用户借阅功能设计)

> **设计日期:** 2026-01-10
> **目标:** 为业务操作员细分出"查询用户"角色，仅提供档案借阅（只读）权限
> **原则:** 最小权限原则 - 只能看到自己申请借阅的档案

---

## 1. 角色定义

### 1.1 新增角色

在现有角色体系基础上，新增 `query_user` 角色：

| 角色代码 | 角色名称 | 说明 |
|---------|---------|------|
| `query_user` | 查询用户 | 只读权限，仅能通过借阅审批访问特定档案 |

### 1.2 权限特征

| 操作 | 权限 | 说明 |
|------|------|------|
| 浏览档案目录 | ✅ | 可查看全宗内档案元数据 |
| 提交借阅申请 | ✅ | 可发起借阅申请 |
| 查看已批准档案 | ✅ | 仅限审批通过且未过期的档案 |
| 创建/编辑档案 | ❌ | 无操作权限 |
| 审批/导出/打印 | ❌ | 除非借阅时特别授权 |

---

## 2. 数据库设计

### 2.1 借阅记录表 (arc_borrow)

```sql
CREATE TABLE arc_borrow (
    id BIGSERIAL PRIMARY KEY,
    borrow_no VARCHAR(50) UNIQUE NOT NULL,        -- 借阅单号 BLYYYYMMDD0001
    fonds_id BIGINT NOT NULL,                     -- 全宗ID
    user_id BIGINT NOT NULL,                      -- 借阅用户ID

    title VARCHAR(200) NOT NULL,                  -- 借阅标题
    purpose TEXT NOT NULL,                        -- 借阅用途（≥10字符）

    expire_days INTEGER DEFAULT 7,                -- 借阅有效期（天）
    expire_at TIMESTAMP NOT NULL,                 -- 到期时间

    status VARCHAR(20) DEFAULT 'PENDING',          -- 状态
    -- PENDING: 待审批
    -- APPROVED: 已批准
    -- REJECTED: 已拒绝
    -- EXPIRED: 已过期
    -- RETURNED: 已归还

    approved_by BIGINT,                           -- 审批人ID
    approved_at TIMESTAMP,                        -- 审批时间
    approval_remark TEXT,                         -- 审批备注

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_borrow_fonds FOREIGN KEY (fonds_id) REFERENCES acc_fonds(id),
    CONSTRAINT fk_borrow_approver FOREIGN KEY (approved_by) REFERENCES sys_user(id)
);

CREATE INDEX idx_borrow_user ON arc_borrow(user_id);
CREATE INDEX idx_borrow_status ON arc_borrow(status);
CREATE INDEX idx_borrow_expire ON arc_borrow(expire_at);
```

### 2.2 借阅档案明细表 (arc_borrow_item)

```sql
CREATE TABLE arc_borrow_item (
    id BIGSERIAL PRIMARY KEY,
    borrow_id BIGINT NOT NULL,                    -- 借阅记录ID
    archive_id BIGINT NOT NULL,                   -- 档案ID
    granted_permissions VARCHAR(100) DEFAULT 'VIEW', -- 授予权限
    -- VIEW: 仅查看
    -- DOWNLOAD: 允许下载
    -- PRINT: 允许打印

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_borrow_item_borrow FOREIGN KEY (borrow_id) REFERENCES arc_borrow(id) ON DELETE CASCADE,
    CONSTRAINT fk_borrow_item_archive FOREIGN KEY (archive_id) REFERENCES acc_archive(id),
    UNIQUE(borrow_id, archive_id)
);

CREATE INDEX idx_borrow_item_archive ON arc_borrow_item(archive_id);
```

---

## 3. 业务流程

### 3.1 完整流程图

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ 查询用户    │ ──▶ │ 档案管理员  │ ──▶ │ 系统定时任务│ ──▶ │ 自动到期    │
│             │     │             │     │             │     │             │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
     │                    │                     │                    │
     ▼                    ▼                     ▼                    ▼
 1. 选择档案          2. 审批申请           4. 到期前3天提醒      6. 自动收回
 2. 填写用途          - 批准:设置权限       5. 到期后失效         访问权限
 3. 提交申请          - 拒绝:填写原因       - 状态=EXPIRED      - 不可再访问
 4. 等待审批          - 可修改权限粒度
```

### 3.2 状态流转表

| 当前状态 | 可流转至 | 触发条件 |
|---------|---------|---------|
| PENDING | APPROVED | 管理员审批通过 |
| PENDING | REJECTED | 管理员审批拒绝 |
| APPROVED | EXPIRED | 定时任务检测到期 |
| APPROVED | RETURNED | 管理员手动提前归还 |
| EXPIRED | APPROVED | 用户申请延期并获批 |

### 3.3 业务规则

**提交申请时校验：**
- 单次借阅档案数量 ≤ 10 份
- 用户当前持有借阅数 + 本次申请 ≤ 20 份
- 借阅用途必填，长度 ≥ 10 字符
- 档案状态必须为 `ARCHIVED`

**审批时校验：**
- 管理员必须拥有对应全宗权限
- 可调整单个档案的权限粒度（仅查看/允许下载/允许打印）

**访问档案时校验：**
- 查询用户只能访问 `APPROVED` 状态且未过期的借阅记录中的档案
- 通过 `BorrowPermissionService` 统一检查权限

---

## 4. 配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| borrow.default.expire.days | 7 | 借阅有效期（天） |
| borrow.limit.single | 10 | 单次借阅数量上限 |
| borrow.limit.concurrent | 20 | 同时持有借阅上限 |
| borrow.reminder.days | 3 | 到期前提醒天数 |

---

## 5. API设计

### 5.1 借阅管理端点

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/borrow/apply` | 提交借阅申请 | `query_user` |
| GET | `/api/borrow/my` | 我的借阅记录 | `query_user` |
| GET | `/api/borrow/{id}` | 借阅详情 | 本人 |
| POST | `/api/borrow/{id}/extend` | 申请延期 | 本人 |
| GET | `/api/borrow/pending` | 待审批列表 | 审批权 |
| POST | `/api/borrow/{id}/approve` | 审批通过 | 审批权 |
| POST | `/api/borrow/{id}/reject` | 审批拒绝 | 审批权 |
| POST | `/api/borrow/{id}/revert` | 提前归还 | 审批权 |

### 5.2 权限拦截器

新增 `BorrowPermissionInterceptor`，在档案访问时注入借阅检查：

```java
if (currentUser.hasRole("query_user")) {
    boolean hasPermission = borrowService.checkAccess(
        userId, archiveId, "VIEW"
    );
    if (!hasPermission) {
        throw new AccessDeniedException("无权访问该档案，请先提交借阅申请");
    }
}
```

---

## 6. 前端设计

### 6.1 新增页面

**档案借阅管理页面** (`src/pages/archive/BorrowManagement.tsx`)

- **查询用户视图**：
  - 我的借阅（列表展示）
  - 申请借阅（弹窗选择档案）
  - 延期申请

- **管理员视图**：
  - 待审批列表
  - 借阅记录管理
  - 到期提醒

### 6.2 新增组件

| 组件名 | 功能 |
|--------|------|
| `BorrowApplicationDialog` | 借阅申请弹窗（选择档案、填写用途） |
| `BorrowApprovalDrawer` | 审批抽屉（查看详情、设置权限） |
| `BorrowStatusTag` | 状态标签组件 |
| `BorrowCountWarning` | 数量超限提示 |

---

## 7. 安全与审计

### 7.1 审计日志

所有借阅操作必须记录审计日志：

| 操作 | 记录内容 |
|------|---------|
| 提交申请 | 申请人、档案清单、用途 |
| 审批操作 | 审批人、结果、备注 |
| 档案访问 | 访问时间、档案ID、操作类型 |
| 到期/归还 | 状态变更时间 |

### 7.2 安全防护

- **越权防护**：借阅记录的 `user_id` 必须匹配当前登录用户
- **全宗隔离**：查询用户只能申请借阅其拥有全宗权限的档案
- **过期检查**：每次访问档案时检查 `expire_at > NOW()`
- **数据脱敏**：查询用户查看审计日志时脱敏他人信息

### 7.3 定时任务

新增 `BorrowExpirationScheduler`：
- 每小时执行一次
- 将过期借阅状态变更为 `EXPIRED`
- 到期前3天发送通知

---

## 8. 测试策略

### 8.1 单元测试

- `BorrowServiceTest` - 业务规则验证
- `BorrowPermissionInterceptorTest` - 权限拦截测试

### 8.2 集成测试

- 完整借阅流程测试
- 边界条件测试

### 8.3 E2E测试

- 查询用户提交借阅申请
- 管理员审批借阅
- 查询用户访问获批档案
- 到期自动失效

---

## 9. 实施步骤

1. **数据库迁移** - 创建 `arc_borrow` 和 `arc_borrow_item` 表
2. **角色权限配置** - 添加 `query_user` 角色和权限定义
3. **后端服务实现** - Service、Controller、Interceptor
4. **定时任务** - 到期检查和提醒
5. **前端页面开发** - 借阅管理页面和组件
6. **测试验证** - 单元测试、集成测试、E2E测试
