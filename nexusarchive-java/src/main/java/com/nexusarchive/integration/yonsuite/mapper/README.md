# YonSuite Mapper Layer

> **Updated**: 2026-01-04

## Overview

本目录包含 YonSuite 集成的数据映射器，负责将 YonSuite API 响应映射为 `ArcFileContent` 实体。

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  YonSuite API Response                       │
│  SalesOutListResponse / SalesOutDetailResponse              │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                     Mapper Layer                             │
│  SalesOutMapper.toPreArchiveFile(record)                    │
│  - 字段映射                                                  │
│  - 类型转换                                                  │
│  - 数据验证                                                  │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                   ArcFileContent                             │
│  (电子凭证池表)                                              │
└─────────────────────────────────────────────────────────────┘
```

## Mappers

### 1. SalesOutMapper ⭐ (v2.0 新增)

**职责**：销售出库单数据映射器

| Method | Description |
|--------|-------------|
| `toPreArchiveFile(SalesOutRecord)` | 将列表记录映射为 ArcFileContent |
| `toPreArchiveFile(SalesOutDetail)` | 将详情记录映射为 ArcFileContent |

**字段映射规则**：

| YonSuite Field | ArcFileContent Field | Mapping Logic |
|----------------|---------------------|---------------|
| `id` | `id` | `YonSuite_SALESOUT_{id}` |
| `id` | `businessDocNo` | `YonSuite_SALESOUT_{id}` |
| `code` | `erpVoucherNo` | 直接映射 |
| `vouchdate` | `docDate` | 解析为 `LocalDate` |
| `cust_name` | `summary` | `客户: {custName}, 仓库: {warehouseName}` |
| `warehouse_name` | `summary` | 同上 |
| `totalQuantity` | `sourceData` | JSON 序列化 |
| - | `sourceSystem` | 固定值: `"YonSuite"` |
| - | `fileType` | 固定值: `"SALES_OUT"` |
| - | `preArchiveStatus` | 固定值: `"PENDING_CHECK"` |
| - | `archivalCode` | 自动生成: `SCP-{year}-{timestamp}` |
| - | `fileName` | `销售出库单-{code}.json` |
| - | `storagePath` | `pending/YonSuite/salesout/{businessDocNo}.json` |

**示例**：

```java
// Input
SalesOutListResponse.SalesOutRecord record = new SalesOutListResponse.SalesOutRecord();
record.setId("123456");
record.setCode("SO202501001");
record.setVouchdate("2025-01-04 00:00:00");
record.setCustName("某某客户");
record.setWarehouseName("主仓库");
record.setTotalQuantity("100.0");

// Output
ArcFileContent fileContent = salesOutMapper.toPreArchiveFile(record);
fileContent.getId();           // "YonSuite_SALESOUT_123456"
fileContent.getBusinessDocNo(); // "YonSuite_SALESOUT_123456"
fileContent.getErpVoucherNo();  // "SO202501001"
fileContent.getDocDate();       // LocalDate.of(2025, 1, 4)
fileContent.getSummary();       // "客户: 某某客户, 仓库: 主仓库"
fileContent.getSourceSystem();  // "YonSuite"
fileContent.getFileType();      // "SALES_OUT"
```

### 2. YonVoucherMapper

**职责**：凭证数据映射器（旧版）

## Usage Example

### 基本使用

```java
@Service
public class SyncService {
    private final SalesOutMapper salesOutMapper;

    public void syncSalesOut() {
        // 调用 API 获取数据
        SalesOutListResponse response = callYonSuiteApi();

        // 映射每条记录
        for (SalesOutListResponse.SalesOutRecord record : response.getData().getRecordList()) {
            ArcFileContent fileContent = salesOutMapper.toPreArchiveFile(record);

            // 保存到数据库
            arcFileContentMapper.insert(fileContent);
        }
    }
}
```

### 自定义映射

如需自定义映射逻辑，可以扩展 `SalesOutMapper`：

```java
@Component
public class CustomSalesOutMapper extends SalesOutMapper {
    @Override
    public ArcFileContent toPreArchiveFile(SalesOutListResponse.SalesOutRecord record) {
        ArcFileContent content = super.toPreArchiveFile(record);

        // 添加自定义字段
        content.setCustomField("custom_value");

        return content;
    }
}
```

## Error Handling

### 空值处理

```java
private LocalDate parseDate(String dateStr) {
    if (dateStr == null || dateStr.isEmpty()) {
        return null;  // 空值返回 null
    }
    try {
        return LocalDate.parse(dateStr.split(" ")[0], DATE_FORMATTER);
    } catch (Exception e) {
        log.warn("Failed to parse date: {}", dateStr);
        return null;  // 解析失败返回 null
    }
}
```

### 异常捕获

```java
public ArcFileContent toPreArchiveFile(SalesOutRecord record) {
    try {
        // 映射逻辑
        return fileContent;
    } catch (Exception e) {
        log.error("Failed to map sales out record: {}", record.getId(), e);
        return null;  // 映射失败返回 null
    }
}
```

## Source Data Storage

原始 API 响应数据以 JSON 格式存储在 `sourceData` 字段：

```json
{
  "id": "123456",
  "code": "SO202501001",
  "vouchdate": "2025-01-04 00:00:00",
  "cust_name": "某某客户",
  "warehouse_name": "主仓库",
  "totalQuantity": "100.0"
}
```

**用途**：
- 数据溯源
- 问题排查
- 后续处理
- 审计日志

## Testing

### Unit Tests

```java
@SpringBootTest
class SalesOutMapperTest {

    @Autowired
    private SalesOutMapper salesOutMapper;

    @Test
    void shouldMapSalesOutRecordToArcFileContent() {
        // Given
        SalesOutListResponse.SalesOutRecord record = new SalesOutListResponse.SalesOutRecord();
        record.setId("123456");
        record.setCode("SO202501001");
        record.setVouchdate("2025-01-04 00:00:00");
        record.setCustName("测试客户");

        // When
        ArcFileContent result = salesOutMapper.toPreArchiveFile(record);

        // Then
        assertEquals("YonSuite_SALESOUT_123456", result.getId());
        assertEquals("SO202501001", result.getErpVoucherNo());
        assertEquals("YonSuite", result.getSourceSystem());
        assertEquals("SALES_OUT", result.getFileType());
    }

    @Test
    void shouldHandleNullDate() {
        // Given
        SalesOutListResponse.SalesOutRecord record = new SalesOutListResponse.SalesOutRecord();
        record.setId("123456");
        record.setVouchdate(null);

        // When
        ArcFileContent result = salesOutMapper.toPreArchiveFile(record);

        // Then
        assertNull(result.getDocDate());
    }
}
```

## File List

| 文件 | 状态 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `SalesOutMapper.java` | ✅ Mapper | 销售出库单映射器（v2.0 新增） |
| `YonVoucherMapper.java` | ⚠️ 旧版 | 凭证映射器 |

## References

- [YonSuite 集成模块文档](/nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/README.md)
- [ArcFileContent 实体定义](/nexusarchive-java/src/main/java/com/nexusarchive/entity/ArcFileContent.java)
