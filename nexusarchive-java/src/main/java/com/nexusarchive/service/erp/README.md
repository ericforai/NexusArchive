一旦我所属的文件夹有所变化，请更新我。
本目录存放 ERP 集成相关业务服务。
从 ErpScenarioService 拆分出的专用服务，遵循单一职责原则。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `AsyncErpSyncService.java` | 业务服务 | ERP 同步异步执行 |
| `ErpChannelService.java` | 业务服务 | ERP 集成通道聚合服务 |
| `ErpConfigDtoBuilder.java` | 工具类 | ERP 配置 DTO 构建 |
| `ErpSyncService.java` | 业务服务 | ERP 同步执行服务（含凭证 JSON 预存、路由校验 Guard Clause） |
| `SyncDateRangeExtractor.java` | 工具类 | 同步日期范围提取 |
| `SyncTaskCleanupService.java` | 业务服务 | 同步任务清理 |
| `VoucherFetcher.java` | 业务服务 | 凭证数据获取 |
| `VoucherMapper.java` | 工具类 | ERP 凭证 DTO 到档案文件内容的映射器 |
| `VoucherPersistenceService.java` | 业务服务 | 凭证持久化与 PDF 生成协调 |
| `plugin/` | 目录入口 | ERP 插件扩展 |

## 模块化拆分说明

本目录服务是从 `ErpScenarioService` (原799行) 拆分而成：

| 服务 | 职责 | 原方法 |
|------|------|--------|
| `ErpSyncService` | 同步执行 | `syncScenario()`, `processVouchers()` |
| `ErpChannelService` | 通道聚合 | `listAllChannels()`, `extractReceivedCount()`, `cronToHuman()` 等 |
| `VoucherFetcher` | 数据获取 | `fetchVouchers()` |
| `VoucherPersistenceService` | 持久化 | `saveVoucher()` |
| `VoucherMapper` | 数据转换 | `toArcFileContent()` (原内部类) |

## 依赖关系

```
ErpScenarioService (协调层)
    ├── ErpSyncService (同步执行)
    │   ├── VoucherFetcher (数据获取)
    │   └── VoucherPersistenceService (持久化)
    ├── ErpChannelService (通道聚合)
    └── ErpSubInterfaceService (子接口管理)
```
