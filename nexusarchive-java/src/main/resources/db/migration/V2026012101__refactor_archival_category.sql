-- ---------------------------------------------------------
-- 统一档案门类元数据重构
-- 1. 重命名 voucher_category 为 archival_category
-- 2. 增加 source_type 字段并设置默认值
-- 3. 将 archival_category 设为 NOT NULL
-- ---------------------------------------------------------

-- 对齐字段名 (若存在旧字段)
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='arc_original_voucher' AND column_name='voucher_category') THEN
        ALTER TABLE public.arc_original_voucher RENAME COLUMN voucher_category TO archival_category;
    END IF;
END $$;

-- 增加 source_type
ALTER TABLE public.arc_original_voucher ADD COLUMN IF NOT EXISTS source_type VARCHAR(20) DEFAULT 'MANUAL_UPLOAD';
COMMENT ON COLUMN public.arc_original_voucher.source_type IS '数据来源类型: API_SYNC / MANUAL_UPLOAD';

-- 刷历史数据逻辑 (遵循 DA/T 94-2022)
UPDATE public.arc_original_voucher SET source_type = 'API_SYNC' WHERE source_system != 'WEB上传';
UPDATE public.arc_original_voucher SET archival_category = 'VOUCHER' WHERE archival_category IS NULL;

-- 设为不可为空
ALTER TABLE public.arc_original_voucher ALTER COLUMN archival_category SET NOT NULL;

-- 更新索引名 (可选，为了整洁)
ALTER INDEX IF EXISTS idx_ov_type RENAME TO idx_ov_archival_category;
