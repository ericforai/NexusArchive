-- 添加 source_data 列，用于存储原始业务数据(JSON)
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS source_data TEXT;
COMMENT ON COLUMN arc_file_content.source_data IS '原始业务数据(JSON)';
