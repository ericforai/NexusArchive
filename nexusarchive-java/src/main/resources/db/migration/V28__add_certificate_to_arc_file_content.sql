-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 补偿迁移：为 arc_file_content 表添加 certificate 列
-- 此列用于存储数字证书 (Base64 编码)

ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS certificate TEXT;

COMMENT ON COLUMN arc_file_content.certificate IS '数字证书 (Base64编码)';
