-- 添加泛微 E10 系统连接配置

INSERT INTO sys_erp_config (
    name, 
    erp_type, 
    config_json, 
    is_active, 
    created_time, 
    last_modified_time
) VALUES (
    '泛微E10中台', 
    'weaver_e10', 
    '{"baseUrl": "http://e10.demo.com", "clientId": "e10_demo_id", "clientSecret": "e10_demo_secret", "tenantId": "1001"}', 
    1, 
    NOW(), 
    NOW()
);
