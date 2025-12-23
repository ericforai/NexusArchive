-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- Insert a seed ERP config (Layer 1)
-- Using config_json to store details as per entity definition
INSERT INTO sys_erp_config (name, erp_type, config_json, is_active)
VALUES 
('用友YonSuite', 'yonsuite', '{"baseUrl":"https://api.yonsuite.com", "appKey":"mock", "appSecret":"mock"}', 1)
ON CONFLICT DO NOTHING;

-- Insert a Kingdee config
INSERT INTO sys_erp_config (name, erp_type, config_json, is_active)
VALUES 
('金蝶云星空', 'kingdee', '{"baseUrl":"https://api.kingdee.com", "appKey":"mock", "appSecret":"mock"}', 1)
ON CONFLICT DO NOTHING;

