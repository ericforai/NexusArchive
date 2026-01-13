一旦我所属的文件夹有所变化，请更新我。

# YonSuite Controller Layer

> **Updated**: 2026-01-05

## Overview

本目录包含 YonSuite 集成的 REST API 控制器。

## Controllers

### 1. GenericYonSuiteController ⭐ (v2.0 新增)

**职责**：通用适配器控制器，提供配置驱动的同步接口

**路径**：`/api/yonsuite/generic`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/salesout/sync` | POST | 同步销售出库单列表 |
| `/salesout/sync/recent` | POST | 快速同步（最近 7 天） |
| `/salesout/detail` | POST | 同步单个销售出库单详情 |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `configId` | Long | ✅ | ERP 配置 ID（sys_erp_config.id） |
| `startDate` | String | ❌ | 开始日期（yyyy-MM-dd），recent 接口自动计算 |
| `endDate` | String | ❌ | 结束日期（yyyy-MM-dd），recent 接口自动计算 |
| `salesOutId` | String | ❌ | 销售出库单 ID（detail 接口使用） |

**响应格式**：
```json
{
  "code": 200,
  "message": "同步成功，共 N 条",
  "data": ["YonSuite_SALESOUT_123", "YonSuite_SALESOUT_456", ...],
  "timestamp": 1767522820299
}
```

### 2. YonPaymentTestController

**职责**：付款单测试接口

**路径**：`/api/yonsuite/payment/test`

### 3. YonSuiteCollectionController

**职责**：收款单接口

**路径**：`/api/yonsuite/collection`

### 4. YonSuiteWebhookController

**职责**：Webhook 事件接收

**路径**：`/api/yonsuite/webhook`

## Usage Example

### cURL 示例

```bash
# 1. 登录获取 Token
TOKEN=$(curl -s -X POST "http://localhost:19090/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.data.token')

# 2. 同步最近 7 天销售出库单
curl -X POST "http://localhost:19090/api/yonsuite/generic/salesout/sync/recent?configId=1" \
  -H "Authorization: Bearer $TOKEN"

# 3. 同步指定日期范围
curl -X POST "http://localhost:19090/api/yonsuite/generic/salesout/sync" \
  -H "Authorization: Bearer $TOKEN" \
  -d "configId=1&startDate=2025-01-01&endDate=2025-01-31"

# 4. 同步单个详情
curl -X POST "http://localhost:19090/api/yonsuite/generic/salesout/detail" \
  -H "Authorization: Bearer $TOKEN" \
  -d "configId=1&salesOutId=123456"
```

### JavaScript/TypeScript 示例

```typescript
// api/yonsuite.ts
import axios from 'axios';

const API_BASE = '/api/yonsuite/generic';

export interface SalesOutSyncRequest {
  configId: number;
  startDate?: string;  // yyyy-MM-dd
  endDate?: string;    // yyyy-MM-dd
}

export interface SalesOutSyncResponse {
  code: number;
  message: string;
  data: string[];     // 同步的 ID 列表
  timestamp: number;
}

// 同步最近 7 天
export async function syncRecentSalesOut(configId: number): Promise<SalesOutSyncResponse> {
  const response = await axios.post(
    `${API_BASE}/salesout/sync/recent?configId=${configId}`,
    {},
    { withCredentials: true }
  );
  return response.data;
}

// 同步指定日期范围
export async function syncSalesOut(
  configId: number,
  startDate: string,
  endDate: string
): Promise<SalesOutSyncResponse> {
  const response = await axios.post(
    `${API_BASE}/salesout/sync`,
    { configId, startDate, endDate },
    { withCredentials: true }
  );
  return response.data;
}
```

### React Component 示例

```tsx
// components/YonSuiteSyncButton.tsx
import { useState } from 'react';
import { message } from 'antd';
import { syncRecentSalesOut } from '@/api/yonsuite';

export function YonSuiteSyncButton({ configId }: { configId: number }) {
  const [loading, setLoading] = useState(false);

  const handleSync = async () => {
    setLoading(true);
    try {
      const result = await syncRecentSalesOut(configId);
      if (result.code === 200) {
        message.success(result.message);
        console.log('同步的 ID:', result.data);
      }
    } catch (error) {
      message.error('同步失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <button onClick={handleSync} disabled={loading}>
      {loading ? '同步中...' : '同步销售出库单'}
    </button>
  );
}
```

## Error Handling

### HTTP Status Codes

| Status | Description |
|--------|-------------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权（Token 无效或过期） |
| 404 | ERP 配置不存在 |
| 500 | 服务器内部错误 |

### Error Response Format

```json
{
  "code": 500,
  "message": "同步失败: 具体错误信息",
  "timestamp": 1767522820299
}
```

## Security

### Authentication

所有接口需要 JWT Bearer Token：

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSi...
```

### Authorization

建议限制只有管理员角色才能调用同步接口：

```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/salesout/sync")
public Result<List<String>> syncSalesOutList(...) {
    // ...
}
```

## File List

| 文件 | 状态 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `GenericYonSuiteController.java` | ✅ 控制器 | 通用适配器控制器（v2.0 新增） |
| `YonPaymentTestController.java` | ⚠️ 测试 | 付款单测试接口 |
| `YonSuiteCollectionController.java` | ⚠️ 旧版 | 收款单控制器 |
| `YonSuiteWebhookController.java` | ✅ 控制器 | Webhook 控制器 |

## Migration Notes

### 从旧版 Controller 迁移

**旧版 URL**：
```
/api/yonsuite/voucher/sync
```

**新版 URL**：
```
/api/yonsuite/generic/salesout/sync/recent
```

**主要区别**：
- 旧版：每个业务对象一个 Controller
- 新版：通用 Controller，配置驱动
- 旧版：需要编写代码
- 新版：只需配置数据库

## References

- [YonSuite 集成模块文档](/nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/README.md)
- [Service Layer 文档](/nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/service/README.md)
