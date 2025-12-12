-- V29__add_pre_archive_status.sql
-- 添加预归档状态字段，支持预归档库状态管理
-- 依据《会计档案管理办法》第11条

-- 添加预归档状态字段
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS pre_archive_status VARCHAR(20) DEFAULT 'PENDING_CHECK';
COMMENT ON COLUMN arc_file_content.pre_archive_status IS '预归档状态: PENDING_CHECK/CHECK_FAILED/PENDING_METADATA/PENDING_ARCHIVE/ARCHIVED';

-- 添加 DA/T 94-2022 必填元数据字段
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS fiscal_year VARCHAR(4);
COMMENT ON COLUMN arc_file_content.fiscal_year IS '会计年度';

ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS voucher_type VARCHAR(50);
COMMENT ON COLUMN arc_file_content.voucher_type IS '凭证类型';

ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS creator VARCHAR(100);
COMMENT ON COLUMN arc_file_content.creator IS '责任者/创建人';

ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS fonds_code VARCHAR(50);
COMMENT ON COLUMN arc_file_content.fonds_code IS '全宗号';

ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS source_system VARCHAR(50);
COMMENT ON COLUMN arc_file_content.source_system IS '来源系统';

-- 添加检测相关字段
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS check_result TEXT;
COMMENT ON COLUMN arc_file_content.check_result IS '四性检测结果(JSON)';

ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS checked_time TIMESTAMP;
COMMENT ON COLUMN arc_file_content.checked_time IS '检测时间';

ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS archived_time TIMESTAMP;
COMMENT ON COLUMN arc_file_content.archived_time IS '归档时间';

-- 创建索引优化查询
CREATE INDEX IF NOT EXISTS idx_arc_file_content_status ON arc_file_content(pre_archive_status);
CREATE INDEX IF NOT EXISTS idx_arc_file_content_fiscal_year ON arc_file_content(fiscal_year);
