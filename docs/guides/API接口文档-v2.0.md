# NexusArchive API 接口文档 v2.0

> 本文档描述 NexusArchive 电子会计档案管理系统的 REST API 接口。
> **版本**: 2.0.0
> **更新日期**: 2026-03-13
> **基础路径**: `/` (无统一前缀，各模块直接使用模块名)

---

## 认证方式

所有 API 请求需要在请求头中携带有效的 JWT Token：

```
Authorization: Bearer <token>
```

启用 MFA 的用户登录时需要额外提供 TOTP 验证码。

---

## 通用响应格式

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 错误响应

```json
{
  "code": 400,
  "message": "错误描述",
  "data": null
}
```

---

## 1. 认证与授权 (Authentication & Authorization)

### 1.1 用户登录

```
POST /api/auth/login
```

**请求体**:
```json
{
  "username": "string",
  "password": "string",
  "totpCode": "string"  // MFA 启用时必需
}
```

**响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "jwt_token_string",
    "user": {
      "id": 1,
      "username": "admin",
      "realName": "管理员",
      "mfaEnabled": true
    }
  }
}
```

### 1.2 用户登出

```
POST /api/auth/logout
```

### 1.3 刷新 Token

```
POST /api/auth/refresh
```

**请求体**:
```json
{
  "refreshToken": "string"
}
```

---

## 2. MFA 双因素认证 (Multi-Factor Authentication)

### 2.1 查询 MFA 状态

```
GET /mfa/status
```

**响应**:
```json
{
  "code": 200,
  "data": true
}
```

### 2.2 生成 MFA 注册密钥

```
POST /mfa/setup
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "secret": "JBSWY3DPEHPK3PXP",
    "qrCodeUrl": "otpauth://totp/..."
  }
}
```

### 2.3 启用 MFA

```
POST /mfa/enable
```

**请求体**:
```json
{
  "totpCode": "123456"
}
```

### 2.4 禁用 MFA

```
POST /mfa/disable
```

### 2.5 验证 TOTP

```
POST /mfa/verify
```

**请求体**:
```json
{
  "totpCode": "123456"
}
```

### 2.6 验证备用恢复码

```
POST /mfa/verify-backup
```

**请求体**:
```json
{
  "backupCode": "abcd-efgh-1234-5678"
}
```

### 2.7 生成备用恢复码

```
POST /mfa/backup-codes
```

---

## 3. 档案冻结/保全 (Archive Freeze & Hold)

### 3.1 冻结档案

```
POST /archive/freeze/apply
```

**请求体**:
```json
{
  "archiveIds": [1, 2, 3],
  "reason": "审计期间冻结",
  "freezeType": "AUDIT",
  "expectedUnfreezeDate": "2026-12-31"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "successCount": 3,
    "failedCount": 0
  }
}
```

### 3.2 解除冻结

```
POST /archive/freeze/{id}/release
```

**请求体**:
```json
{
  "reason": "审计结束"
}
```

### 3.3 查询档案冻结状态

```
GET /archive/freeze/check/{id}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "frozen": true,
    "freezeReason": "审计期间冻结",
    "freezeDate": "2026-01-01T00:00:00",
    "expectedUnfreezeDate": "2026-12-31T00:00:00"
  }
}
```

---

## 4. 鉴定清单管理 (Appraisal List Management)

### 4.1 生成鉴定清单

```
POST /archive/appraisal/generate
```

**请求体**:
```json
{
  "year": 2025,
  "retentionPeriod": "10年"
}
```

### 4.2 分页查询鉴定清单

```
GET /archive/appraisal/list?page=1&size=20
```

**查询参数**:
- `status`: DRAFT, PENDING, APPROVED, EXECUTED

### 4.3 获取鉴定清单详情

```
GET /archive/appraisal/{id}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "listName": "2025年度到期档案鉴定",
    "status": "PENDING",
    "archives": [...]
  }
}
```

### 4.4 提交鉴定结论

```
POST /archive/appraisal/{id}/conclusion
```

**请求体**:
```json
{
  "conclusion": "DESTROY",
  "note": "保管期限已满，无继续保存价值"
}
```

### 4.5 导出鉴定清单

```
GET /archive/appraisal/{id}/export
```

---

## 5. 用户生命周期管理 (User Lifecycle Management)

### 5.1 员工入职

```
POST /user-lifecycle/onboard
```

**请求体**:
```json
{
  "username": "zhangsan",
  "realName": "张三",
  "email": "zhangsan@example.com",
  "department": "财务部",
  "position": "会计",
  "fondsIds": [1, 2]
}
```

**响应**:
```json
{
  "code": 200,
  "data": "用户入职处理成功",
  "message": "100"
}
```

### 5.2 员工离职

```
POST /user-lifecycle/offboard
```

**请求体**:
```json
{
  "userId": 100,
  "reason": "员工辞职"
}
```

### 5.3 员工调动

```
POST /user-lifecycle/transfer
```

**请求体**:
```json
{
  "userId": 100,
  "toDepartment": "审计部",
  "effectiveDate": "2026-03-13"
}
```

---

## 6. 权限复核 (Access Review)

### 6.1 查询待复核任务列表

```
GET /access-review/tasks
```

**查询参数**:
- `reviewerId`: 复核人ID（可选）

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "reviewName": "2026Q1 权限复核",
      "status": "PENDING",
      "reviewerId": "10"
    }
  ]
}
```

### 6.2 创建复核任务

```
POST /access-review/create
```

**请求体**:
```json
{
  "reviewName": "2026Q1 权限复核",
  "reviewType": "QUARTERLY",
  "reviewerIds": [10, 11]
}
```

### 6.3 执行复核

```
POST /access-review/{id}/execute
```

**请求体**:
```json
{
  "approved": true,
  "comment": "复核通过"
}
```

---

## 7. 预归档管理 (Pre-Archival)

### 7.1 电子凭证池

```
GET /api/pool
```

**查询参数**:
- `page`: 页码
- `size`: 每页数量
- `poolStatus`: 待检测 / 待补全 / 待归档
- `voucherType`: 凭证类型
- `dateStart`: 开始日期
- `dateEnd`: 结束日期

### 7.2 删除单据池记录

```
DELETE /api/pool/{id}
```

### 7.3 手动关联

```
POST /api/pool/associate
```

**请求体**:
```json
{
  "archiveId": 1,
  "attachmentIds": [10, 20, 30]
}
```

---

## 8. 档案管理 (Archives Management)

### 8.1 档案列表

```
GET /api/archives
```

### 8.2 档案详情

```
GET /api/archives/{id}
```

### 8.3 归档审批批量操作

```
POST /api/archives/approval/batch-approve
POST /api/archives/approval/batch-reject
```

**请求体**:
```json
{
  "archiveIds": [1, 2, 3, ...],
  "comment": "批量审批备注"
}
```

---

## 9. 案卷管理 (Volume Management)

### 9.1 案卷列表

```
GET /api/volumes
```

### 9.2 创建案卷

```
POST /api/volumes
```

### 9.3 案卷审批

```
POST /api/volumes/{id}/approve
```

---

## 10. 借阅管理 (Borrowing Management)

### 10.1 创建借阅申请

```
POST /api/borrowing
```

**请求体**:
```json
{
  "archiveIds": [1, 2, 3],
  "borrowReason": "审计需要",
  "expectedReturnDate": "2026-04-13"
}
```

### 10.2 借阅列表

```
GET /api/borrowing
```

### 10.3 审批借阅

```
POST /api/borrowing/{id}/approve
```

### 10.4 归还

```
POST /api/borrowing/{id}/return
```

### 10.5 取消借阅

```
POST /api/borrowing/{id}/cancel
```

---

## 11. 销毁管理 (Destruction Management)

### 11.1 销毁申请列表

```
GET /api/destruction
```

### 11.2 销毁审批批量操作

```
POST /api/destruction/batch-approve
POST /api/destruction/batch-reject
```

**请求体**:
```json
{
  "destructionIds": [1, 2, 3, ...],
  "comment": "批量审批备注"
}
```

---

## 12. 全宗管理 (Fonds Management)

### 12.1 全宗列表

```
GET /api/fonds
```

### 12.2 创建全宗

```
POST /api/fonds
```

### 12.3 更新全宗

```
PUT /api/fonds/{id}
```

---

## 13. 全库检索 (Global Search)

### 13.1 全文搜索

```
GET /api/search?q=关键词&page=1&size=20
```

**查询参数**:
- `q`: 搜索关键词
- `type`: 搜索类型 (all/archives/metadata)
- `fondsCode`: 全宗代码

---

## 14. 符合性检查 (Compliance Check)

### 14.1 执行检查

```
POST /api/compliance/check
```

**请求体**:
```json
{
  "archiveIds": [1, 2, 3],
  "checkItems": ["RETENTION_PERIOD", "CLASSIFICATION", "ARCHIVAL_CODE"]
}
```

### 14.2 获取检查报告

```
GET /api/compliance/report/{id}
```

---

## 错误代码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 500 | 服务器内部错误 |

---

## 附录

### A. 档案状态枚举

| 状态 | 说明 |
|------|------|
| DRAFT | 草稿 |
| PENDING | 待审批 |
| APPROVED | 已批准 |
| REJECTED | 已拒绝 |
| ARCHIVED | 已归档 |
| DESTROYED | 已销毁 |

### B. 保管期限枚举

| 代码 | 说明 |
|------|------|
| PERMANENT | 永久 |
| 30_YEARS | 30年 |
| 10_YEARS | 10年 |

### C. 用户角色枚举

| 角色 | 代码 |
|------|------|
| 超级管理员 | super_admin |
| 系统管理员 | system_admin |
| 安全保密员 | security_admin |
| 安全审计员 | audit_admin |
| 业务操作员 | business_user |
