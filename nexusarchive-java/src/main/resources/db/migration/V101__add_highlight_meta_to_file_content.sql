-- Input: arc_file_content 表
-- Output: 新增 highlight_meta 字段
-- Pos: V101
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS highlight_meta JSONB;
COMMENT ON COLUMN arc_file_content.highlight_meta IS '文件高亮元数据(坐标信息)';
