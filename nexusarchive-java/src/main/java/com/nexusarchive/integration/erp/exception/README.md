一旦我所属的文件夹有所变化，请更新我。
本目录存放 ERP 集成层异常类。
用于定义 ERP 适配器相关的业务异常。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `PeriodNotClosedException.java` | 异常类 | 期间未关账异常（强制模式下抛出）|

## 异常说明

### PeriodNotClosedException

**用途**: 当 YonSuite 配置了强制关账检查模式，且目标期间未关账时抛出。

**字段**:
| 字段 | 类型 | 说明 |
|------|------|------|
| period | String | 未关账的期间（格式: yyyy-MM）|
| accbookCode | String | 目标账套编码 |

**使用场景**:
```java
// YonSuiteErpAdapter.syncVouchers()
if (!closeInfo.isClosed() && isRequireClosedPeriod(config)) {
    throw new PeriodNotClosedException(period, targetAccbookCode);
}
```

**异常消息格式**:
```
期间 2025-01 (账套: BR01) 未关账，请先在 ERP 系统完成关账
```
