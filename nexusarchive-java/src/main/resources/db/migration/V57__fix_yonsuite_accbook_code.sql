-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

UPDATE sys_erp_config
SET config_json = jsonb_set(
    config_json::jsonb,
    '{accbookCode}',
    '"BRYS002"'
)::text,
last_modified_time = NOW()
WHERE id = 7;
