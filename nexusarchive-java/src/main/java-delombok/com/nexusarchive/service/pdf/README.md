一旦我所属的文件夹有所变化，请更新我。
本目录存放 PDF 生成相关服务。
从 VoucherPdfGeneratorService (1058行) 拆分出的专用生成器，遵循单一职责原则。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `PaymentPdfGenerator.java` | PDF 生成器 | 付款单 PDF 生成器 (~260行) |
| `CollectionPdfGenerator.java` | PDF 生成器 | 收款单 PDF 生成器 (~210行) |
| `VoucherPdfGenerator.java` | PDF 生成器 | 会计凭证 PDF 生成器 (~370行) |
| `PdfDataParser.java` | 工具类 | JSON 数据解析工具 (~110行) |
| `PdfFontLoader.java` | 工具类 | 中文字体加载器 (~55行) |
| `PdfUtils.java` | 工具类 | PDF 工具方法 (~75行) |

## 模块化拆分说明

本目录服务是从 `VoucherPdfGeneratorService` (原1058行) 拆分而成：

| 服务/工具 | 职责 | 原方法 |
|----------|------|--------|
| `PaymentPdfGenerator` | 付款单 PDF 生成 | `generatePaymentPdf()` (~225行) |
| `CollectionPdfGenerator` | 收款单 PDF 生成 | `generateCollectionBillPdf()` (~190行) |
| `VoucherPdfGenerator` | 会计凭证 PDF 生成 | `generateVoucherPdf()`, `renderVoucherEntries()` (~310行) |
| `PdfDataParser` | JSON 数据解析 | `parseAuxiliaryItems()`, `parseCashFlowItems()`, `getTextValue()` (~65行) |
| `PdfFontLoader` | 字体加载 | `loadChineseFont()` (~35行) |
| `PdfUtils` | 工具方法 | `calculateSM3Hash()`, `truncateText()`, `safeText()` (~50行) |

## 依赖关系

```
VoucherPdfGeneratorService (协调层)
    ├── PaymentPdfGenerator (付款单生成)
    ├── CollectionPdfGenerator (收款单生成)
    ├── VoucherPdfGenerator (凭证生成)
    └── 工具类
        ├── PdfDataParser (数据解析)
        ├── PdfFontLoader (字体加载)
        └── PdfUtils (工具方法)
```