-- 更新泛微 E10 真实凭证
-- 对应 Step 2506 用户提供的 Key/Secret

UPDATE sys_erp_config 
SET config_json = '{"baseUrl": "http://e10.demo.com", "clientId": "7577f814096e611038c5eff1479d3b9", "clientSecret": "cdc0d6c9bc39312bd6288ced1789a49", "tenantId": "1001"}'
WHERE erp_type = 'weaver_e10';
