-- 更新泛微 E10 服务器地址
-- 对应 Step 2524 用户提供的 Host，但修正为文档中的 API 网关

UPDATE sys_erp_config 
SET config_json = jsonb_set(
    config_json::jsonb, 
    '{baseUrl}', 
    '"https://api.eteams.cn"'
)::text
WHERE erp_type = 'weaver_e10';
