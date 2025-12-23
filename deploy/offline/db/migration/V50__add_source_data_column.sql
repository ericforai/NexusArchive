-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 添加 source_data 列，用于存储原始业务数据(JSON)
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS source_data TEXT;
COMMENT ON COLUMN arc_file_content.source_data IS '原始业务数据(JSON)';
