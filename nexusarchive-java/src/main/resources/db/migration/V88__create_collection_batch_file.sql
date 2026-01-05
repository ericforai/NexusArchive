-- ============================================================
-- 资料收集批次文件表 (Collection Batch File)
-- 记录批次内每个文件的上传状态和处理结果
-- ============================================================

CREATE TABLE IF NOT EXISTS public.collection_batch_file (
    -- 主键
    id BIGSERIAL PRIMARY KEY,

    -- 批次关联
    batch_id BIGINT NOT NULL REFERENCES public.collection_batch(id) ON DELETE CASCADE,

    -- 文件标识 (关联到 arc_file_content)
    file_id VARCHAR(50), -- 上传成功后关联到 arc_file_content.id

    -- 原始文件信息
    original_filename VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    file_type VARCHAR(20), -- PDF/OFD/XML/JPG/PNG

    -- 文件哈希 (幂等性控制)
    file_hash VARCHAR(128),
    hash_algorithm VARCHAR(20) DEFAULT 'SHA-256',

    -- 上传状态
    upload_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- PENDING: 等待上传
    -- UPLOADING: 上传中
    -- UPLOADED: 上传成功
    -- FAILED: 上传失败
    -- DUPLICATE: 重复文件
    -- VALIDATING: 校验中
    -- VALIDATED: 校验完成
    -- CHECK_FAILED: 四性检测失败

    -- 处理结果 (JSON 格式)
    processing_result JSONB,

    -- 错误信息
    error_message TEXT,

    -- 上传顺序
    upload_order INTEGER NOT NULL,

    -- 时间戳
    started_time TIMESTAMP,
    completed_time TIMESTAMP,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 约束
    CONSTRAINT chk_upload_status
        CHECK (upload_status IN ('PENDING', 'UPLOADING', 'UPLOADED', 'FAILED', 'DUPLICATE', 'VALIDATING', 'VALIDATED', 'CHECK_FAILED'))
);

-- 注释
COMMENT ON TABLE public.collection_batch_file IS '资料收集批次文件表 - 记录批次内每个文件的上传和处理状态';
COMMENT ON COLUMN public.collection_batch_file.batch_id IS '所属批次ID';
COMMENT ON COLUMN public.collection_batch_file.file_id IS '关联的文件ID (arc_file_content.id)';
COMMENT ON COLUMN public.collection_batch_file.original_filename IS '原始文件名';
COMMENT ON COLUMN public.collection_batch_file.file_hash IS '文件哈希值 (用于幂等性控制)';
COMMENT ON COLUMN public.collection_batch_file.upload_status IS '上传状态';
COMMENT ON COLUMN public.collection_batch_file.processing_result IS '处理结果 (包含四性检测报告)';

-- 索引
CREATE INDEX IF NOT EXISTS idx_collection_batch_file_batch_id ON public.collection_batch_file(batch_id);
CREATE INDEX IF NOT EXISTS idx_collection_batch_file_file_id ON public.collection_batch_file(file_id);
CREATE INDEX IF NOT EXISTS idx_collection_batch_file_status ON public.collection_batch_file(upload_status);
CREATE INDEX IF NOT EXISTS idx_collection_batch_file_hash ON public.collection_batch_file(file_hash);

-- 唯一约束 (同一批次内文件名唯一)
CREATE UNIQUE INDEX IF NOT EXISTS idx_collection_batch_file_batch_name
    ON public.collection_batch_file(batch_id, original_filename)
    WHERE upload_status NOT IN ('FAILED', 'DUPLICATE');

-- 索引注释
COMMENT ON INDEX idx_collection_batch_file_batch_id IS '批次查询索引';
COMMENT ON INDEX idx_collection_batch_file_file_id IS '文件关联索引';
COMMENT ON INDEX idx_collection_batch_file_status IS '上传状态索引';
COMMENT ON INDEX idx_collection_batch_file_hash IS '文件哈希去重索引';
COMMENT ON INDEX idx_collection_batch_file_batch_name IS '批次内文件名唯一约束索引';
