一旦我所属的文件夹有所变化，请更新我。
本目录存放 ERP 集成相关业务服务。
从 ErpScenarioService 拆分出的专用服务，遵循单一职责原则。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `ErpChannelService.java` | 业务服务 | ERP 集成通道聚合服务（250行） |
| `ErpSyncService.java` | 业务服务 | ERP 同步执行服务（493行） |
| `VoucherMapper.java` | 工具类 | ERP 凭证 DTO 到档案文件内容的映射器（73行） |

## 模块化拆分说明

本目录服务是从 `ErpScenarioService` (原799行) 拆分而成：

| 服务 | 职责 | 原方法 |
|------|------|--------|
| `ErpSyncService` | 同步执行 | `syncScenario()`, `isVoucherExist()`, `createArchiveFromVoucher()` |
| `ErpChannelService` | 通道聚合 | `listAllChannels()`, `extractReceivedCount()`, `cronToHuman()` 等 |
| `VoucherMapper` | 数据转换 | `toArcFileContent()` (原内部类) |

## 依赖关系

```
ErpScenarioService (协调层)
    ├── ErpSyncService (同步执行)
    ├── ErpChannelService (通道聚合)
    └── ErpSubInterfaceService (子接口管理)
```