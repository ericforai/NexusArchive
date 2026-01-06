# 借阅管理 API 文档

## 概述
借阅管理模块提供档案借阅申请、审批、出库、归还等全流程功能。

## API 端点

### 基础路径
- `POST /api/borrowing` - 创建借阅申请
- `GET /api/borrowing` - 获取借阅列表
- `POST /api/borrowing/{id}/approve` - 审批借阅申请
- `POST /api/borrowing/{id}/return` - 归还档案
- `POST /api/borrowing/{id}/cancel` - 取消借阅申请

### 统计接口
- `GET /api/stats/borrowing` - 获取借阅统计

---

## 1. 创建借阅申请

**接口**: `POST /api/borrowing`

**权限**: `borrowing:create` 或 `ROLE_business_user` 或 `ROLE_SYSTEM_ADMIN`

**请求体**:
```json
{
  "archiveId": "archive-uuid",
  "reason": "审计查阅需要",
  "borrowDate": "2025-01-10",
  "expectedReturnDate": "2025-01-15"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "borrowing-uuid",
    "archiveId": "archive-uuid",
    "archiveTitle": "2024年度财务报告",
    "userName": "张三",
    "borrowDate": "2025-01-10",
    "expectedReturnDate": "2025-01-15",
    "status": "PENDING",
    "reason": "审计查阅需要"
  }
}
```

---

## 2. 获取借阅列表

**接口**: `GET /api/borrowing`

**权限**: `borrowing:view` 或 `borrowing:manage` 或 `ROLE_SYSTEM_ADMIN`

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| limit | int | 否 | 每页数量，默认10 |
| status | string | 否 | 状态筛选 |
| my | boolean | 否 | 是否只查看本人的借阅 |

**状态值**:
- `PENDING` - 待审批
- `APPROVED` - 已批准
- `REJECTED` - 已拒绝
- `BORROWED` - 已借出
- `RETURNED` - 已归还
- `OVERDUE` - 逾期
- `CANCELLED` - 已取消

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": "borrowing-uuid",
        "archiveId": "archive-uuid",
        "archiveTitle": "2024年度财务报告",
        "userName": "张三",
        "borrowDate": "2025-01-10",
        "expectedReturnDate": "2025-01-15",
        "actualReturnDate": null,
        "status": "PENDING",
        "reason": "审计查阅需要",
        "approvalComment": null
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1
  }
}
```

---

## 3. 审批借阅申请

**接口**: `POST /api/borrowing/{id}/approve`

**权限**: `borrowing:approve` 或 `borrowing:manage` 或 `ROLE_SYSTEM_ADMIN`

**请求体**:
```json
{
  "approved": true,
  "comment": "同意借阅，请注意保管"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "borrowing-uuid",
    "status": "APPROVED",
    "approvalComment": "同意借阅，请注意保管"
  }
}
```

---

## 4. 归还档案

**接口**: `POST /api/borrowing/{id}/return`

**权限**: `borrowing:return` 或 `borrowing:manage` 或 `ROLE_SYSTEM_ADMIN`

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 5. 取消借阅申请

**接口**: `POST /api/borrowing/{id}/cancel`

**权限**: `borrowing:cancel` 或 `borrowing:manage` 或 `ROLE_SYSTEM_ADMIN`

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 6. 获取借阅统计

**接口**: `GET /api/stats/borrowing`

**权限**: 与其他 stats 接口一致

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pendingCount": 5,
    "approvedCount": 2,
    "borrowedCount": 10,
    "overdueCount": 1,
    "totalActiveCount": 17
  }
}
```

---

## 状态流转图

```
     创建
       ↓
    PENDING (待审批)
       ↓
    审批 → APPROVED (已批准) → BORROWED (已借出) → RETURNED (已归还)
       ↓                                     ↓
    REJECTED (已拒绝)                    OVERDUE (逾期)
       ↓                                     ↓
    CANCELLED (已取消)                 (归还后完成)
```

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| BORROW_REQUEST_CANNOT_BE_EMPTY | 借阅请求不能为空 |
| BORROW_ARCHIVE_CANNOT_BE_EMPTY | 借阅档案不能为空 |
| BORROW_ARCHIVE_NOT_FOUND | 档案不存在，无法发起借阅 |
| BORROW_APPROVAL_PARAMS_CANNOT_BE_EMPTY | 审批参数不能为空 |
| BORROW_RECORD_NOT_FOUND | 借阅记录不存在或已被删除 |
| BORROW_INVALID_STATUS | 当前状态不允许执行此操作 |
| BORROW_USER_NOT_FOUND | 未获取到当前用户，请重新登录后重试 |
