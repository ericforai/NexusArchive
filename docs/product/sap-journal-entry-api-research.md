# SAP Journal Entry API 调研文档

> **调研日期**: 2026-01-09
> **目的**: 了解 SAP S/4HANA Journal Entry API 完整信息，为后续凭证同步功能提供技术参考
> **状态**: 调研完成，待实现

---

## 一、官方文档来源

| 文档名称 | URL | 说明 |
|----------|-----|------|
| APIs for Journal Entries – The Collection | [链接](https://community.sap.com/t5/technology-blog-posts-by-sap/apis-for-journal-entries-the-collection-updated-july-2025/ba-p/13565258) | 综合API集合，2025年7月更新 |
| Operational Journal Entry Item - Read (A2X) | [链接](https://help.sap.com/docs/SAP_S4HANA_CLOUD/b978f98fc5884ff2aeb10c8fdeb8a43b/c024170fa7af40878975e218f3426387.html) | OData读取服务主文档 |
| Operations for Journal Entry Item - Read (A2X) | [链接](https://help.sap.com/docs/SAP_S4HANA_CLOUD/b978f98fc5884ff2aeb10c8fdeb8a43b/8aa29c6ac8234f9a9b975b3900aa002d.html) | 操作说明和URL模式 |
| Journal Entry Item - Read (A2X) | [链接](https://help.sap.com/docs/SAP_S4HANA_ON-PREMISE/3ab6e6fc510f4840a5508e126ef01e22/c2e849c5ff5341388dff293e07a4549d.html) | On-Premise版本文档 |
| ODATAL API: Operational Journal Entry Item - Read | [链接](https://help.sap.com/docs/SAP_S4HANA_ON-PREMISE/f296651f454c4284ade361292c633d69/1b8d583467cf4ebf829fedb953e39668.html) | OData API详细说明 |

---

## 二、支持的 SAP 产品版本

| 产品类型 | 版本支持 | 部署方式 |
|----------|----------|----------|
| **SAP S/4HANA Cloud** | ✅ 全版本 | 公有云 |
| **SAP S/4HANA** | ✅ 2020+ | 私有部署/On-Premise |

---

## 三、Journal Entry API 类型对比

### 3.1 写入类 API (外部系统 → SAP)

| API 名称 | 协议 | 服务名称 | 用途 |
|----------|------|----------|------|
| Journal Entry - Post (Synchronous) | SOAP | `API_JOURNAL_ENTRY` | 同步写入凭证 |
| Journal Entry - Post (Asynchronous) | SOAP | `API_JOURNAL_ENTRY` | 异步写入凭证 |
| Journal Entry - Change (Asynchronous) | SOAP | `API_JOURNAL_ENTRY` | 修改现有凭证 |
| Journal Entry – Clearing (Asynchronous) | SOAP | `API_JOURNAL_ENTRY` | 清账操作 |
| Journal Entry by Ledger - Post (Asynchronous) | SOAP | `API_JOURNAL_ENTRY` | 按分类账写入凭证 |

### 3.2 读取类 API (SAP → 外部系统)

| API 名称 | 协议 | 服务名称 | 用途 |
|----------|------|----------|------|
| **Operational Journal Entry Item - Read (A2X)** | **OData V4** | `API_OPLACCTGDOCITEMCUBE_SRV` | **读取凭证数据到外部系统** |
| Journal Entry Item - Read (A2X) | OData | `API_JOURNAL_ENTRY_ITEM_SRV` | 读取凭证项目 |

> **关键发现**: SAP 的 Journal Entry API 采用明确的协议分工：
> - **写入**操作使用 SOAP 协议
> - **读取**操作使用 OData 协议

---

## 四、OData 读取服务详解 (用于凭证同步)

### 4.1 服务信息

```
服务名称: API_OPLACCTGDOCITEMCUBE_SRV
API 名称: Operational Journal Entry Item - Read (A2X)
协议: OData V4
用途: 将操作性凭证项目数据提取到外部系统
```

### 4.2 端点 URL 格式

```
基础URL: /sap/opu/odata4/sap/API_OPLACCTGDOCITEMCUBE_SRV
实体集: AccountingDocumentItem
```

完整示例：
```
https://<your-host>/sap/opu/odata4/sap/API_OPLACCTGDOCITEMCUBE_SRV/AccountingDocumentItem
```

### 4.3 服务限制

根据 SAP 官方文档：

| 限制项 | 说明 |
|--------|------|
| 数据范围 | 仅提取具有 entry view 的凭证 |
| 数据量 | **不适合**大数据量提取 |
| 适用场景 | Fiori 应用和其他用户界面消费 |

---

## 五、认证方式

### 5.1 On-Premise 版本

| 认证类型 | 说明 |
|----------|------|
| **Basic Authentication** | 用户名 + 密码 |
| Client Number | SAP 客户端编号 (例如: 000, 100) |

### 5.2 Cloud 版本

| 认证类型 | 说明 |
|----------|------|
| OAuth 2.0 | 标准OAuth2流程 |
| API Key | 可能需要额外的API密钥 |

---

## 六、SAP 侧配置步骤

### 6.1 激活 OData 服务

1. **启动事务代码**: `/n/IWFND/MAINT_SERVICE`
2. **激活服务**: 选择 `API_OPLACCTGDOCITEMCUBE_SRV`
3. **分配服务到 ICF 节点**

### 6.2 创建 Service Binding

1. **启动事务代码**: `/n/IWFND/SB_BASE`
2. **创建绑定**: 选择 `API_OPLACCTGDOCITEMCUBE_SRV`
3. **绑定类型**: OData V4
4. **激活绑定**

### 6.3 配置通信用户

1. **创建通信用户**: 事务代码 `SU01`
2. **分配角色**: 需要至少 `SAP_BR_CONFIG_EXPERT` 角色
3. **配置通信系统**: 事务代码 `/n/SMT1_CUST`
4. **配置通信场景**: SAP_COM_0002 (Posting Integration)

### 6.4 测试服务

使用 **Postman** 或 **SAP Gateway Client** 测试：

```
GET /sap/opu/odata4/sap/API_OPLACCTGDOCITEMCUBE_SRV/AccountingDocumentItem?$top=10
Authorization: Basic <base64(username:password)>
```

---

## 七、数据结构概述

### 7.1 主要实体

```
AccountingDocumentItem (凭证项目)
├── CompanyCode (公司代码)
├── FiscalYear (会计年度)
├── AccountingDocument (凭证编号)
├── AccountingDocumentItem (凭证行号)
├── PostingDate (过账日期)
├── DocumentDate (凭证日期)
├── AmountInTransactionCurrency (金额)
├── TransactionCurrency (币种)
├── DebitCreditCode (借贷标识)
├── AccountNumber (科目编号)
├── AccountName (科目名称)
├── CostCenter (成本中心)
├── ProfitCenter (利润中心)
└── ...
```

> **注意**: 具体字段结构需要参考 SAP 官方 OData 元数据文档 ($metadata)

---

## 八、集成架构设计

### 8.1 整体流程

```
┌─────────────────────────────────────────────────────────────────┐
│                      SAP S/4HANA                                │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  OData Service: API_OPLACCTGDOCITEMCUBE_SRV               ││
│  │  Endpoint: /sap/opu/odata4/sap/.../AccountingDocumentItem ││
│  └─────────────────────────────────────────────────────────────┘│
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP/JSON (OData V4)
                             ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    NexusArchive (归档系统)                          │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  SapODataClient (HTTP 客户端)                               │  │
│  │  - Basic 认证                                              │  │
│  │  - OData V4 查询构建                                        │  │
│  │  - 分页处理                                                 │  │
│  └───────────────────────┬───────────────────────────────────────┘  │
│                          ↓                                          │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  SapJournalEntryMapper (数据映射)                           │  │
│  │  SAP 字段 → 归档系统字段                                     │  │
│  └───────────────────────┬───────────────────────────────────────┘  │
│                          ↓                                          │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  SapVoucherSyncService (同步服务)                           │  │
│  │  - 定时调度                                                 │  │
│  │  - 增量同步 (按日期/凭证号)                                  │  │
│  │  - 同步历史记录                                             │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

### 8.2 前端配置界面 (已实现)

```
┌─────────────────────────────────────────────────────────────────┐
│  ConnectorForm - SAP S/4HANA 配置                               │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ ERP 类型: SAP S/4HANA                                       ││
│  │ 服务地址: https://<sap-host>/                              ││
│  │ 应用ID: <client_id>                                        ││
│  │ 应用密钥: <client_secret>                                   ││
│  │                                                             ││
│  │ 接口类型: OData 服务 ◀── 已选择                             ││
│  │                                                             ││
│  │ ┌─ OData 服务配置 ────────────────────────────────────────┐ ││
│  │ │ 服务端点: /sap/opu/odata4/sap/...                      │ ││
│  │ │ 认证方式: Basic                                         │ ││
│  │ │ 技术用户名: <username>                                  │ ││
│  │ │ 密码: ********                                          │ ││
│  │ │ 客户端编号: 800 (on-premise only)                      │ ││
│  │ │ 测试服务: API_OPLACCTGDOCITEMCUBE_SRV                   │ ││
│  │ └───────────────────────────────────────────────────────────┘ ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

---

## 九、后续实现待办事项

### 9.1 SAP 侧配置

- [ ] 在 SAP 系统中激活 OData 服务
- [ ] 创建 Service Binding
- [ ] 配置通信用户和权限
- [ ] 测试 OData 端点可访问性

### 9.2 后端实现

- [ ] 实现 `SapODataClient` (已完成基础HTTP客户端)
- [ ] 实现 `SapJournalEntryMapper` 数据映射
- [ ] 实现 `SapVoucherSyncService` 同步服务
- [ ] 实现定时任务调度
- [ ] 实现同步历史记录

### 9.3 前端实现

- [ ] 同步历史记录页面
- [ ] 同步状态监控
- [ ] 错误日志查看

### 9.4 测试

- [ ] 单元测试 (Mapper, Service)
- [ ] 集成测试 (端到端)
- [ ] 性能测试 (大数据量)

---

## 十、参考资料

### 10.1 SAP 官方文档

- [SAP Help Portal - OData APIs](https://help.sap.com/docs/)
- [SAP Community - Integration Blogs](https://community.sap.com/t5/technology-blog-posts-by-sap/)
- [SAP API Business Hub](https://api.sap.com/)

### 10.2 技术博客

- [How to setup Web Service Configuration for Journal Entry API](https://community.sap.com/t5/enterprise-resource-planning-blog-posts-by-sap/how-to-setup-the-web-service-configuration-of-journal-entry-api-for-s-4/ba-p/13474501)
- [Guidelines for API Journal Entry – Post](https://community.sap.com/t5/enterprise-resource-planning-blog-posts-by-sap/guidelines-for-api-journal-entry-post/ba-p/13421397)

### 10.3 相关代码文件

| 文件路径 | 说明 |
|----------|------|
| `src/components/settings/integration/components/SapInterfaceConfigForm.tsx` | SAP OData 配置表单 (已实现) |
| `src/components/settings/integration/types.ts` | SAP 接口类型定义 (已实现) |
| `java/.../integration/sap/SapODataClient.java` | SAP OData HTTP 客户端 (部分实现) |
| `java/.../integration/sap/dto/JournalEntryDto.java` | 凭证 DTO (已定义) |

---

**文档维护**: 当 SAP API 有更新时，请及时更新本文档。
