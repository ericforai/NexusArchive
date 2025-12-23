-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 更新泛微 E10 服务器地址
-- 对应 Step 2524 用户提供的 Host，但修正为文档中的 API 网关

UPDATE sys_erp_config 
SET config_json = jsonb_set(
    config_json::jsonb, 
    '{baseUrl}', 
    '"https://api.eteams.cn"'
)::text
WHERE erp_type = 'weaver_e10';
