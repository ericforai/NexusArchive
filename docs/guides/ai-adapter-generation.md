# AI 驱动的 ERP 适配器生成指南

## 概述

本系统使用 Claude AI 自动生成完整的 ERP 适配器代码，包括 HTTP 客户端、认证签名、数据映射和错误处理。

## 使用步骤

### 1. 准备 OpenAPI 文档

确保你的 OpenAPI 文档包含：
- 完整的请求/响应 Schema
- 认证方式说明
- Base URL 和端点路径

示例：
```yaml
openapi: 3.0.0
info:
  title: YonSuite API
  version: 1.0.0
servers:
  - url: https://api.yonyoucloud.com
paths:
  /yiyan/salesOut/list:
    get:
      summary: 销售出库单列表
      parameters:
        - name: startDate
          in: query
          schema:
            type: string
```

### 2. 上传并生成

**方法 1: 使用前端界面**
1. 访问 http://localhost:15175/system/settings/integration
2. 点击 "AI 智能适配器"
3. 上传 OpenAPI 文档
4. 选择 ERP 类型和认证方式
5. 点击 "生成适配器"

**方法 2: 使用 REST API**
```bash
curl -X POST http://localhost:19090/api/erp-ai/generate-ai \
  -F "file=@yonsuite-api.json" \
  -F "erpType=yonsuite" \
  -F "erpName=我的YonSuite" \
  -F "authType=appkey" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 3. 审核和迭代

生成的代码会返回供审核。你可以：
- 查看生成的代码
- 提供修改意见
- 请求重新生成
- 批准后部署

### 4. 部署

审核通过后，系统会：
- 保存代码到源码目录
- 编译验证
- 数据库注册
- 热加载适配器

## 成本控制

- 速率限制：10 次/分钟
- Token 使用：~2000 tokens/次
- 建议每月预算：$50-100

## 故障排查

### AI 生成失败
- 检查 API Key 配置
- 查看日志中的错误信息
- 确认 OpenAPI 文档格式正确

### 生成的代码无法编译
- 检查 AI 生成的代码是否完整
- 查看编译错误日志
- 提供反馈让 AI 重新生成
