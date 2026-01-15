一旦我所属的文件夹有所变化，请更新我。
本目录存放 ERP 适配器 HTTP 客户端。
负责各 ERP 系统的 API 调用、认证和会话管理。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `KingdeeVoucherClient.java` | 客户端 | 金蝶云星空凭证客户端 (会话认证、凭证查询) |
| `SapHttpClient.java` | 客户端 | SAP S/4HANA OData 客户端 (Basic 认证、OData 查询) |
| `YonSuiteAuthClient.java` | 客户端 | 用友 YonSuite 认证客户端 |
| `YonSuiteCollectionClient.java` | 客户端 | 用友 YonSuite 收款单客户端 |
| `YonSuiteFeedbackClient.java` | 客户端 | 用友 YonSuite 回写客户端 |
| `YonSuitePaymentClient.java` | 客户端 | 用友 YonSuite 付款单客户端 |
| `YonSuiteRefundClient.java` | 客户端 | 用友 YonSuite 退款单客户端 |
| `YonSuiteVoucherClient.java` | 客户端 | 用友 YonSuite 凭证客户端 |

## 客户端说明

### SapHttpClient

**用途**: SAP S/4HANA OData HTTP 客户端

**技术栈**: Hutool HttpRequest + Jackson ObjectMapper

**核心方法**:
| 方法 | 说明 |
|------|------|
| `buildQueryUrl()` | 构建 OData 查询 URL (带日期过滤) |
| `buildDetailUrl()` | 构建凭证详情 URL (含 $expand) |
| `queryJournalEntries()` | 查询凭证列表 |
| `getJournalEntryDetail()` | 获取凭证详情 (展开分录和附件) |
| `mapDebitCreditCode()` | 映射 SAP 借贷码 (S=借, H=贷) |

**OData 服务路径**: `/sap/opu/odata4/sap/api_journal_entry/srvd_a2x/sap/journal_entry/0001`

### KingdeeVoucherClient

**用途**: 金蝶云星空凭证客户端

**技术栈**: Hutool HttpRequest + Jackson ObjectMapper

**认证方式**: ValidateUser (会话 ID)

**核心方法**:
| 方法 | 说明 |
|------|------|
| `authenticate()` | 认证获取 SessionId |
| `syncVouchers()` | 查询凭证列表 |
| `getVoucherDetail()` | 查询凭证详情 |

### YonSuite 客户端群

**用途**: 用友 YonSuite 各业务模块客户端

**认证方式**: OAuth2 (access_token)

**客户端分工**:
- `YonSuiteAuthClient`: 统一认证，获取 access_token
- `YonSuiteVoucherClient`: 凭证查询和同步
- `YonSuiteCollectionClient`: 收款单文件获取
- `YonSuitePaymentClient`: 付款单文件获取
- `YonSuiteRefundClient`: 退款单文件获取
- `YonSuiteFeedbackClient`: 归档状态回写
