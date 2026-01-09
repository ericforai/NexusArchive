# NexusArchive API 文档

> **版本**: v2.0.0
> **更新日期**: 2026-01-09
> **基础路径**: `http://localhost:19090/api`

---

## 目录

1. [概述](#概述)
2. [认证方式](#认证方式)
3. [通用参数](#通用参数)
4. [响应格式](#响应格式)
5. [API 分组](#api-分组)
6. [错误码](#错误码)
7. [Swagger UI](#swagger-ui)

---

## 概述

NexusArchive API 提供电子会计档案管理系统的完整 RESTful 接口，符合 **DA/T 94-2022**《电子会计档案管理规范》。

### 主要功能模块

| 模块 | 描述 |
|------|------|
| 档案管理 | 电子会计档案的创建、查询、更新、删除 |
| 归档审批 | 档案归档的审批流程管理 |
| 四性检测 | 真实性、完整性、可用性、安全性检测 |
| ERP 集成 | 用友 YonSuite 等第三方 ERP 系统数据同步 |
| 全宗管理 | 多全宗体系下的数据隔离与管理 |
| 三员分立 | 系统管理员、安全保密员、安全审计员权限分离 |
| 审计日志 | 基于 SM3 哈希链的防篡改审计日志 |
| 批量操作 | 归档审批、批次管理、销毁申请的批量处理 |

---

## 认证方式

### JWT Bearer Token

所有 API 请求需要在 HTTP Header 中携带 JWT Token：

```http
Authorization: Bearer <your-jwt-token>
```

### 获取 Token

通过登录接口获取 Token：

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJSUzI1NiJ9...",
    "expiresIn": 86400
  }
}
```

---

## 通用参数

### 分页参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码，从 1 开始 |
| limit | int | 10 | 每页条数，最大 100 |

### 全宗上下文

所有数据查询接口会自动根据用户当前的全宗上下文过滤数据。

---

## 响应格式

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    // 响应数据
  }
}
```

### 错误响应

```json
{
  "code": 400,
  "message": "参数错误",
  "data": null
}
```

### 分页响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

---

## API 分组

### 01. 档案管理 (Archive Management)

**基础路径**: `/archives`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/archives` | 分页查询档案 | `archive:read` |
| GET | `/archives/{id}` | 获取档案详情 | `archive:read` |
| GET | `/archives/{id}/files` | 获取档案关联文件 | `archive:read` |
| GET | `/archives/recent` | 获取最近档案 | `archive:read` |
| POST | `/archives` | 创建档案 | `archive:manage` |
| PUT | `/archives/{id}` | 更新档案 | `archive:manage` |
| DELETE | `/archives/{id}` | 删除档案 | `archive:manage` |

#### 示例：分页查询档案

```http
GET /api/archives?page=1&limit=10&search=2024年度
Authorization: Bearer <token>
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": "123456",
        "archiveCode": "COMP001-2024-10Y-FIN-AC01-V9900",
        "title": "2024年度会计凭证",
        "amount": 10000.00,
        "docDate": "2024-01-01",
        "status": "ARCHIVED",
        "fondsNo": "COMP001"
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

---

### 02. 归档审批 (Archive Approval)

**基础路径**: `/approval`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/approval/pending` | 获取待审批列表 | `archive:manage` |
| POST | `/approval/{id}/approve` | 批准归档 | `archive:manage` |
| POST | `/approval/{id}/reject` | 拒绝归档 | `archive:manage` |
| POST | `/approval/batch` | 批量审批 | `archive:manage` |

---

### 03. 四性检测 (Four-Nature Check)

**基础路径**: `/compliance`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/compliance/archives/{archiveId}` | 检查档案符合性 | `archive:read` |
| POST | `/compliance/archives/batch` | 批量检查符合性 | `archive:read` |
| POST | `/compliance/four-nature/{archiveId}/async` | 提交异步四性检测 | `archive:read` |
| GET | `/compliance/four-nature/tasks/{taskId}` | 查询检测任务状态 | `archive:read` |
| GET | `/compliance/four-nature/tasks/{taskId}/result` | 获取检测结果 | `archive:read` |
| DELETE | `/compliance/four-nature/tasks/{taskId}` | 取消检测任务 | `archive:read` |

#### 示例：异步四性检测

```http
POST /api/compliance/four-nature/123456/async
Authorization: Bearer <token>
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task-uuid-123",
    "archiveId": "123456",
    "archiveCode": "COMP001-2024-10Y-FIN-AC01-V9900"
  }
}
```

#### 查询检测任务状态

```http
GET /api/compliance/four-nature/tasks/task-uuid-123
Authorization: Bearer <token>
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task-uuid-123",
    "status": "RUNNING",
    "progress": 50,
    "startedAt": "2026-01-09T10:00:00",
    "estimatedCompletionAt": "2026-01-09T10:02:00"
  }
}
```

---

### 04. ERP 集成 (ERP Integration)

**基础路径**: `/erp`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/erp/scenario/list/{configId}` | 获取场景列表 | SYSTEM_ADMIN |
| PUT | `/erp/scenario` | 更新场景配置 | SYSTEM_ADMIN |
| POST | `/erp/scenario/{id}/sync` | 手动触发同步 | SYSTEM_ADMIN |
| GET | `/erp/scenario/{id}/sync/status/{taskId}` | 查询同步状态 | SYSTEM_ADMIN |
| GET | `/erp/scenario/{id}/sync/tasks` | 获取同步任务列表 | SYSTEM_ADMIN |
| GET | `/erp/scenario/channels` | 获取所有集成通道 | SYSTEM_ADMIN |

#### 示例：手动触发 ERP 同步

```http
POST /api/erp/scenario/1/sync
Authorization: Bearer <token>
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "sync-task-123",
    "scenarioId": 1,
    "status": "SUBMITTED",
    "submittedAt": "2026-01-09T10:00:00"
  }
}
```

---

### 05. 全宗管理 (Fonds Management)

**基础路径**: `/fonds`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/fonds` | 获取全宗列表 | `nav:all` 或特定权限 |
| GET | `/fonds/{id}` | 获取全宗详情 | `nav:all` 或特定权限 |
| POST | `/fonds` | 创建全宗 | SYSTEM_ADMIN |
| PUT | `/fonds/{id}` | 更新全宗 | SYSTEM_ADMIN |
| DELETE | `/fonds/{id}` | 删除全宗 | SYSTEM_ADMIN |

---

### 06. 用户权限 (User & Auth)

**基础路径**: `/auth`, `/users`, `/roles`, `/permissions`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/auth/login` | 用户登录 | 公开 |
| POST | `/auth/logout` | 用户登出 | 已认证 |
| GET | `/users` | 获取用户列表 | SYSTEM_ADMIN |
| POST | `/users` | 创建用户 | SECURITY_ADMIN |
| PUT | `/users/{id}` | 更新用户 | SECURITY_ADMIN |
| DELETE | `/users/{id}` | 删除用户 | SECURITY_ADMIN |
| GET | `/roles` | 获取角色列表 | SYSTEM_ADMIN |
| POST | `/roles` | 创建角色 | SECURITY_ADMIN |

---

### 07. 系统管理 (System Management)

**基础路径**: `/admin`, `/system`, `/monitoring`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/system/config` | 获取系统配置 | SYSTEM_ADMIN |
| PUT | `/system/config` | 更新系统配置 | SYSTEM_ADMIN |
| GET | `/monitoring/integration` | 获取集成监控指标 | SYSTEM_ADMIN |
| GET | `/admin/async/thread-pools` | 获取线程池状态 | SYSTEM_ADMIN |
| GET | `/admin/async/health` | 获取线程池健康状态 | SYSTEM_ADMIN |
| GET | `/system/metrics/report` | 获取性能指标报告 | system:monitor |

#### 示例：获取线程池状态

```http
GET /api/admin/async/thread-pools
Authorization: Bearer <token>
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskExecutor": {
      "activeCount": 2,
      "corePoolSize": 4,
      "maximumPoolSize": 8,
      "queueSize": 5,
      "completedTaskCount": 100
    },
    "erpSyncExecutor": {
      "activeCount": 1,
      "corePoolSize": 2,
      "maximumPoolSize": 4,
      "queueSize": 0,
      "completedTaskCount": 50
    }
  }
}
```

---

### 08. 审计日志 (Audit Log)

**基础路径**: `/audit`, `/logs`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/audit/logs` | 获取审计日志 | AUDIT_ADMIN |
| GET | `/audit/logs/{id}` | 获取日志详情 | AUDIT_ADMIN |
| GET | `/audit/verify` | 验证日志链完整性 | AUDIT_ADMIN |

---

### 09. 搜索 (Search)

**基础路径**: `/search`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/search?q={keyword}` | 全局搜索 | `archive:read` |

#### 示例：全局搜索

```http
GET /api/search?q=2024年度
Authorization: Bearer <token>
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "type": "ARCHIVE",
      "id": "123456",
      "title": "2024年度会计凭证",
      "summary": "凭证号: 2024-001"
    },
    {
      "type": "FONDS",
      "id": "F001",
      "title": "COMP001 - 2024年度全宗",
      "summary": "全宗号: COMP001"
    }
  ]
}
```

---

### 10. 异步任务 (Async Tasks)

**基础路径**: `/async`, `/tasks`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/async/tasks` | 获取异步任务列表 | SYSTEM_ADMIN |
| GET | `/async/tasks/{id}` | 获取任务详情 | SYSTEM_ADMIN |
| DELETE | `/async/tasks/{id}` | 取消任务 | SYSTEM_ADMIN |

---

### 11. 销毁管理 (Destruction Management)

**基础路径**: `/destruction`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/destruction/requests` | 获取销毁申请列表 | `archive:manage` |
| POST | `/destruction/requests` | 创建销毁申请 | `archive:manage` |
| POST | `/destruction/requests/{id}/approve` | 批准销毁 | SYSTEM_ADMIN |
| POST | `/destruction/requests/{id}/reject` | 拒绝销毁 | SYSTEM_ADMIN |

---

### 12. 扫描集成 (Scan Integration)

**基础路径**: `/scan`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/scan/workspace` | 获取扫描工作区 | `archive:manage` |
| POST | `/scan/ocr` | OCR 识别 | `archive:manage` |
| POST | `/scan/folder-monitor` | 启动文件夹监控 | SYSTEM_ADMIN |

---

### 13. 其他接口 (Other APIs)

**基础路径**: `/health`, `/license`, `/ticket`, `/notification`, 等

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/health` | 健康检查 | 公开 |
| GET | `/license/info` | 获取 License 信息 | 公开 |
| POST | `/ticket/verify` | 验证操作票据 | 已认证 |
| GET | `/notification/list` | 获取通知列表 | 已认证 |

---

## 错误码

### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未授权 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

### 业务错误码

| 错误码 | 说明 |
|--------|------|
| 1001 | 用户名或密码错误 |
| 1002 | Token 过期 |
| 1003 | Token 无效 |
| 2001 | 档案不存在 |
| 2002 | 档案已删除 |
| 3001 | 全宗不存在 |
| 3002 | 用户无全宗权限 |
| 4001 | ERP 连接失败 |
| 4002 | ERP 同步失败 |
| 5001 | 文件上传失败 |
| 5002 | 文件格式不支持 |
| 6001 | 审计日志验证失败 |
| 7001 | License 无效 |
| 7002 | License 已过期 |

---

## Swagger UI

### 访问地址

- **本地开发**: http://localhost:19090/api/swagger-ui.html
- **开发环境**: https://api-dev.nexusarchive.com/api/swagger-ui.html
- **生产环境**: https://api.nexusarchive.com/api/swagger-ui.html

### 使用说明

1. 打开 Swagger UI 页面
2. 点击右上角 "Authorize" 按钮
3. 输入 JWT Token（格式: `Bearer <token>`）
4. 选择要测试的 API 接口
5. 点击 "Try it out" 进行测试

---

## 更新日志

### v2.0.0 (2026-01-09)

- 新增异步四性检测接口
- 新增 ERP 同步状态查询接口
- 新增全局搜索接口
- 新增系统监控接口
- 新增异步任务监控接口
- 优化分页接口性能
- 完善 API 文档注解

### v1.0.0 (2025-12-01)

- 初始版本
- 基础 CRUD 接口
- 归档审批流程
- 审计日志功能
