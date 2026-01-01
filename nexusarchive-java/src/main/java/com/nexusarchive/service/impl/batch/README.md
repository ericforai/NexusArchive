一旦我所属的文件夹有所变化，请更新我。
本目录存放归档批次相关服务模块。
从 ArchiveSubmitBatchServiceImpl (779行) 拆分出的专用模块，遵循单一职责原则。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `BatchManager.java` | 批次管理器 | 批次的创建、查询、删除 (~140行) |
| `BatchItemManager.java` | 条目管理器 | 批次条目的添加、移除、查询 (~140行) |
| `BatchWorkflowService.java` | 工作流服务 | 提交、审批、驳回、执行归档 (~150行) |
| `FourNatureChecker.java` | 四性检测器 | 真实性、完整性、可用性、安全性检测 (~310行) |

## 模块化拆分说明

本目录服务是从 `ArchiveSubmitBatchServiceImpl` (原779行) 拆分而成：

| 服务/工具 | 职责 | 原方法 |
|----------|------|--------|
| `BatchManager` | 批次管理 | `createBatch()`, `getBatch()`, `listBatches()`, `deleteBatch()` (~80行) |
| `BatchItemManager` | 条目管理 | `addVouchersToBatch()`, `addDocsToBatch()`, `removeItemFromBatch()`, `getBatchItems()` (~130行) |
| `BatchWorkflowService` | 工作流程 | `submitBatch()`, `approveBatch()`, `rejectBatch()`, `executeBatchArchive()` (~130行) |
| `FourNatureChecker` | 四性检测 | `validateBatch()`, `runIntegrityCheck()`, `checkAuthenticity()`, `checkIntegrity()`, `checkUsability()`, `checkSecurity()` (~310行) |

## 依赖关系

```
ArchiveSubmitBatchServiceImpl (协调层)
    ├── BatchManager (批次管理)
    ├── BatchItemManager (条目管理)
    ├── BatchWorkflowService (工作流程)
    │   └── FourNatureChecker (四性检测)
    └── FourNatureChecker (四性检测)
```

## 数据流

1. **BatchManager** - 创建和管理归档批次
2. **BatchItemManager** - 管理批次中的凭证和单据条目
3. **BatchWorkflowService** - 处理批次的提交流程、审批流程、执行归档
4. **FourNatureChecker** - 执行四性检测（真实性、完整性、可用性、安全性）
5. **ArchiveSubmitBatchServiceImpl** - 协调所有模块，对上层提供统一接口

## 关键规范引用

- **四性检测**: 真实性、完整性、可用性、安全性
- **归档流程**: 创建 → 添加条目 → 提交 → 校验 → 审批 → 执行归档
- **期间锁定**: 归档后锁定期间，防止重复操作
