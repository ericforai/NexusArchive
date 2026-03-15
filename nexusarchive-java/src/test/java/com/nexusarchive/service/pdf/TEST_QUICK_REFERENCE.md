# PaymentPdfGeneratorTest - 快速参考

## 文件位置

```
/Users/user/nexusarchive/nexusarchive-java/src/test/java/com/nexusarchive/service/pdf/PaymentPdfGeneratorTest.java
```

## 测试统计

- **总测试数**: 30
- **测试类别**: 单元测试 (Unit Tests)
- **测试标签**: `@Tag("unit")`
- **测试框架**: JUnit 5 + AssertJ + PDFBox

## 快速命令

### 运行所有测试
```bash
mvn test -Dtest=PaymentPdfGeneratorTest
```

### 运行单个测试
```bash
mvn test -Dtest=PaymentPdfGeneratorTest#generate_fullPaymentPdf_success
```

### 生成覆盖率报告
```bash
mvn test -Dtest=PaymentPdfGeneratorTest jacoco:report
open target/site/jacoco/index.html
```

## 测试分类

### 🟢 正常路径 (6 个)
### 🟡 边界条件 (7 个)
### 🔵 数据格式 (5 个)
### 🔴 异常处理 (3 个)
### 🟣 PDF 结构 (3 个)
### 🟠 性能和内存 (2 个)

详细列表见 TEST_RESULTS_SUMMARY.md

## 测试数据模板

### 标准付款单数据
```json
{
    "code": "PAY-2026-001",
    "billDate": "2026-01-15",
    "financeOrgName": "测试组织",
    "supplierName": "测试供应商",
    "oriCurrencyName": "CNY",
    "oriTaxIncludedAmount": 50000.00,
    "creatorUserName": "张三",
    "bodyItem": [
        {
            "quickTypeName": "货款",
            "materialName": "办公设备采购",
            "oriTaxIncludedAmount": 30000.00,
            "srcBillNo": "PO-2026-001"
        }
    ]
}
```

## 测试覆盖率目标

| 指标 | 目标 | 预期 |
|------|------|------|
| 行覆盖率 | 80%+ | 85%+ |
| 分支覆盖率 | 80%+ | 82%+ |
| 方法覆盖率 | 80%+ | 90%+ |
| 类覆盖率 | 80%+ | 100% |

