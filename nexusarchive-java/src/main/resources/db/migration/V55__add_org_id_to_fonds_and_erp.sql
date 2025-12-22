-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- Add org_id to bas_fonds
ALTER TABLE bas_fonds ADD COLUMN IF NOT EXISTS org_id VARCHAR(64);
COMMENT ON COLUMN bas_fonds.org_id IS '关联组织ID（公司级）';
CREATE INDEX IF NOT EXISTS idx_bas_fonds_org ON bas_fonds(org_id);

-- Add org_id to sys_erp_config
ALTER TABLE sys_erp_config ADD COLUMN IF NOT EXISTS org_id VARCHAR(64);
COMMENT ON COLUMN sys_erp_config.org_id IS '关联组织ID';
CREATE INDEX IF NOT EXISTS idx_erp_config_org ON sys_erp_config(org_id);
