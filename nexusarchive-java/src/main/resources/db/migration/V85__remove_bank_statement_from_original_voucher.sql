-- Input: sys_original_voucher_type
-- Output: Cleanup of Bank Statement type
-- Pos: db/migration
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 1. Remove any existing "Original Voucher" instances of Bank Statement to prevent orphans
DELETE FROM arc_original_voucher WHERE voucher_type = 'BANK_STATEMENT';

-- 2. Remove the definition from the dictionary so it no longer appears in the UI
DELETE FROM sys_original_voucher_type WHERE type_code = 'BANK_STATEMENT';
