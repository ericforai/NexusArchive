一旦我所属的文件夹有所变化，请更新我。
本目录存放历史数据导入相关服务模块。
从 LegacyImportServiceImpl (722行) 拆分出的专用模块，遵循单一职责原则。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `LegacyFileParser.java` | 文件解析器 | 解析 CSV/Excel 文件 (~190行) |
| `LegacyDataConverter.java` | 数据转换器 | 将 ImportRow 转换为 Archive 并导入 (~100行) |
| `LegacyImportOrchestrator.java` | 导入编排器 | 编排导入流程（解析、验证、创建全宗、导入、日志）(~280行) |
| `LegacyImportUtils.java` | 工具类 | 通用工具方法 (~85行) |

## 模块化拆分说明

本目录服务是从 `LegacyImportServiceImpl` (原722行) 拆分而成：

| 服务/工具 | 职责 | 原方法 |
|----------|------|--------|
| `LegacyFileParser` | 文件解析 | `parseFile()`, `parseExcel()`, `parseCsv()`, `parseRowFromExcel()`, `parseRowFromCsv()` (~180行) |
| `LegacyDataConverter` | 数据转换 | `batchImportArchives()`, `convertToArchive()` (~75行) |
| `LegacyImportOrchestrator` | 流程编排 | `executeImport()`, `previewImport()`, `generateErrorReport()` (~180行) |
| `LegacyImportUtils` | 工具方法 | `buildImportRow()`, `getStringValue()`, `getIntegerValue()`, `getBigDecimalValue()`, `getDateValue()`, `getCurrentUserName()` (~85行) |

## 依赖关系

```
LegacyImportServiceImpl (协调层)
    └── LegacyImportOrchestrator (导入编排)
        ├── LegacyFileParser (文件解析)
        ├── LegacyDataConverter (数据转换)
        └── LegacyImportUtils (工具方法)
```

## 数据流

1. **LegacyFileParser** - 解析 CSV/Excel 文件为 ImportRow 列表
2. **LegacyImportOrchestrator** - 编排导入流程
3. **LegacyDataConverter** - 转换 ImportRow 为 Archive 并导入
4. **LegacyImportUtils** - 提供通用工具方法
5. **LegacyImportServiceImpl** - 协调所有模块，对上层提供统一接口

## 关键规范引用

- **OpenSpec**: openspec-legacy-data-import.md
- **文件格式**: 支持 CSV 和 Excel (xls/xlsx)
- **导入流程**: 解析 → 验证 → 创建全宗/实体 → 导入档案 → 记录日志
- **批量处理**: 每批 1000 条记录
