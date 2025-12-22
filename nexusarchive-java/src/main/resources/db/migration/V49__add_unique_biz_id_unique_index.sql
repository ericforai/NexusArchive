-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- Enforce unique business ID for archives (ignore logically deleted rows)
DROP INDEX IF EXISTS idx_archive_unique_biz_id;
CREATE UNIQUE INDEX IF NOT EXISTS ux_acc_archive_unique_biz_id_not_deleted
    ON acc_archive(unique_biz_id)
    WHERE deleted = 0;
