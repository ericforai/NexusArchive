一旦我所属的文件夹有所变化，请更新我。
本目录存放 API 接口说明文档。
用于接口查询与开发对接。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `API接口文档-v2.0.md` | 文档 | **完整 API 文档 (v2.0)** |
| `error-codes.md` | 文档 | **错误码文档** |
| `pagination-api.md` | 文档 | **分页接口文档** |
| `async-api.md` | 文档 | **异步接口文档** |
| `access_token_notes.md` | 文档 | access_token_notes 文档 |
| `api_test_plan.md` | 文档 | api_test_plan 文档 |
| `API接口文档` | 文件 | API接口文档 文件 |
| `YS凭证列表查询API` | 文件 | YS凭证列表查询API 文件 |
| `YS凭证详情查询API` | 文件 | YS凭证详情查询API 文件 |
| `接口速查.md` | 文档 | 接口速查 文档 |
| `收款单列表查询.md` | 文档 | 收款单列表查询 文档 |
| `收款单详情查询.md` | 文档 | 收款单详情查询 文档 |
| `组卷归档API接口文档.md` | 文档 | 组卷归档API接口文档 文档 |
| `ai-generation-api.md` | 文档 | AI 适配器生成 API 文档 |
| `borrowing.md` | 文档 | 借阅管理 API 文档 |

## 快速导航

### 核心文档
- [完整 API 文档 (v2.0)](./API接口文档-v2.0.md) - 完整的 API 接口文档
- [错误码文档](./error-codes.md) - 系统错误码定义和处理建议
- [分页接口文档](./pagination-api.md) - 分页查询接口说明
- [异步接口文档](./async-api.md) - 异步任务接口说明

### Swagger UI
- **本地开发**: http://localhost:19090/api/swagger-ui.html
- **开发环境**: https://api-dev.nexusarchive.com/api/swagger-ui.html

### 认证说明
所有 API 请求需要在 HTTP Header 中携带 JWT Token：
```http
Authorization: Bearer <your-jwt-token>
```

### 通用响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {
    // 响应数据
  }
}
```
