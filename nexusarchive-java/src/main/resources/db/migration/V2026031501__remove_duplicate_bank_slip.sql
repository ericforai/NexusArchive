-- ============================================================
-- V2026031501: 删除重复的“银行回单”凭证类型
-- ============================================================
-- 问题描述：sys_original_voucher_type 中同时存在 BANK_RECEIPT 和 BANK_SLIP，导致 UI 重复。
-- 解决方案：删除多余的 BANK_SLIP，保留 BANK_RECEIPT。

DELETE FROM sys_original_voucher_type WHERE type_code = 'BANK_SLIP';

-- 确保已有的数据不会因为外键而失败（如有必要）
-- 经查：arc_original_voucher 使用 voucher_type 存储字符串，非强外键约束，
-- 但为了稳妥，如果有人已经用了 BANK_SLIP，将其迁移到 BANK_RECEIPT。
UPDATE arc_original_voucher SET voucher_type = 'BANK_RECEIPT' WHERE voucher_type = 'BANK_SLIP';
UPDATE arc_file_content SET voucher_type = 'BANK_RECEIPT' WHERE voucher_type = 'BANK_SLIP';

COMMENT ON TABLE sys_original_voucher_type IS '原始凭证类型表：解决银行回单重复项 (BANK_SLIP 已移除)';
