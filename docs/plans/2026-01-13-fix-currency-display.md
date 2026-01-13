# Bug Fix: 凭证预览币种字段缺失修复计划

## Goal
解决预归档池及全景视图中，凭证预览页面不显示币种和原币金额的问题。
根本原因是前端 `voucherDataParser.ts` 对 YonSuite 返回的原始 JSON 数据结构解析不完整，未能正确提取币种信息。

## User Review Required
> [!NOTE]
> 此修复仅涉及数据展示层面的解析逻辑调整，不会修改已归档的数据内容。

## Proposed Changes

### Frontend
#### [MODIFY] [voucherDataParser.ts](file:///Users/user/nexusarchive/src/pages/archives/utils/voucherDataParser.ts)
- 增强 `parseVoucherData` 函数的兼容性
- 添加对 `currency_id` / `currency_name` 扁平字段的支持
- 添加对 `oriCurrency` / `oriCurrencyName` 等变体字段的支持
- 优化原币金额 (`debitOriginal` / `creditOriginal`) 的获取逻辑，支持 `amountOriginal`, `localAmount` 等可能的字段名

## Verification Plan

### Automated Tests
- 编写一个新的 Jest 测试用例用于 `voucherDataParser.ts` (可选，视环境而定)

### Manual Verification
1. 启动前端和后端服务
2. 进入 `/system/pre-archive/pool` 页面
3. 点击任意包含外币（或本位币但带有币种信息）的凭证行查看预览
4. 确认"币种"列显示 "CNY" 或其他币种代码
5. 确认"原币"列在非 CNY 时显示金额
