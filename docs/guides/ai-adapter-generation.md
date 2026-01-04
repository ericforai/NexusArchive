# ERP 适配器自动生成指南

> **重要提示**: 本系统已简化为基于模板的代码生成方式，不再使用 AI/LLM。本文档保留作为历史参考。

## 概述

本系统使用模板引擎自动生成完整的 ERP 适配器代码，包括 HTTP 客户端、数据映射和错误处理。

**主要特性**:
- ✅ 基于 OpenAPI 规范自动生成代码
- ✅ 模板驱动，无需 AI 依赖
- ✅ 自动场景映射和代码生成
- ✅ 一键部署到生产环境

## 使用步骤

### 1. 准备 OpenAPI 文档

确保你的 OpenAPI 文档包含：
- 完整的请求/响应 Schema
- Base URL 和端点路径
- 清晰的 API 命名

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
      operationId: listSalesOut
      summary: 销售出库单列表
      parameters:
        - name: startDate
          in: query
          schema:
            type: string
```

### 2. 上传并生成

**使用 REST API**:
```bash
# 生成代码
curl -X POST http://localhost:19090/api/erp-ai/adapt \
  -F "files=@yonsuite-api.json" \
  -F "erpType=yonsuite" \
  -F "erpName=YonSuite" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 生成并自动部署
curl -X POST http://localhost:19090/api/erp-ai/deploy \
  -F "files=@yonsuite-api.json" \
  -F "erpType=yonsuite" \
  -F "erpName=YonSuite" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 3. 部署流程

使用 `/api/erp-ai/deploy` 端点时，系统会自动：
1. 解析 OpenAPI 文档
2. 映射业务场景
3. 生成适配器代码
4. 保存到源码目录
5. 编译验证
6. 运行测试
7. 数据库注册
8. 热加载适配器

## 工作原理

### 文档解析
- 使用 Swagger Parser 解析 OpenAPI 规范
- 提取 API 端点、参数、响应结构

### 场景映射
- 基于 API 路径、操作 ID、摘要进行模式匹配
- 自动识别标准业务场景（凭证同步、发票同步等）

### 代码生成
- 使用 Velocity 模板引擎
- 生成适配器类、DTO、测试类

### 自动部署
- Maven 编译和测试
- 数据库自动注册
- Spring 热加载

## 常见问题

### 文档解析失败
- 确认 OpenAPI 格式正确（3.0 规范）
- 使用在线工具验证：https://validator.swagger.io/

### 场景识别不准确
- 使用清晰的 API 命名（operationId, summary）
- 添加相关标签（tags）
- 参考支持的标准场景列表

### 部署失败
- 检查编译日志
- 验证数据库连接
- 查看测试结果

## 相关文档

- [完整使用指南](../development/erp-adapter-guide.md)
- [系统架构说明](../architecture/erp-ai-usage-guide.md)
- [手动开发指南](../architecture/erp-adapter-development-guide.md)

## 版本历史

- **v2.0.0** (2026-01-04): 简化为模板驱动，移除 AI 依赖
- **v1.0.0** (2026-01-02): 初始 MVP 版本（基于 AI）

