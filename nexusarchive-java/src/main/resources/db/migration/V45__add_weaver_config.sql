-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 添加泛微 OA 系统连接配置
-- 对应 Task 1: 新增连接泛微OA系统

INSERT INTO sys_erp_config (
    name, 
    erp_type, 
    config_json, 
    is_active, 
    created_time, 
    last_modified_time
) VALUES (
    '泛微OA系统', 
    'weaver', 
    '{"baseUrl": "http://oa.nexus-demo.com", "appKey": "weaver_demo_key", "appSecret": "weaver_demo_secret", "accbookCode": "WEAVER01"}', 
    1, 
    NOW(), 
    NOW()
);
