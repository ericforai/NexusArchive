-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- Add org_name to biz_archive_approval
ALTER TABLE biz_archive_approval ADD COLUMN IF NOT EXISTS org_name VARCHAR(255);
COMMENT ON COLUMN biz_archive_approval.org_name IS '立档单位';
