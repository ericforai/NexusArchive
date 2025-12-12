-- 补偿迁移：为 arc_file_content 表添加 certificate 列
-- 此列用于存储数字证书 (Base64 编码)

ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS certificate TEXT;

COMMENT ON COLUMN arc_file_content.certificate IS '数字证书 (Base64编码)';
