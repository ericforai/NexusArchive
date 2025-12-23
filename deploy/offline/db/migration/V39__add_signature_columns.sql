-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- V30: 添加电子签章相关列到 arc_file_content 表
-- 此列用于存储电子签章证书

-- 添加列 (幂等)
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS certificate TEXT;
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS check_result TEXT;
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS checked_time TIMESTAMP;
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS archived_time TIMESTAMP;
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS fiscal_year VARCHAR(10);
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS voucher_type VARCHAR(50);
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS creator VARCHAR(100);
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS fonds_code VARCHAR(50);
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS source_system VARCHAR(100);

COMMENT ON COLUMN arc_file_content.certificate IS '电子签章证书内容（Base64编码）';
COMMENT ON COLUMN arc_file_content.check_result IS '四性检测结果（JSON格式）';
COMMENT ON COLUMN arc_file_content.checked_time IS '检测时间';
COMMENT ON COLUMN arc_file_content.archived_time IS '归档时间';
COMMENT ON COLUMN arc_file_content.fiscal_year IS '会计年度';
COMMENT ON COLUMN arc_file_content.voucher_type IS '凭证类型';
COMMENT ON COLUMN arc_file_content.creator IS '创建人';
COMMENT ON COLUMN arc_file_content.fonds_code IS '全宗号';
COMMENT ON COLUMN arc_file_content.source_system IS '来源系统';
