# ERP AI 适配系统 - 使用指南

> **版本**: 1.0.0-MVP
> **更新日期**: 2026-01-02
> **模块**: `integration.erp.ai`

---

## 目录

1. [快速开始](#快速开始)
2. [REST API 使用](#rest-api-使用)
3. [工作流程详解](#工作流程详解)
4. [示例场景](#示例场景)
5. [故障排查](#故障排查)

---

## 快速开始

### 前置条件

- 后端服务运行在 `http://localhost:19090`
- 已登录并获取有效的 JWT Token
- 准备好 ERP 系统的 OpenAPI 文档（JSON/YAML 格式）

### 最简单的示例

```bash
# 1. 准备 OpenAPI 文件
cat > openapi.json << 'EOF'
{
  "openapi": "3.0.0",
  "info": {
    "title": "金蝶云星空 API",
    "version": "1.0.0"
  },
  "paths": {
    "/api/v1/vouchers": {
      "get": {
        "operationId": "listVouchers",
        "summary": "获取凭证列表",
        "tags": ["vouchers"]
      }
    }
  }
}
EOF

# 2. 调用适配 API
curl -X POST "http://localhost:19090/api/erp-ai/adapt" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "files=@openapi.json" \
  -F "erpType=kingdee" \
  -F "erpName=金蝶云星空"

# 3. 查看生成的代码
# 响应中包含生成的 Java 适配器代码
```

---

## REST API 使用

### 端点概览

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/erp-ai/adapt` | POST | 上传 OpenAPI 文档并生成适配器 |
| `/api/erp-ai/preview/{sessionId}` | GET | 预览生成的代码（MVP 简化版） |

### 1. 上传文档并生成适配器

**请求**: `POST /api/erp-ai/adapt`

**参数**:
- `files` (required): OpenAPI 文档文件（支持多个）
- `erpType` (required): ERP 类型标识符（如 `kingdee`, `yonsuite`）
- `erpName` (required): ERP 系统名称（如 `金蝶云星空`）

**示例 1: 单文件上传**

```bash
curl -X POST "http://localhost:19090/api/erp-ai/adapt" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "files=@kingdle-voucher-api.json" \
  -F "erpType=kingdee" \
  -F "erpName=金蝶云星空"
```

**示例 2: 多文件上传**

```bash
curl -X POST "http://localhost:19090/api/erp-ai/adapt" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "files=@voucher-api.json" \
  -F "files=@invoice-api.json" \
  -F "files=@receipt-api.json" \
  -F "erpType=yonsuite" \
  -F "erpName=用友 YonSuite"
```

**成功响应**:

```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "success": true,
    "code": {
      "adapterClass": "// 生成的适配器类代码...",
      "className": "KingdeeVoucherAdapter",
      "packageName": "com.nexusarchive.integration.erp",
      "erpType": "kingdee",
      "erpName": "金蝶云星空",
      "dtoClasses": [
        {
          "className": "VoucherDto",
          "packageName": "com.nexusarchive.integration.erp.dto",
          "code": "// DTO 类代码..."
        }
      ],
      "testClass": "// 测试类代码...",
      "configSql": "-- SQL 配置脚本..."
    },
    "mappings": [
      {
        "scenario": "VOUCHER_SYNC",
        "apiPath": "/api/v1/vouchers",
        "method": "GET",
        "confidence": "HIGH"
      }
    ],
    "adapterId": "kingdee",
    "message": "ERP 适配完成"
  }
}
```

**错误响应**:

```json
{
  "success": false,
  "message": "未能从文档中提取任何 API 定义"
}
```

### 2. 预览生成的代码（MVP 简化版）

**请求**: `GET /api/erp-ai/preview/{sessionId}`

**注意**: MVP 版本中此端点为简化实现，不返回实际代码。

```bash
curl -X GET "http://localhost:19090/api/erp-ai/preview/abc123" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 工作流程详解

### 端到端流程

```
┌─────────────┐
│ 1. 上传文档 │  OpenAPI JSON/YAML
└──────┬──────┘
       ▼
┌─────────────┐
│ 2. 解析文档 │  提取 API 定义（路径、方法、参数）
└──────┬──────┘
       ▼
┌─────────────┐
│ 3. 语义映射 │  理解业务意图 → 标准场景
└──────┬──────┘
       ▼
┌─────────────┐
│ 4. 代码生成 │  生成 Java 适配器类
└──────┬──────┘
       ▼
┌─────────────┐
│ 5. 返回结果 │  完整的代码 + 映射关系
└─────────────┘
```

### 步骤详解

#### Step 1: 文档解析

**输入**: OpenAPI JSON/YAML 文件

**处理**:
- 使用 Swagger Parser 解析文档
- 提取所有 API 端点定义
- 解析路径、HTTP 方法、操作 ID、摘要、参数、请求体、响应

**输出**: `OpenApiDefinition` 列表

**示例**:

```json
// 输入 OpenAPI
{
  "paths": {
    "/api/v1/vouchers": {
      "get": {
        "operationId": "listVouchers",
        "summary": "获取凭证列表",
        "parameters": [...]
      }
    }
  }
}

// 输出 OpenApiDefinition
{
  "path": "/api/v1/vouchers",
  "method": "GET",
  "operationId": "listVouchers",
  "summary": "获取凭证列表",
  "parameters": [...]
}
```

#### Step 2: 语义映射

**输入**: `OpenApiDefinition` 列表

**处理**:
- 基于 API 的路径、操作 ID、摘要、标签推断业务意图
- 匹配到预定义的标准场景（如 `VOUCHER_SYNC`, `INVOICE_SYNC`）
- 计算匹配置信度

**支持的意图维度**:
- **操作类型**: QUERY（查询）, SYNC（同步）, SUBMIT（提交）, CALLBACK（回调）, NOTIFY（通知）
- **业务对象**: ACCOUNTING_VOUCHER（凭证）, INVOICE（发票）, RECEIPT（收据）, CONTRACT（合同）, ATTACHMENT（附件）, ACCOUNT_BALANCE（账户余额）
- **触发时机**: REALTIME（实时）, BATCH（批量）, SCHEDULED（定时）
- **数据流向**: INBOUND（ERP → 档案系统）, OUTBOUND（档案系统 → ERP）

**输出**: `ScenarioMapping` 列表

**示例**:

```java
// API: GET /api/v1/vouchers
// 映射结果:
{
  "scenario": "VOUCHER_SYNC",
  "apiPath": "/api/v1/vouchers",
  "method": "GET",
  "confidence": "HIGH",
  "intent": {
    "operationType": "QUERY",
    "businessObject": "ACCOUNTING_VOUCHER",
    "triggerTiming": "REALTIME",
    "dataFlowDirection": "INBOUND"
  }
}
```

#### Step 3: 代码生成

**输入**: `ScenarioMapping` 列表 + ERP 类型/名称

**生成内容**:
1. **适配器类**: 实现 `ErpAdapter` 接口的主类
2. **DTO 类**: 数据传输对象
3. **测试类**: JUnit 测试框架
4. **SQL 配置**: 数据库配置脚本

**示例输出**:

```java
@ErpAdapter(
    identifier = "kingdee",
    name = "金蝶云星空",
    supportedScenarios = {"VOUCHER_SYNC", "INVOICE_SYNC"}
)
public class KingdeeVoucherAdapter implements ErpAdapter {
    @Override
    public List<VoucherDto> fetchVouchers(LocalDateTime start, LocalDateTime end) {
        // 生成的实现代码
    }
}
```

---

## 示例场景

### 场景 1: 金蝶云星空凭证同步

**输入 OpenAPI 文档**:

```json
{
  "openapi": "3.0.0",
  "info": {
    "title": "金蝶云星空 Web API",
    "version": "1.0.0"
  },
  "paths": {
    "/api/v1/vouchers": {
      "get": {
        "operationId": "listVouchers",
        "summary": "查询会计凭证列表",
        "tags": ["voucher", "accounting"]
      }
    },
    "/api/v1/vouchers/{id}": {
      "get": {
        "operationId": "getVoucherDetail",
        "summary": "获取凭证详情",
        "tags": ["voucher"]
      }
    }
  }
}
```

**请求**:

```bash
curl -X POST "http://localhost:19090/api/erp-ai/adapt" \
  -H "Authorization: Bearer $TOKEN" \
  -F "files=@kingdee-voucher-api.json" \
  -F "erpType=kingdee" \
  -F "erpName=金蝶云星空"
```

**结果**:

- 识别场景: `VOUCHER_SYNC`（凭证同步）
- 生成的适配器: `KingdeeVoucherAdapter.java`
- 支持的操作: `fetchVouchers()`, `fetchVoucherDetail()`

### 场景 2: 用友 YonSuite 多场景

**输入 OpenAPI 文档**:

```json
{
  "openapi": "3.0.0",
  "info": {
    "title": "YonSuite OpenAPI",
    "version": "2.0.0"
  },
  "paths": {
    "/yonbip/fi/gl/voucher": {
      "post": {
        "operationId": "syncVoucher",
        "summary": "同步会计凭证",
        "tags": ["gl", "voucher"]
      }
    },
    "/yonbip/ar/ap/invoice": {
      "get": {
        "operationId": "queryInvoices",
        "summary": "查询应付发票",
        "tags": ["ar", "invoice"]
      }
    }
  }
}
```

**请求**:

```bash
curl -X POST "http://localhost:19090/api/erp-ai/adapt" \
  -H "Authorization: Bearer $TOKEN" \
  -F "files=@yonsuite-api.json" \
  -F "erpType=yonsuite" \
  -F "erpName=用友 YonSuite"
```

**结果**:

- 识别场景:
  - `VOUCHER_SYNC`（凭证同步）
  - `INVOICE_SYNC`（发票同步）
- 生成的适配器: `YonSuiteVoucherInvoiceAdapter.java`

---

## 故障排查

### 常见错误

#### 错误 1: 文件格式不正确

**症状**: `400 Bad Request` - "文件处理失败"

**原因**:
- 上传的文件不是有效的 JSON/YAML
- OpenAPI 格式不符合 3.0 规范

**解决方案**:

```bash
# 验证 JSON 格式
cat openapi.json | jq .

# 或使用在线验证工具
# https://validator.swagger.io/
```

#### 错误 2: 无法识别场景

**症状**: 响应中 `mappings` 为空或包含 `UNKNOWN` 场景

**原因**:
- API 路径、操作 ID、摘要中缺少关键词
- 业务对象不在支持列表中

**解决方案**:

```json
// 确保 OpenAPI 文档包含清晰的命名
{
  "operationId": "syncVouchers",  // ✅ 包含 "Voucher" 关键词
  "summary": "同步会计凭证",       // ✅ 清晰的描述
  "tags": ["voucher"]             // ✅ 相关标签
}
```

#### 错误 3: 认证失败

**症状**: `401 Unauthorized`

**解决方案**:

```bash
# 1. 重新登录获取 Token
TOKEN=$(curl -X POST "http://localhost:19090/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.data.token')

# 2. 使用 Token 调用 API
curl -X POST "http://localhost:19090/api/erp-ai/adapt" \
  -H "Authorization: Bearer $TOKEN" \
  ...
```

### 调试技巧

#### 启用详细日志

**application.yml**:

```yaml
logging:
  level:
    com.nexusarchive.integration.erp.ai: DEBUG
```

**查看日志**:

```bash
# 后端日志位置
tail -f nexusarchive-java/logs/application.log | grep "ErpAdaptation"

# 输出示例
# DEBUG ErpAdaptationOrchestrator - 开始 ERP 适配: erpType=kingdee
# DEBUG OpenApiDocumentParser - 解析文件: kingdee-api.json
# DEBUG BusinessSemanticMapper - 映射场景: /api/v1/vouchers → VOUCHER_SYNC
```

#### 测试单个组件

```bash
# 仅测试解析器
mvn test -Dtest=OpenApiDocumentParserTest

# 仅测试映射器
mvn test -Dtest=BusinessSemanticMapperTest

# 仅测试生成器
mvn test -Dtest=ErpAdapterCodeGeneratorTest
```

---

## Phase 2 功能预览

以下功能计划在后续版本中实现：

### PDF 文档解析

```bash
curl -X POST "http://localhost:19090/api/erp-ai/adapt" \
  -F "files=@erp-api-documentation.pdf" \
  -F "erpType=sap" \
  -F "erpName=SAP S/4HANA"
```

### Markdown 文档支持

```bash
curl -X POST "http://localhost:19090/api/erp-ai/adapt" \
  -F "files=@api-reference.md" \
  -F "erpType=custom" \
  -F "erpName=自定义 ERP"
```

### 智能语义理解（集成 Claude API）

- 自动理解复杂的 API 文档结构
- 支持非标准命名约定
- 提供映射建议和优化

### 自动编译和部署

```bash
# 生成的代码自动编译
curl -X POST "http://localhost:19090/api/erp-ai/deploy" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"adapterId": "kingdee"}'

# 返回部署状态
{
  "success": true,
  "buildStatus": "SUCCESS",
  "deploymentUrl": "/api/erp/kingdee"
}
```

---

## 附录

### A. 支持的标准场景

| 场景代码 | 描述 | 典型 API 模式 |
|---------|------|--------------|
| `VOUCHER_SYNC` | 会计凭证同步 | `GET /api/vouchers` |
| `INVOICE_SYNC` | 发票同步 | `GET /api/invoices` |
| `RECEIPT_SYNC` | 收据同步 | `GET /api/receipts` |
| `CONTRACT_SYNC` | 合同同步 | `GET /api/contracts` |
| `ATTACHMENT_SYNC` | 附件同步 | `GET /api/attachments` |
| `BALANCE_QUERY` | 账户余额查询 | `GET /api/balances` |
| `VOUCHER_SUBMIT` | 凭证提交 | `POST /api/vouchers` |
| `INVOICE_SUBMIT` | 发票提交 | `POST /api/invoices` |
| `VOUCHER_CALLBACK` | 凭证回调 | `POST /api/callbacks/voucher` |
| `NOTIFY_STATUS` | 状态通知 | `POST /api/notifications` |

### B. API 完整响应示例

```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "success": true,
    "code": {
      "adapterClass": "package com.nexusarchive.integration.erp;\n\n@ErpAdapter(...)\npublic class KingdeeVoucherAdapter implements ErpAdapter {\n  ...\n}",
      "className": "KingdeeVoucherAdapter",
      "packageName": "com.nexusarchive.integration.erp",
      "erpType": "kingdee",
      "erpName": "金蝶云星空",
      "dtoClasses": [
        {
          "className": "VoucherDto",
          "packageName": "com.nexusarchive.integration.erp.dto",
          "code": "package com.nexusarchive.integration.erp.dto;\n\n@Data\npublic class VoucherDto {\n  private String id;\n  private String voucherNo;\n  ...\n}"
        }
      ],
      "testClass": "@Test\nvoid testFetchVouchers() {\n  ...\n}",
      "configSql": "INSERT INTO sys_erp_config (erp_type, api_endpoint, ...) VALUES ('kingdee', 'https://api.kingdee.com', ...);"
    },
    "mappings": [
      {
        "scenario": "VOUCHER_SYNC",
        "scenarioDescription": "凭证同步",
        "apiPath": "/api/v1/vouchers",
        "method": "GET",
        "confidence": "HIGH",
        "intent": {
          "operationType": "QUERY",
          "businessObject": "ACCOUNTING_VOUCHER",
          "triggerTiming": "REALTIME",
          "dataFlowDirection": "INBOUND"
        }
      }
    ],
    "adapterId": "kingdee",
    "message": "ERP 适配完成，共识别 1 个标准场景"
  }
}
```

### C. 技术支持

- **模块位置**: `com.nexusarchive.integration.erp.ai`
- **测试覆盖**: 12/12 测试通过
- **文档更新**: 2026-01-02
- **版本**: 1.0.0-MVP
