一旦我所属的文件夹有所变化，请更新我。

# YonSuite Integration Module

> **Version**: 2.1.0
> **Updated**: 2026-01-05
> **Module**: `integration.yonsuite`

## Overview

NexusArchive YonSuite 集成模块提供**配置驱动**的 ERP 适配能力，支持从用友 YonSuite 同步业务数据到电子会计档案系统。

**核心特性：**
- ✅ 配置驱动架构（读取数据库配置 → 调用 ERP API → 存储数据）
- ✅ HMAC-SHA256 签名认证
- ✅ Access Token 自动缓存
- ✅ 分页数据同步
- ✅ 自动映射到 ArcFileContent（电子凭证池）
- ✅ PDF 凭证生成（可选）
- ✅ 退款单同步支持（v2.1 新增）

## Architecture

### Directory Structure

```
integration/yonsuite/
├── client/               # HTTP 客户端封装
├── connector/            # YonSuite 连接器
├── controller/           # REST 控制器
│   ├── GenericYonSuiteController.java    # 通用同步接口
│   └── YonPaymentTestController.java    # 测试接口
├── dto/                  # 数据传输对象
│   ├── SalesOutListRequest.java         # 销售出库单列表请求
│   ├── SalesOutListResponse.java        # 销售出库单列表响应
│   ├── SalesOutDetailResponse.java      # 销售出库单详情响应
│   ├── VoucherAttachmentRequest.java    # 凭证附件查询请求
│   ├── VoucherAttachmentResponse.java   # 凭证附件查询响应
│   ├── YonRefundListRequest.java        # 退款单列表请求（v2.1 新增）
│   ├── YonRefundListResponse.java       # 退款单列表响应（v2.1 新增）
│   ├── YonRefundFileRequest.java        # 退款单附件请求（v2.1 新增）
│   └── YonRefundFileResponse.java       # 退款单附件响应（v2.1 新增）
├── event/                # Webhook 事件处理
├── mapper/               # 数据映射器
│   ├── SalesOutMapper.java              # 销售出库单映射
│   └── YonVoucherMapper.java            # 凭证映射
├── security/             # 签名验证与加密
└── service/              # 业务服务
    ├── YonAuthService.java              # Token 认证服务
    ├── GenericYonSuiteAdapter.java      # 通用适配器
    └── YonRefundListService.java        # 退款单列表服务（v2.1 新增）
```

### Data Flow

```
┌─────────────────┐
│  前端/定时任务   │  触发同步
└────────┬────────┘
         ▼
┌─────────────────────────────┐
│  GenericYonSuiteController  │  REST API 入口
└────────┬────────────────────┘
         ▼
┌─────────────────────────────┐
│  GenericYonSuiteAdapter     │  配置驱动适配器
│  - 读取 ErpConfig           │
│  - 获取 access_token        │
│  - 调用 YonSuite API        │
│  - 分页处理数据             │
└────────┬────────────────────┘
         ▼
┌─────────────────────────────┐
│  SalesOutMapper             │  数据映射
│  - API → ArcFileContent     │
└────────┬────────────────────┘
         ▼
┌─────────────────────────────┐
│  ArcFileContent 表          │  电子凭证池
│  - 状态: PENDING_CHECK      │
└─────────────────────────────┘
```

## Quick Start

### Prerequisites

1. **YonSuite 开通条件**：
   - 申请 appKey 和 appSecret
   - 配置回调地址（如使用 Webhook）
   - 开放相关 API 权限

2. **数据库配置**：
   ```sql
   INSERT INTO sys_erp_config (
       erp_name, erp_type, config_json, is_active
   ) VALUES (
       '用友 YonSuite',
       'yonsuite',
       '{"appKey":"your_app_key","appSecret":"your_app_secret"}',
       true
   );
   ```

3. **系统配置**（application.yml）：
   ```yaml
   yonsuite:
     base-url: https://dbox.yonyoucloud.com/iuap-api-gateway
     app-key: ${YONSUITE_APP_KEY:}
     app-secret: ${YONSUITE_APP_SECRET:}
   ```

### Basic Usage

#### 1. 同步销售出库单（最近 7 天）

```bash
# 获取 Token
TOKEN=$(curl -X POST "http://localhost:19090/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.data.token')

# 触发同步
curl -X POST "http://localhost:19090/api/yonsuite/generic/salesout/sync/recent?configId=1" \
  -H "Authorization: Bearer $TOKEN"
```

**Response**:
```json
{
  "code": 200,
  "message": "同步成功，共 5 条",
  "data": ["YonSuite_SALESOUT_123", "YonSuite_SALESOUT_456", ...],
  "timestamp": 1767522820299
}
```

#### 2. 同步指定日期范围

```bash
curl -X POST "http://localhost:19090/api/yonsuite/generic/salesout/sync" \
  -H "Authorization: Bearer $TOKEN" \
  -d "configId=1&startDate=2025-01-01&endDate=2025-01-31"
```

#### 3. 同步单个销售出库单详情

```bash
curl -X POST "http://localhost:19090/api/yonsuite/generic/salesout/detail" \
  -H "Authorization: Bearer $TOKEN" \
  -d "configId=1&salesOutId=123456"
```

## REST API Reference

### Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/yonsuite/generic/salesout/sync` | POST | 同步销售出库单列表 |
| `/api/yonsuite/generic/salesout/sync/recent` | POST | 快速同步（最近 7 天） |
| `/api/yonsuite/generic/salesout/detail` | POST | 同步单个销售出库单详情 |

### Authentication

所有接口需要 JWT Bearer Token：

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
```

### Response Format

**Success**:
```json
{
  "code": 200,
  "message": "同步成功，共 N 条",
  "data": [...],
  "timestamp": 1767522820299
}
```

**Error**:
```json
{
  "code": 500,
  "message": "同步失败: 具体错误信息",
  "timestamp": 1767522820299
}
```

## Configuration-Driven Architecture

本模块采用**配置驱动**而非代码生成的架构设计：

### 核心思想

```
配置数据库 → 读取配置 → 调用 API → 存储结果
```

**优势：**
- 一次实现，永久使用
- 不需要代码生成
- 配置变更无需重新部署
- 易于维护和扩展

### 实现细节

**GenericYonSuiteAdapter**:
```java
@Service
public class GenericYonSuiteAdapter {
    public List<String> syncSalesOutList(
        String appKey, String appSecret,
        String startDate, String endDate
    ) {
        // 1. 获取 access_token（自动缓存）
        String accessToken = yonAuthService.getAccessToken(appKey, appSecret);

        // 2. 分页调用 API
        while (hasMore) {
            SalesOutListResponse response = callSalesOutListApi(accessToken, request);
            // 3. 映射并保存
            for (SalesOutRecord record : response.getData().getRecordList()) {
                ArcFileContent fileContent = salesOutMapper.toPreArchiveFile(record);
                saveOrUpdateFileContent(fileContent);
            }
        }
    }
}
```

**SalesOutMapper**:
```java
@Component
public class SalesOutMapper {
    public ArcFileContent toPreArchiveFile(SalesOutListResponse.SalesOutRecord record) {
        String businessDocNo = "YonSuite_SALESOUT_" + record.getId();
        return ArcFileContent.builder()
                .id(businessDocNo)
                .businessDocNo(businessDocNo)
                .erpVoucherNo(record.getCode())
                .docDate(parseDate(record.getVouchdate()))
                .sourceSystem("YonSuite")
                .fileType("SALES_OUT")
                .preArchiveStatus("PENDING_CHECK")
                .summary("客户: " + record.getCustName() + ", 仓库: " + record.getWarehouseName())
                .sourceData(objectMapper.writeValueAsString(record))
                .build();
    }
}
```

## Data Model

### ArcFileContent Mapping

| YonSuite Field | ArcFileContent Field | Description |
|----------------|---------------------|-------------|
| `id` | `id`, `businessDocNo` | 业务唯一标识 |
| `code` | `erpVoucherNo` | ERP 凭证号 |
| `vouchdate` | `docDate` | 单据日期 |
| `cust_name` | `summary` | 客户名称（摘要） |
| `warehouse_name` | `summary` | 仓库名称（摘要） |
| `totalQuantity` | `sourceData` | 原始 JSON |
| - | `sourceSystem` | 固定值: "YonSuite" |
| - | `fileType` | 固定值: "SALES_OUT" |
| - | `preArchiveStatus` | 固定值: "PENDING_CHECK" |

### Source Data Structure

原始 API 响应存储在 `sourceData` 字段：

```json
{
  "id": "123456",
  "code": "SO202501001",
  "vouchdate": "2025-01-04 00:00:00",
  "cust_name": "某某客户",
  "warehouse_name": "主仓库",
  "totalQuantity": "100.0"
}
```

## Error Handling

### Common Errors

#### 1. Authentication Failed (401)

**Cause**: appKey/appSecret 错误或过期

**Solution**:
```sql
-- 更新配置
UPDATE sys_erp_config
SET config_json = '{"appKey":"new_key","appSecret":"new_secret"}'
WHERE id = 1;
```

#### 2. No Data Returned

**Cause**: 日期范围内无数据或 API 权限不足

**Solution**:
- 检查日期范围是否正确
- 确认 YonSuite API 权限已开通
- 使用 YonSuite 管理后台验证数据

#### 3. Token Cache Expired

**Cause**: Access Token 超时（默认 2 小时）

**Solution**: 系统自动刷新，无需手动处理

## Development Guide

### Adding New API Support

1. **创建 DTO**:
```java
// dto/NewApiRequest.java
@Data
public class NewApiRequest {
    @JsonProperty("pageIndex")
    private Integer pageIndex = 1;

    @JsonProperty("pageSize")
    private Integer pageSize = 100;
    // ...
}
```

2. **创建 Mapper**:
```java
// mapper/NewApiMapper.java
@Component
public class NewApiMapper {
    public ArcFileContent toPreArchiveFile(NewApiResponse.Record record) {
        // 映射逻辑
    }
}
```

3. **扩展 Adapter**:
```java
// service/GenericYonSuiteAdapter.java
public List<String> syncNewApi(String appKey, String appSecret, ...) {
    // 调用新 API
}
```

4. **添加 Controller**:
```java
// controller/GenericYonSuiteController.java
@PostMapping("/newapi/sync")
public Result<List<String>> syncNewApi(...) {
    // 调用 adapter
}
```

## Testing

### Unit Tests

```bash
cd nexusarchive-java
mvn test -Dtest=YonAuthServiceTest
mvn test -Dtest=SalesOutMapperTest
mvn test -Dtest=GenericYonSuiteAdapterTest
```

### Integration Tests

```bash
# 启动后端
SCHEMA_VALIDATION_FAIL=false DB_PORT=5432 mvn spring-boot:run

# 测试同步接口
TOKEN=$(curl -X POST "http://localhost:19090/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.data.token')

curl -X POST "http://localhost:19090/api/yonsuite/generic/salesout/sync/recent?configId=1" \
  -H "Authorization: Bearer $TOKEN"
```

## Troubleshooting

### Enable Debug Logging

**application.yml**:
```yaml
logging:
  level:
    com.nexusarchive.integration.yonsuite: DEBUG
    cn.hutool.http: DEBUG
```

### Check Token Cache

```sql
-- Redis 中查看 token
KEYS yonsuite:token:*
GET yonsuite:token:your_app_key
```

### Verify Data Mapping

```sql
-- 查看同步的数据
SELECT
    id,
    business_doc_no,
    erp_voucher_no,
    doc_date,
    pre_archive_status,
    source_system
FROM arc_file_content
WHERE source_system = 'YonSuite'
ORDER BY doc_date DESC
LIMIT 10;
```

## References

- [YonSuite OpenAPI 文档](https://help.yonyoucloud.com/doc)
- [电子凭证池设计](/docs/database/数据库设计.md)
- [ERP 适配器开发指南](/docs/development/erp-adapter-guide.md)

## Changelog

### v2.1.0 (2026-01-05)

- ✅ 新增退款单同步支持
- ✅ 新增 YonRefundListService
- ✅ 新增 YonRefundListRequest/Response
- ✅ 新增 YonRefundFileRequest/Response
- ✅ 支持退款单列表、详情、附件查询

### v2.0.0 (2026-01-04)

- ✅ 新增配置驱动架构
- ✅ 新增 GenericYonSuiteAdapter
- ✅ 新增 GenericYonSuiteController
- ✅ 新增销售出库单同步支持
- ✅ 新增 SalesOutMapper

### v1.0.0 (2025-12-20)

- ✅ 初始版本
- ✅ YonAuthService Token 认证
- ✅ 凭证同步支持
- ✅ Webhook 事件处理
