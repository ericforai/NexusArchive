-- ============================================================
-- V2026031501: 删除重复的“银行回单”凭证类型
-- ============================================================
-- 问题描述：sys_original_voucher_type 中同时存在 BANK_RECEIPT 和 BANK_SLIP，导致 UI 重复。
-- 解决方案：先更新业务数据引用，再从字典表中删除多余的 BANK_SLIP，保留 BANK_RECEIPT。

-- 1. 更新业务数据（确保引用完整性，即使没有强外键约束也应遵循此顺序）
UPDATE arc_original_voucher SET voucher_type = 'BANK_RECEIPT' WHERE voucher_type = 'BANK_SLIP';
UPDATE arc_file_content SET voucher_type = 'BANK_RECEIPT' WHERE voucher_type = 'BANK_SLIP';

-- 2. 从类型字典表中删除多余项
DELETE FROM sys_original_voucher_type WHERE type_code = 'BANK_SLIP';

COMMENT ON TABLE sys_original_voucher_type IS '原始凭证类型表：解决银行回单重复项 (BANK_SLIP 已移除，数据已迁移至 BANK_RECEIPT)';
