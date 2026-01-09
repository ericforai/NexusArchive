# 分页接口文档

> **版本**: v2.0.0
> **更新日期**: 2026-01-09

---

## 概述

系统提供统一的分页查询接口，支持多条件筛选、排序、全宗隔离等功能。

---

## 分页参数

### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码，从 1 开始 |
| limit | int | 否 | 10 | 每页条数，最大 100 |
| sort | string | 否 | createdAt | 排序字段 |
| order | string | 否 | desc | 排序方向: asc/desc |

### 响应结构

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],        // 数据列表
    "total": 100,         // 总记录数
    "size": 10,           // 每页条数
    "current": 1,         // 当前页码
    "pages": 10           // 总页数
  }
}
```

---

## 档案分页接口

### 接口定义

```http
GET /api/archives
```

### 请求参数

| 参数 | 类型 | 说明 | 示例 |
|------|------|------|------|
| page | int | 页码 | 1 |
| limit | int | 每页条数 | 20 |
| search | string | 搜索关键词 | 2024年度 |
| status | string | 状态 | ARCHIVED |
| categoryCode | string | 类别号 | AC01 |
| orgId | string | 部门ID | DEPT001 |
| subType | string | 子类型 | 10Y |
| uniqueBizId | string | 唯一业务ID | YS-2024-001 |
| fondsNo | string | 全宗号 | COMP001 |

### 请求示例

```http
GET /api/archives?page=1&limit=20&status=ARCHIVED&fondsNo=COMP001
Authorization: Bearer <token>
```

### 响应示例

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
        "fondsNo": "COMP001",
        "categoryCode": "AC01",
        "createdAt": "2024-01-01T10:00:00"
      }
    ],
    "total": 150,
    "size": 20,
    "current": 1,
    "pages": 8
  }
}
```

---

## ERP 同步任务分页接口

### 接口定义

```http
GET /api/erp/scenario/{id}/sync/tasks
```

### 请求参数

| 参数 | 类型 | 说明 | 示例 |
|------|------|------|------|
| id | long | 场景ID (路径参数) | 1 |
| page | int | 页码 | 1 |
| limit | int | 每页条数 | 20 |
| status | string | 任务状态 | RUNNING |

### 请求示例

```http
GET /api/erp/scenario/1/sync/tasks?page=1&limit=20
Authorization: Bearer <token>
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "taskId": "sync-task-001",
        "scenarioId": 1,
        "scenarioName": "YonSuite 凭证同步",
        "status": "COMPLETED",
        "submittedAt": "2026-01-09T10:00:00",
        "completedAt": "2026-01-09T10:05:00",
        "syncedCount": 100,
        "failedCount": 0
      }
    ],
    "total": 50,
    "size": 20,
    "current": 1,
    "pages": 3
  }
}
```

---

## 审计日志分页接口

### 接口定义

```http
GET /api/audit/logs
```

### 请求参数

| 参数 | 类型 | 说明 | 示例 |
|------|------|------|------|
| page | int | 页码 | 1 |
| limit | int | 每页条数 | 50 |
| startDate | string | 开始日期 | 2026-01-01 |
| endDate | string | 结束日期 | 2026-01-31 |
| operationType | string | 操作类型 | CREATE |
| userId | string | 用户ID | user001 |

### 请求示例

```http
GET /api/audit/logs?page=1&limit=50&startDate=2026-01-01&endDate=2026-01-31
Authorization: Bearer <token>
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": "log-001",
        "operationType": "CREATE",
        "resourceType": "ARCHIVE",
        "resourceId": "123456",
        "userId": "user001",
        "userName": "张三",
        "ipAddress": "192.168.1.100",
        "dataBefore": null,
        "dataAfter": "{\"title\":\"新档案\"}",
        "createdAt": "2026-01-09T10:00:00"
      }
    ],
    "total": 500,
    "size": 50,
    "current": 1,
    "pages": 10
  }
}
```

---

## 用户分页接口

### 接口定义

```http
GET /api/users
```

### 请求参数

| 参数 | 类型 | 说明 | 示例 |
|------|------|------|------|
| page | int | 页码 | 1 |
| limit | int | 每页条数 | 20 |
| search | string | 搜索关键词 | 张三 |
| roleId | string | 角色ID | ROLE001 |
| status | string | 状态 | ACTIVE |

### 请求示例

```http
GET /api/users?page=1&limit=20&status=ACTIVE
Authorization: Bearer <token>
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": "user001",
        "username": "zhangsan",
        "name": "张三",
        "email": "zhangsan@example.com",
        "phone": "13800138000",
        "status": "ACTIVE",
        "roles": ["ARCHIVE_MANAGER"],
        "fondsList": ["COMP001"],
        "createdAt": "2024-01-01T10:00:00"
      }
    ],
    "total": 30,
    "size": 20,
    "current": 1,
    "pages": 2
  }
}
```

---

## 性能优化说明

### 分页查询优化

1. **索引使用**: 所有分页查询字段均已添加数据库索引
2. **全宗隔离**: 自动添加全宗过滤条件，减少数据扫描范围
3. **缓存策略**: 热点数据使用 Redis 缓存，减少数据库查询
4. **最大限制**: 单页最多返回 100 条记录，防止大数据量查询

### 推荐实践

1. **按需查询**: 使用 search、status 等条件精确筛选
2. **合理分页**: 每页 20-50 条记录，避免单页数据过多
3. **字段过滤**: 只查询需要的字段，减少数据传输量

---

## 客户端示例

### JavaScript/Axios

```javascript
// 分页查询档案
async function getArchives(page = 1, limit = 20, filters = {}) {
  const params = {
    page,
    limit,
    ...filters
  };

  const response = await axios.get('/api/archives', { params });
  return response.data;
}

// 使用示例
getArchives(1, 20, { status: 'ARCHIVED', fondsNo: 'COMP001' })
  .then(result => {
    console.log(`共 ${result.data.total} 条记录`);
    console.log(`第 ${result.data.current}/${result.data.pages} 页`);
    console.log('数据:', result.data.records);
  });
```

### React 分页组件

```typescript
import { useState, useEffect } from 'react';
import axios from 'axios';

interface PaginationState {
  page: number;
  limit: number;
  total: number;
  pages: number;
}

export function ArchiveList() {
  const [data, setData] = useState([]);
  const [pagination, setPagination] = useState<PaginationState>({
    page: 1,
    limit: 20,
    total: 0,
    pages: 0
  });

  const fetchData = async (page: number) => {
    const response = await axios.get('/api/archives', {
      params: { page, limit: pagination.limit }
    });

    setData(response.data.data.records);
    setPagination({
      page: response.data.data.current,
      limit: response.data.data.size,
      total: response.data.data.total,
      pages: response.data.data.pages
    });
  };

  useEffect(() => {
    fetchData(1);
  }, []);

  return (
    <div>
      {/* 数据列表 */}
      <table>
        {data.map(item => (
          <tr key={item.id}>{item.title}</tr>
        ))}
      </table>

      {/* 分页控件 */}
      <Pagination
        current={pagination.page}
        total={pagination.total}
        pageSize={pagination.limit}
        onChange={(page) => fetchData(page)}
      />
    </div>
  );
}
```

---

## 更新日志

### v2.0.0 (2026-01-09)

- 新增 ERP 同步任务分页接口
- 优化分页查询性能
- 添加全宗隔离说明
- 新增客户端示例代码

### v1.0.0 (2025-12-01)

- 初始版本
- 基础分页接口定义
