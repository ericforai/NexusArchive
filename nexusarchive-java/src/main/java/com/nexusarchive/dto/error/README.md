一旦我所属的文件夹有所变化，请更新我。
本目录存放错误响应相关的 DTO。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `ErrorResponse.java` | DTO | 统一错误响应格式 |
| `ErrorCategory.java` | 枚举 | 错误分类枚举 |

## 错误响应格式

所有异常响应统一使用 `ErrorResponse` 格式：

```json
{
  "code": "EAA_400",
  "message": "用户友好的错误信息",
  "requestId": "a1b2c3d4e5f6g7h8i9j0k1l2",
  "category": "VALIDATION_ERROR",
  "timestamp": "2024-01-01T00:00:00Z",
  "path": "/api/archives",
  "detail": {
    "exceptionType": "MethodArgumentNotValidException",
    "debugMessage": "详细错误信息（仅开发环境）",
    "validationError": {
      "field": "amount",
      "rejectedValue": -100,
      "message": "必须为正数"
    },
    "stackTrace": [
      "com.nexusarchive.controller.ArchiveController.create(ArchiveController.java:123)",
      "..."
    ]
  }
}
```

## 字段说明

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `code` | String | 是 | 错误码（如 "EAA_400"） |
| `message` | String | 是 | 用户友好的错误信息 |
| `requestId` | String | 是 | 请求追踪 ID，用于日志关联 |
| `category` | ErrorCategory | 是 | 错误分类 |
| `timestamp` | String | 是 | ISO-8601 格式时间戳 |
| `path` | String | 否 | 请求路径 |
| `detail` | Detail | 否 | 详细错误信息（仅开发环境） |

## 错误分类

| 分类 | HTTP 状态码 | 说明 |
| --- | --- | --- |
| `BUSINESS_ERROR` | 400 | 业务异常 |
| `VALIDATION_ERROR` | 400 | 参数验证失败 |
| `AUTHENTICATION_ERROR` | 401 | 认证失败 |
| `AUTHORIZATION_ERROR` | 403 | 授权失败 |
| `NOT_FOUND_ERROR` | 404 | 资源不存在 |
| `CONFLICT_ERROR` | 409 | 资源冲突 |
| `RATE_LIMIT_ERROR` | 429 | 请求过于频繁 |
| `SYSTEM_ERROR` | 500 | 系统内部错误 |
| `SERVICE_UNAVAILABLE_ERROR` | 503 | 服务暂时不可用 |
| `SECURITY_ERROR` | 500 | 安全异常 |

## 环境差异

- **生产环境**: 仅返回 `code`、`message`、`requestId`、`category`、`timestamp`、`path`
- **开发环境**: 额外返回 `detail` 字段，包含异常类型、堆栈跟踪等调试信息

## 请求追踪

请求追踪 ID (`requestId`) 通过以下方式生成和传递：

1. 客户端可通过请求头 `X-Request-ID` 传递自定义 ID
2. 若未提供，服务端自动生成 UUID
3. 响应头中会返回 `X-Request-ID`，方便客户端关联
