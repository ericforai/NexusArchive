-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- V32: Fix schema validation errors
-- 1. biz_archive_approval: missing org_name
ALTER TABLE biz_archive_approval ADD COLUMN IF NOT EXISTS org_name VARCHAR(255);
COMMENT ON COLUMN biz_archive_approval.org_name IS '立档单位';

-- 2. sys_setting: missing id, config_key, config_value
-- Try to create table if not exists with basic structure matching Entity
CREATE TABLE IF NOT EXISTS sys_setting (
    id VARCHAR(64) PRIMARY KEY,
    config_key VARCHAR(100),
    config_value VARCHAR(500),
    description VARCHAR(255),
    category VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

-- Supplement columns if table existed but was incomplete
ALTER TABLE sys_setting ADD COLUMN IF NOT EXISTS id VARCHAR(64);
ALTER TABLE sys_setting ADD COLUMN IF NOT EXISTS config_key VARCHAR(100);
ALTER TABLE sys_setting ADD COLUMN IF NOT EXISTS config_value VARCHAR(500);
ALTER TABLE sys_setting ADD COLUMN IF NOT EXISTS description VARCHAR(255);
