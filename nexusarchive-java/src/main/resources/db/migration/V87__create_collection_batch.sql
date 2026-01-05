-- ============================================================
-- 资料收集批次表 (Collection Batch)
-- 符合 GB/T 39362-2020 电子会计档案管理系统建设要求
-- ============================================================

CREATE TABLE public.collection_batch (
    -- 主键
    id BIGSERIAL PRIMARY KEY,

    -- 批次标识
    batch_no VARCHAR(50) NOT NULL UNIQUE,
    batch_name VARCHAR(200) NOT NULL,

    -- 全宗信息 (会计档案归属)
    fonds_id BIGINT NOT NULL,
    fonds_code VARCHAR(20) NOT NULL,

    -- 会计期间
    fiscal_year VARCHAR(10) NOT NULL,
    fiscal_period VARCHAR(20),

    -- 档案门类 (凭证/账簿/报告/其他)
    archival_category VARCHAR(50) NOT NULL,

    -- 来源渠道 (WEB上传/邮箱导入/银企直联/ERP集成)
    source_channel VARCHAR(50) NOT NULL DEFAULT 'WEB上传',

    -- 批次状态
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADING',
    -- UPLOADING: 上传中
    -- UPLOADED: 上传完成
    -- VALIDATING: 校验中
    -- VALIDATED: 校验完成
    -- FAILED: 上传/校验失败
    -- ARCHIVED: 已归档

    -- 统计信息
    total_files INTEGER NOT NULL DEFAULT 0,
    uploaded_files INTEGER NOT NULL DEFAULT 0,
    failed_files INTEGER NOT NULL DEFAULT 0,
    total_size_bytes BIGINT NOT NULL DEFAULT 0,

    -- 校验结果 (JSON 格式存储四性检测汇总)
    validation_report JSONB,

    -- 错误信息
    error_message TEXT,

    -- 审计字段
    created_by BIGINT NOT NULL,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_time TIMESTAMP,

    -- 索引优化
    CONSTRAINT chk_collection_batch_status
        CHECK (status IN ('UPLOADING', 'UPLOADED', 'VALIDATING', 'VALIDATED', 'FAILED', 'ARCHIVED'))
);

-- 评论
COMMENT ON TABLE public.collection_batch IS '资料收集批次表 - 管理批量上传会话';
COMMENT ON COLUMN public.collection_batch.batch_no IS '批次编号 (格式: COL-YYYYMMDD-NNN)';
COMMENT ON COLUMN public.collection_batch.batch_name IS '批次名称';
COMMENT ON COLUMN public.collection_batch.fonds_id IS '全宗ID';
COMMENT ON COLUMN public.collection_batch.fonds_code IS '全宗代码';
COMMENT ON COLUMN public.collection_batch.fiscal_year IS '会计年度';
COMMENT ON COLUMN public.collection_batch.fiscal_period IS '会计期间';
COMMENT ON COLUMN public.collection_batch.archival_category IS '档案门类 (VOUCHER/LEDGER/REPORT/OTHER)';
COMMENT ON COLUMN public.collection_batch.source_channel IS '来源渠道';
COMMENT ON COLUMN public.collection_batch.status IS '批次状态';
COMMENT ON COLUMN public.collection_batch.validation_report IS '四性检测汇总报告 (JSONB)';

-- 索引
CREATE INDEX idx_collection_batch_fonds ON public.collection_batch(fonds_id);
CREATE INDEX idx_collection_batch_status ON public.collection_batch(status);
CREATE INDEX idx_collection_batch_created_time ON public.collection_batch(created_time DESC);
CREATE INDEX idx_collection_batch_fiscal_year ON public.collection_batch(fiscal_year);
