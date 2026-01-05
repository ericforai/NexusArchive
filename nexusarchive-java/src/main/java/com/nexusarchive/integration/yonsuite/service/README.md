# YonSuite Service Layer

> **Updated**: 2026-01-04

## Overview

本目录包含 YonSuite 集成的核心业务服务层。

## Architecture

### Service Layer Design

```
┌─────────────────────────────────────────────────────────────┐
│                    Controller Layer                          │
│  GenericYonSuiteController / YonPaymentTestController       │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                     Service Layer                            │
│  ┌──────────────────┐  ┌──────────────────────────────────┐ │
│  │ YonAuthService   │  │  GenericYonSuiteAdapter          │ │
│  │                  │  │  - syncSalesOutList()            │ │
│  │ - getAccessToken │  │  - syncSalesOutDetail()          │ │
│  │ - 签名验证        │  │  - 分页处理                      │ │
│  └──────────────────┘  └──────────────────────────────────┘ │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                     Mapper Layer                             │
│  SalesOutMapper / YonVoucherMapper                           │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                  Database / External API                     │
│  YonSuite API / ArcFileContent Table                         │
└─────────────────────────────────────────────────────────────┘
```

## Services

### 0. 凭证附件查询 API ⚠️

> **重要说明**: 这是一个 **查询工具**，不是同步场景！
> 请通过 REST API 直接调用，不要在"在线接收"页面使用同步按钮。

| Method | Endpoint | Description |
|--------|----------|-------------|
| `queryVoucherAttachments(appKey, appSecret, businessIds)` | `POST /api/yonsuite/generic/voucher/attachments?configId={id}` | 批量查询凭证附件 |

**调用示例**：

```bash
# 后端 API 调用
curl -X POST "http://localhost:19090/api/yonsuite/generic/voucher/attachments?configId=1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '["voucher_id_1", "voucher_id_2", "voucher_id_3"]'
```

```typescript
// 前端调用示例
import { yonsuiteApi } from '@/api/yonsuite';

const attachments = await yonsuiteApi.queryVoucherAttachments(
  1,  // configId
  ['voucher_id_1', 'voucher_id_2']
);

// 返回格式: Record<string, VoucherAttachment[]>
// {
//   "voucher_id_1": [{ fileId: "xxx", fileName: "xxx.pdf", ... }],
//   "voucher_id_2": [{ fileId: "yyy", fileName: "yyy.jpg", ... }]
// }
```

**返回数据结构**：

```typescript
interface VoucherAttachment {
  fileId: string;        // 文件ID
  filePath: string;      // 文件路径
  fileName: string;      // 文件名
  fileExtension: string; // 文件扩展名
  fileSize: number;      // 文件大小
  ctime: number;         // 创建时间戳
  utime: number;         // 更新时间戳
}
```

**使用场景**：
- 在凭证详情页面展示附件
- 在凭证列表显示附件数量
- 下载指定凭证的附件

**注意**：
- ❌ 不要在"在线接收"页面使用（同步按钮无效）
- ✅ 直接通过 API 调用
- ✅ 返回的是查询结果，不会保存到数据库

### 1. YonAuthService

**职责**：YonSuite Token 认证服务

| Method | Description |
|--------|-------------|
| `getAccessToken(appKey, appSecret)` | 获取 Access Token（自动缓存） |
| `refreshToken(appKey, appSecret)` | 刷新 Token |
| `validateSignature(signature, timestamp, appSecret)` | 验证 HMAC-SHA256 签名 |

**Token 缓存策略**：
- 使用 Redis 缓存
- 默认有效期：2 小时
- Key 格式：`yonsuite:token:{appKey}`

### 2. GenericYonSuiteAdapter ⭐

**职责**：配置驱动的通用适配器

| Method | Description |
|--------|-------------|
| `syncSalesOutList(appKey, appSecret, startDate, endDate)` | 同步销售出库单列表 |
| `syncSalesOutDetail(appKey, appSecret, salesOutId)` | 同步单个销售出库单详情 |

**核心特性**：
- ✅ 分页自动处理（默认每页 100 条）
- ✅ 自动重试（失败跳过，继续下一条）
- ✅ 数据幂等性（基于 businessDocNo 去重）
- ✅ 原始数据保存（sourceData 字段）

**实现流程**：

```
1. 获取 access_token
   ↓
2. 构建请求（pageIndex, pageSize, 日期范围）
   ↓
3. 调用 YonSuite API
   ↓
4. 解析响应
   ↓
5. 映射到 ArcFileContent
   ↓
6. 保存到数据库
   ↓
7. 下一页（或结束）
```

### 3. YonRefundListService ⭐ (v2.1 新增)

**职责**：退款单列表查询服务

| Method | Description |
|--------|-------------|
| `getRefundList(appKey, appSecret, request)` | 查询退款单列表 |
| `getRefundDetail(appKey, appSecret, refundId)` | 查询退款单详情 |
| `getRefundFiles(appKey, appSecret, refundId)` | 查询退款单附件 |

**API 端点**:
- 列表: `/yonbip/arap/refund/list`
- 详情: `/yonbip/arap/refund/detail`
- 附件: `/yonbip/EFI/rest/v1/openapi/queryBusinessFiles`

### 4. YonPaymentListService

**职责**：付款单列表查询服务

### 5. YonPaymentFileService

**职责**：付款单文件处理服务

### 6. YonSuiteVoucherSyncService

**职责**：凭证同步服务（旧版，推荐使用 GenericYonSuiteAdapter）

## Usage Example

### GenericYonSuiteAdapter 使用示例

```java
@Service
public class SyncSchedulerService {
    private final GenericYonSuiteAdapter adapter;
    private final ErpConfigService configService;

    // 每天凌晨同步前一天数据
    @Scheduled(cron = "0 0 1 * * ?")
    public void syncYesterdayData() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String date = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 获取配置
        ErpConfig config = configService.findById(1L);
        JSONObject configJson = JSONUtil.parseObj(config.getConfigJson());
        String appKey = configJson.getStr("appKey");
        String appSecret = configJson.getStr("appSecret");

        // 执行同步
        List<String> syncedIds = adapter.syncSalesOutList(
            appKey, appSecret, date, date
        );

        log.info("同步完成，共 {} 条", syncedIds.size());
    }
}
```

### Controller 调用示例

```java
@RestController
@RequestMapping("/yonsuite/generic")
public class GenericYonSuiteController {
    private final GenericYonSuiteAdapter adapter;
    private final ErpConfigService configService;

    @PostMapping("/salesout/sync/recent")
    public Result<List<String>> syncRecentSalesOut(@RequestParam Long configId) {
        // 获取配置
        ErpConfig config = configService.findById(configId);
        JSONObject configJson = JSONUtil.parseObj(config.getConfigJson());
        String appKey = configJson.getStr("appKey");
        String appSecret = configJson.getStr("appSecret");

        // 计算日期范围（最近 7 天）
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(7);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 执行同步
        List<String> syncedIds = adapter.syncSalesOutList(
            appKey, appSecret,
            sevenDaysAgo.format(formatter),
            today.format(formatter)
        );

        return Result.success("同步成功，共 " + syncedIds.size() + " 条", syncedIds);
    }
}
```

## Error Handling

### 重试策略

```java
private String processSalesOutRecord(SalesOutListResponse.SalesOutRecord record) {
    try {
        ArcFileContent fileContent = salesOutMapper.toPreArchiveFile(record);
        return saveOrUpdateFileContent(fileContent);
    } catch (Exception e) {
        log.error("处理销售出库单记录失败: {}", record.getId(), e);
        return null;  // 失败跳过，继续下一条
    }
}
```

### 日志记录

```java
log.info("开始同步销售出库单列表: {} - {}", startDate, endDate);
log.info("第 {} 页处理完成，本页 {} 条", pageIndex, records.size());
log.error("同步销售出库单失败", e);
```

## Configuration

### application.yml

```yaml
yonsuite:
  base-url: https://dbox.yonyoucloud.com/iuap-api-gateway
  app-key: ${YONSUITE_APP_KEY:}
  app-secret: ${YONSUITE_APP_SECRET:}
```

### Redis 配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 3000ms
```

## File List

| 文件 | 状态 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `YonAuthService.java` | ✅ 服务 | Token 认证服务 |
| `GenericYonSuiteAdapter.java` | ✅ 服务 | 通用适配器（v2.0 新增） |
| `YonRefundListService.java` | ✅ 服务 | 退款单列表服务（v2.1 新增） |
| `YonPaymentFileService.java` | ⚠️ 旧版 | 付款单文件服务 |
| `YonPaymentListService.java` | ⚠️ 旧版 | 付款单列表服务 |
| `YonSuiteVoucherSyncService.java` | ⚠️ 旧版 | 凭证同步服务 |

## Migration Notes

### 从旧版迁移到 GenericYonSuiteAdapter

**旧版代码**:
```java
@Autowired
private YonSuiteVoucherSyncService voucherSyncService;

List<VoucherDTO> vouchers = voucherSyncService.syncVouchers(config, start, end);
```

**新版代码**:
```java
@Autowired
private GenericYonSuiteAdapter adapter;

List<String> ids = adapter.syncSalesOutList(appKey, appSecret, startDate, endDate);
```

**优势**：
- 配置驱动，无需修改代码
- 支持分页自动处理
- 直接保存到 ArcFileContent
- 支持 HTTP 请求级别签名

## Testing

### Unit Tests

```bash
mvn test -Dtest=YonAuthServiceTest
mvn test -Dtest=GenericYonSuiteAdapterTest
```

### Integration Tests

```bash
# 启动后端
mvn spring-boot:run

# 测试同步接口
curl -X POST "http://localhost:19090/api/yonsuite/generic/salesout/sync/recent?configId=1" \
  -H "Authorization: Bearer $TOKEN"
```

## References

- [YonSuite 集成模块文档](/nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/README.md)
- [ERP 适配器开发指南](/docs/development/erp-adapter-guide.md)
