# [修复 YonSuite 凭证预览币种字段缺失] Walkthrough

## Use Case
修复 YonSuite 凭证同步后，在预归档池及全景视图中预览时，"币种"、"原币金额"及"汇率"列显示为空的问题。

## Changes

### 1. DTO 扩展
#### [MODIFY] [VoucherEntryDto.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/dto/sip/VoucherEntryDto.java)
- 新增字段：`currencyCode`, `currencyName`, `debitOriginal`, `creditOriginal`, `exchangeRate`.
- 更新构造函数和 Builder。

#### [MODIFY] [YonVoucherListResponse.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/YonVoucherListResponse.java)
- 在 `VoucherBody` 中新增 `exchangeRate` 字段，对应 JSON 字段 `exchange_rate`。

### 2. 映射配置更新
#### [MODIFY] [yonsuite-mapping.yml](file:///Users/user/nexusarchive/nexusarchive-java/src/main/resources/erp-mapping/yonsuite-mapping.yml)
- 新增分录字段映射规则，将 ERP 数据中的币种信息映射到 SIP DTO。

### 3. 适配器逻辑更新
#### [MODIFY] [YonSuiteErpAdapter.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/YonSuiteErpAdapter.java)
- 在 `convertSipToVoucherDto` 方法中，将 SIP DTO 中的币种字段传递给内部使用的 `VoucherDTO`。

## Verification Results

### Automated Tests
- **Test Class**: `com.nexusarchive.integration.erp.mapping.YonSuiteMappingTest`
- **Result**: PASSED
- **Scope**: 验证了从 `YonVoucherListResponse` 到 `AccountingSipDto` 的完整映射逻辑，确认所有新增字段均能正确提取和赋值。

### Manual Verification Steps (For User)
1. 重启后端服务。
2. 在 YonSuite 凭证同步页面，重新触发同步（或删除现有的一条凭证后重新同步）。
3. 进入 **预归档池**，点击凭证预览。
4. 确认 **分录表格** 中的 "币种"、"原币借方"、"原币贷方" 列现在显示正确数值。
