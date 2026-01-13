# [修复 YonSuite 凭证预览币种字段缺失] Implementation Plan

## Goal Description
修复 YonSuite 凭证在预览时（预归档池及全景视图）不显示"币种"和"原币金额"的问题。
经排查，问题的根本原因在于数据同步链路中（SIP DTO -> 映射配置 -> 适配器转换）完全缺失了币种相关字段的定义和映射。

## User Review Required
> [!IMPORTANT]
> 此修改涉及核心 DTO (`VoucherEntryDto`) 的变更。虽然增加字段通常向后兼容，但请确保没有其他自定义适配器严格依赖旧的字段数量（虽然可能性极小）。

## Proposed Changes

### 数据传输层 (DTO)
#### [MODIFY] [VoucherEntryDto.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/dto/sip/VoucherEntryDto.java)
- 新增字段：
    - `String currencyCode` (币种代码)
    - `String currencyName` (币种名称)
    - `BigDecimal debitOriginal` (原币借方)
    - `BigDecimal creditOriginal` (原币贷方)
    - `BigDecimal exchangeRate` (汇率)

### 映射配置层
#### [MODIFY] [yonsuite-mapping.yml](file:///Users/user/nexusarchive/nexusarchive-java/src/main/resources/erp-mapping/yonsuite-mapping.yml)
- 在 `entries.item` 下新增映射规则：
    - `currencyCode`: `currency.code`
    - `currencyName`: `currency.name`
    - `debitOriginal`: `debitOriginal`
    - `creditOriginal`: `creditOriginal`
    - `exchangeRate`: `exchangeRate`

### 适配器逻辑层
#### [MODIFY] [YonSuiteErpAdapter.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/YonSuiteErpAdapter.java)
- 更新 `convertSipToVoucherDto` 方法：
    - 将 `AccountingSipDto.entries` 中的新字段映射到 `VoucherDTO.entries`。

## Verification Plan

### Automated Tests
创建一个新的测试类 `com.nexusarchive.integration.erp.mapping.YonSuiteMappingTest` 来验证完整的映射链路。

1. **测试目标**: 验证 `DefaultErpMapper` 能正确加载 `yonsuite-mapping.yml` 并将包含币种信息的 Mock 数据转换为 `AccountingSipDto`。
2. **测试逻辑**:
    - 构造一个包含 `currency: {code: "USD", name: "美元"}` 和 `debitOriginal: 100.00` 的 `YonVoucherListResponse.VoucherRecord` 对象。
    - 调用 `defaultErpMapper.mapToSipDto(...)`。
    - 断言生成的 `AccountingSipDto` 中的 `entries` 包含正确的 `currencyCode` 和 `debitOriginal`。

### Manual Verification
1.  **部署代码**: 编译并重启后端服务。
2.  **数据刷新**: 由于旧数据的 `sourceData` 已经是损坏的（缺失字段的 JSON），需要用户在界面上**重新点击"同步"** 或者等待定时任务执行，或者删除旧文件重新触发预览（如果是实时获取）。
    - *注意*: 如果是预归档池已存在的数据，必须删除后重新同步才能生效。
3.  **UI 验证**: 进入 `/system/pre-archive/pool`，打开新同步的凭证预览，检查"币种"和"原币"列是否显示数据。
