-- V60: 集成中心模板预置与交付优化
-- 目的是为私有化部署提供开箱即用的标准配置模板

-- 1. 预置金蝶云星空标准模板 (如果不存在)
INSERT INTO sys_erp_config (name, erp_type, config_json, is_active, created_time, last_modified_time)
SELECT '金蝶云星空 (标准模板)', 'kingdee', '{"baseUrl":"https://api.kingdee.com/k3cloud/", "appKey":"YOUR_APP_KEY", "appSecret":"YOUR_APP_SECRET"}', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_erp_config WHERE name = '金蝶云星空 (标准模板)');

-- 2. 预置泛微 OA 标准模板 (如果不存在)
INSERT INTO sys_erp_config (name, erp_type, config_json, is_active, created_time, last_modified_time)
SELECT '泛微 OA (标准模板)', 'weaver', '{"baseUrl":"http://YOUR_OA_HOST/weaver/", "appKey":"YOUR_APP_KEY", "appSecret":"YOUR_APP_SECRET"}', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_erp_config WHERE name = '泛微 OA (标准模板)');

-- 3. 预置用友 YonSuite 标准模板 (如果不存在)
INSERT INTO sys_erp_config (name, erp_type, config_json, is_active, created_time, last_modified_time)
SELECT '用友 YonSuite (标准模板)', 'yonsuite', '{"baseUrl":"https://api.yonyoucloud.com/iuap-api-gateway", "appKey":"YOUR_APP_KEY", "appSecret":"YOUR_APP_SECRET"}', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_erp_config WHERE name = '用友 YonSuite (标准模板)');

-- 4. 强制刷新子接口元数据 (确保所有凭证同步场景都有基础子接口控制)
-- 这一步是为了防止手动插入 config 后场景未初始化导致子接口缺失
-- 实际场景初始化建议通过代码逻辑保证，但迁移脚本可以作为兜底

-- 为所有 yonsuite 类型的场景 增加 VOUCHER_SYNC 默认子接口
INSERT INTO sys_erp_sub_interface (scenario_id, interface_key, interface_name, description, sort_order, is_active)
SELECT s.id, 'LIST_QUERY', '凭证列表查询', '查询指定期间的凭证列表', 1, TRUE
FROM sys_erp_scenario s
WHERE s.scenario_key = 'VOUCHER_SYNC' AND NOT EXISTS (
    SELECT 1 FROM sys_erp_sub_interface sub WHERE sub.scenario_id = s.id AND sub.interface_key = 'LIST_QUERY'
);

INSERT INTO sys_erp_sub_interface (scenario_id, interface_key, interface_name, description, sort_order, is_active)
SELECT s.id, 'DETAIL_QUERY', '凭证详情查询', '获取单个凭证的完整信息', 2, TRUE
FROM sys_erp_scenario s
WHERE s.scenario_key = 'VOUCHER_SYNC' AND NOT EXISTS (
    SELECT 1 FROM sys_erp_sub_interface sub WHERE sub.scenario_id = s.id AND sub.interface_key = 'DETAIL_QUERY'
);
