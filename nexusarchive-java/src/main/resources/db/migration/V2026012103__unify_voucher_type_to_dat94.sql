-- ============================================================================
-- V2026012103__unify_voucher_type_to_dat94.sql
-- 统一 voucher_type 为 DA/T 94 标准码
-- 
-- DA/T 94-2022 会计档案门类标准码:
--   AC01 = 会计凭证（原始凭证）
--   AC02 = 会计账簿
--   AC03 = 财务报告
--   AC04 = 其他会计资料
--   VOUCHER = 记账凭证（ERP同步，保留）
--   ATTACHMENT = 原始凭证附件（归入AC01范畴，保留）
-- ============================================================================

-- 1. 将 REPORT 统一为 AC03（财务报告）
UPDATE arc_file_content 
SET voucher_type = 'AC03' 
WHERE voucher_type = 'REPORT';

-- 2. 将 OTHER 统一为 AC04（其他资料）（如有）
UPDATE arc_file_content 
SET voucher_type = 'AC04' 
WHERE voucher_type = 'OTHER';

-- 3. 将 LEDGER 统一为 AC02（会计账簿）（如有）
UPDATE arc_file_content 
SET voucher_type = 'AC02' 
WHERE voucher_type = 'LEDGER';

-- 4. 添加索引优化门类查询性能
CREATE INDEX IF NOT EXISTS idx_arc_file_voucher_type 
ON arc_file_content(voucher_type);

CREATE INDEX IF NOT EXISTS idx_arc_file_status_type 
ON arc_file_content(pre_archive_status, voucher_type);

-- 5. 添加注释说明标准码含义
COMMENT ON COLUMN arc_file_content.voucher_type IS 
'档案门类代码 (DA/T 94-2022): VOUCHER=记账凭证, AC01=原始凭证, ATTACHMENT=凭证附件, AC02=会计账簿, AC03=财务报告, AC04=其他资料';
