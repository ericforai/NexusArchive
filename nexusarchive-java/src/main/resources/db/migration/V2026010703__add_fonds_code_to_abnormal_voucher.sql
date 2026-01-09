-- 为异常凭证表添加全宗号字段，支持多全宗物理隔离
-- Reference: DA/T 94-2022
ALTER TABLE arc_abnormal_voucher ADD COLUMN IF NOT EXISTS fonds_code VARCHAR(50);
COMMENT ON COLUMN arc_abnormal_voucher.fonds_code IS '全宗号';

-- 为存量数据尝试补全全宗号 (如果 JSON 数据中存在)
-- 注意：这里仅作示意，实际生产环境建议通过程序进行元数据修复
UPDATE arc_abnormal_voucher 
SET fonds_code = (sip_data::jsonb -> 'header' ->> 'fondsCode')
WHERE fonds_code IS NULL AND sip_data IS NOT NULL;

-- 创建索引以优化隔离查询
CREATE INDEX IF NOT EXISTS idx_abnormal_voucher_fonds ON arc_abnormal_voucher(fonds_code);
