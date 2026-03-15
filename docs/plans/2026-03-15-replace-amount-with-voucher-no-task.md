# 任务清单：动态凭证号字段替换金额字段

- [ ] 详细研究与规划 <!-- id: 0 -->
    - [x] 确认前端组件 `CreateOriginalVoucherDialog.tsx` <!-- id: 1 -->
    - [x] 确认 API 定义 `src/api/originalVoucher.ts` <!-- id: 2 -->
    - [x] 制定实施细节：如何根据类别动态显示标签 <!-- id: 3 -->
- [ ] 核心功能修改 <!-- id: 4 -->
    - [ ] 修改 `CreateOriginalVoucherDialog.tsx` 的状态管理 (移除 `amount`, 增加 `voucherNo`) <!-- id: 5 -->
    - [ ] 实现标签映射逻辑 (INVOICE -> 发票号, CONTRACT -> 合同号) <!-- id: 6 -->
    - [ ] 调整 UI 布局与表单逻辑 <!-- id: 7 -->
    - [ ] 更新提交 API 调用逻辑 <!-- id: 8 -->
- [ ] 修复重复凭证类型 (银行回单) <!-- id: 13 -->
    - [ ] 创建迁移脚本 `V2026031501__remove_duplicate_bank_slip.sql` <!-- id: 14 -->
    - [ ] 验证下拉列表是否已去重 <!-- id: 15 -->
- [ ] 验证与清理 <!-- id: 9 -->
    - [ ] 手动验证不同类别的标签显示 <!-- id: 10 -->
    - [ ] 验证数据提交是否正确 <!-- id: 11 -->
    - [ ] 更新相关的 README.md <!-- id: 12 -->
