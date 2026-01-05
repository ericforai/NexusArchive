-- ============================================================
-- 批量上传合规性修改
-- 添加 collection_batch_file.archive_id 字段关联档案记录
-- 符合 DA/T 94-2022 元数据同步捕获要求
-- ============================================================

-- 添加档案关联字段
ALTER TABLE public.collection_batch_file
ADD COLUMN IF NOT EXISTS archive_id VARCHAR(50);

-- 创建索引优化查询
CREATE INDEX IF NOT EXISTS idx_collection_batch_file_archive_id
ON public.collection_batch_file(archive_id);

-- 添加注释
COMMENT ON COLUMN public.collection_batch_file.archive_id IS '关联的档案ID (acc_archive.id) - 上传完成后立即创建的档案记录，用于凭证关联';

-- 添加外键约束 (acc_archive 表可能已有数据，使用 ALTER TABLE 添加)
-- 注意：如果 acc_archive 表不存在或结构不同，此约束可能需要调整
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'acc_archive'
    ) THEN
        ALTER TABLE public.collection_batch_file
        ADD CONSTRAINT fk_collection_batch_file_archive
        FOREIGN KEY (archive_id) REFERENCES public.acc_archive(id)
        ON DELETE SET NULL;
    END IF;
END $$;
