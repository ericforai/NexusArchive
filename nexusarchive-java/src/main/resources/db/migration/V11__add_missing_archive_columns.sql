-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- Add missing columns to acc_archive table
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS unique_biz_id VARCHAR(64);
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS amount DECIMAL(18, 2);
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS doc_date DATE;
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS volume_id VARCHAR(64);

-- Add comments
COMMENT ON COLUMN acc_archive.unique_biz_id IS '唯一业务ID';
COMMENT ON COLUMN acc_archive.amount IS '金额';
COMMENT ON COLUMN acc_archive.doc_date IS '业务日期';
COMMENT ON COLUMN acc_archive.volume_id IS '所属案卷ID';

-- Add missing columns to arc_file_content table
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS item_id VARCHAR(64);
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS original_hash VARCHAR(128);
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS current_hash VARCHAR(128);
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS timestamp_token BYTEA;
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS sign_value BYTEA;

COMMENT ON COLUMN arc_file_content.item_id IS '关联单据ID';
COMMENT ON COLUMN arc_file_content.original_hash IS '原始哈希值 (接收时)';
COMMENT ON COLUMN arc_file_content.current_hash IS '当前哈希值 (巡检时)';
COMMENT ON COLUMN arc_file_content.timestamp_token IS '时间戳Token';
COMMENT ON COLUMN arc_file_content.sign_value IS '电子签名值';
