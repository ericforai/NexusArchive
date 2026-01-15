一旦我所属的文件夹有所变化，请更新我。
本目录存放 ERP 适配器实现。
所有适配器实现 ErpAdapter 统一接口。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `ErpAdapter.java` | 接口 | ERP 适配器统一接口 |
| `ErpAdapterFactory.java` | 工厂类 | 适配器工厂，根据类型创建实例 |
| `GenericErpAdapter.java` | 适配器 | 通用 ERP 适配器 |
| `KingdeeErpAdapter.java` | 适配器 | 金蝶云星空 ERP 适配器 |
| `SapErpAdapter.java` | 适配器 | SAP S/4HANA ERP 适配器 (OData V4) |
| `WeaverAdapter.java` | 适配器 | 泛微 Weaver ERP 适配器 |
| `WeaverE10Adapter.java` | 适配器 | 泛微 Weaver E10 ERP 适配器 |
| `YonSuiteErpAdapter.java` | 适配器 | 用友 YonSuite ERP 适配器 |

## 适配器说明

### SapErpAdapter

**用途**: SAP S/4HANA OData V4 集成适配器

**技术栈**:
- HTTP 客户端: Hutool HttpRequest
- 数据映射: ErpMapper + sap-mapping.yml
- 认证方式: Basic Auth

**支持场景**:
- VOUCHER_SYNC: 凭证同步
- ATTACHMENT_SYNC: 附件同步

**核心依赖**:
- `SapHttpClient`: OData API 调用
- `ErpMapper`: 字段映射转换
- `sap-mapping.yml`: 映射配置

### KingdeeErpAdapter

**用途**: 金蝶云星空 K3Cloud 集成适配器

**技术栈**:
- HTTP 客户端: Hutool HttpRequest
- 数据映射: ErpMapper + kingdee-mapping.yml
- 认证方式: ValidateUser (SessionId)

**支持场景**:
- VOUCHER_SYNC: 凭证同步
- ATTACHMENT_SYNC: 附件同步
- INVENTORY_SYNC: 存货核算同步
- EXPENSE_SYNC: 费用报销同步

### YonSuiteErpAdapter

**用途**: 用友 YonSuite 集成适配器

**技术栈**:
- HTTP 客户端: 专用客户端群
- 数据映射: ErpMapper + yonsuite-mapping.yml
- 认证方式: OAuth2 (access_token)

**支持场景**:
- VOUCHER_SYNC: 凭证同步
- ATTACHMENT_SYNC: 附件同步
- COLLECTION_FILE_SYNC: 收款单同步
- PAYMENT_FILE_SYNC: 付款单同步
- REFUND_FILE_SYNC: 退款单同步
- 支持 Webhook 推送
