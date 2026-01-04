# AI 生成 API 文档

## POST /api/erp-ai/generate-ai

使用 AI 生成 ERP 适配器代码。

**请求参数:**
- `file` (required): OpenAPI 文档文件
- `erpType` (required): ERP 类型标识
- `erpName` (required): 适配器名称
- `baseUrl` (optional): API Base URL
- `authType` (optional): 认证类型 (appkey/oauth2/none)

**响应:**
```json
{
  "success": true,
  "data": {
    "sessionId": "uuid",
    "status": "GENERATED",
    "generatedCode": "package com.example;..."
  }
}
```

## POST /api/erp-ai/regenerate-ai/{sessionId}

根据反馈重新生成代码。

## POST /api/erp-ai/approve/{sessionId}

批准生成的代码并继续部署。
