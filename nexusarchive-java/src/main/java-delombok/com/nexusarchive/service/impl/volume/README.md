一旦我所属的文件夹有所变化，请更新我。
本目录存放案卷相关服务模块。
从 VolumeService (794行) 拆分出的专用模块，遵循单一职责原则。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `VolumeAssembler.java` | 组装器 | 按月自动组卷逻辑 (~105行) |
| `VolumeWorkflowService.java` | 工作流服务 | 审核流程（提交、审批、驳回、移交）(~115行) |
| `AipPackageExporter.java` | AIP包导出器 | AIP包导出（符合DA/T 94-2022）(~200行) |
| `VolumeQuery.java` | 查询器 | 案卷查询操作和登记表生成 (~100行) |
| `VolumePdfGenerator.java` | PDF生成器 | 生成占位PDF凭证 (~210行) |
| `VolumeUtils.java` | 工具类 | 通用工具方法 (~80行) |

## 模块化拆分说明

本目录服务是从 `VolumeService` (原794行) 拆分而成：

| 服务/工具 | 职责 | 原方法 |
|----------|------|--------|
| `VolumeAssembler` | 组卷逻辑 | `assembleByMonth()`, `generateVolumeCode()`, `calculateMaxRetention()` (~100行) |
| `VolumeWorkflowService` | 审核流程 | `submitForReview()`, `approveArchival()`, `rejectArchival()`, `handoverToArchives()` (~80行) |
| `AipPackageExporter` | AIP包导出 | `exportAipPackage()`, `generateIndexXml()`, `generateVolumeXml()`, `generateAuditXml()` (~240行) |
| `VolumeQuery` | 查询操作 | `getVolumeList()`, `getVolumeById()`, `getVolumeFiles()`, `generateRegistrationForm()` (~100行) |
| `VolumePdfGenerator` | PDF生成 | `generatePlaceholderPdf()`, 字体加载, 分录渲染 (~220行) |
| `VolumeUtils` | 工具方法 | `escapeXml()`, `zipDirectory()`, `deleteDirectoryRecursively()`, `truncateText()`, `safeText()` (~80行) |

## 依赖关系

```
VolumeService (协调层)
    ├── VolumeAssembler (组卷逻辑)
    │   └── VolumeUtils (工具方法)
    ├── VolumeWorkflowService (审核流程)
    ├── AipPackageExporter (AIP包导出)
    │   ├── VolumeQuery (查询操作)
    │   ├── VolumePdfGenerator (PDF生成)
    │   └── VolumeUtils (工具方法)
    └── VolumeQuery (查询操作)
        └── VolumeUtils (工具方法)
```

## 数据流

1. **VolumeAssembler** - 按月自动组卷，生成案卷并关联凭证
2. **VolumeWorkflowService** - 处理审核流程状态转换
3. **AipPackageExporter** - 导出符合 DA/T 94-2022 标准的 AIP 包
4. **VolumeQuery** - 提供案卷和卷内文件查询
5. **VolumePdfGenerator** - 为缺失的凭证生成占位 PDF
6. **VolumeUtils** - 提供通用工具方法
7. **VolumeService** - 协调所有模块，对上层提供统一接口

## 关键规范引用

- **DA/T 104-2024 第7.4节**: 组卷规范
- **DA/T 94-2022**: AIP 包标准
- **GB/T 39674**: 电子档案长期保存格式要求
- **GB/T 18894-2016 附录 A**: 归档登记表格式
