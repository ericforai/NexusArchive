一旦我所属的文件夹有所变化，请更新我。
本目录存放对账相关服务模块。
从 ReconciliationServiceImpl (991行) 拆分出的专用模块，遵循单一职责原则。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `ErpDataFetcher.java` | 数据获取器 | ERP 数据获取（科目汇总、凭证数量）(~170行) |
| `ArchiveAggregator.java` | 数据聚合器 | 档案数据聚合（科目模式、凭证模式）(~200行) |
| `EvidenceVerifier.java` | 验证器 | 证据链完整性验证（标准文件、哈希、签名）(~150行) |
| `SubjectExtractor.java` | 数据提取器 | 从元数据提取科目分录(~120行) |
| `ReconciliationUtils.java` | 工具类 | 对账相关工具方法(~80行) |

## 模块化拆分说明

本目录服务是从 `ReconciliationServiceImpl` (原991行) 拆分而成：

| 服务/工具 | 职责 | 原方法 |
|----------|------|--------|
| `ErpDataFetcher` | ERP数据获取 | `fetchErpSummary()`, `fetchErpVoucherCount()`, `convertToDto()` (~120行) |
| `ArchiveAggregator` | 档案数据聚合 | `fetchArchives()`, `aggregateArchives()`, `aggregateArchivesForVoucherOnly()` (~180行) |
| `EvidenceVerifier` | 证据链验证 | `verifyEvidence()`, `isStandardFile()`, `hasHash()`, `hasSignature()` (~150行) |
| `SubjectExtractor` | 科目分录提取 | `extractSubjectAggregation()`, `extractSubjectCode()`, `readBigDecimal()` (~120行) |
| `ReconciliationUtils` | 工具方法 | `buildPeriods()`, `hasText()`, `normalizeSubjectCode()` 等 (~80行) |

## 依赖关系

```
ReconciliationServiceImpl (协调层)
    ├── ErpDataFetcher (ERP数据获取)
    ├── ArchiveAggregator (档案数据聚合)
    │   └── SubjectExtractor (科目分录提取)
    ├── EvidenceVerifier (证据链验证)
    └── ReconciliationUtils (工具方法)
```

## 数据流

1. **ErpDataFetcher** - 从 ERP 系统获取科目汇总/凭证数据
2. **ArchiveAggregator** - 从数据库获取档案并聚合，内部使用 **SubjectExtractor** 解析科目分录
3. **EvidenceVerifier** - 验证档案的原始证据文件
4. **ReconciliationServiceImpl** - 协调整个核对流程，聚合结果并保存
