-- 1. Update YonSuite Config with User Provided AppKey and AppSecret
UPDATE sys_erp_config
SET config_json = '{
    "baseUrl": "https://dbox.yonyoucloud.com/iuap-api-gateway",
    "appKey": "96a95c00982446cba484ccc4936b221b",
    "appSecret": "e9a58fd35f3ca3f0a46d27b8859758b1ed35f0b6",
    "accbookCode": "BR01",
    "extraConfig": ""
}'
WHERE id = 1;

-- 2. Add PAYMENT_FILE_SYNC Scenario (Idempotent)
INSERT INTO sys_erp_scenario (
    config_id,
    scenario_key,
    name,
    description,
    is_active,
    sync_strategy,
    created_time,
    last_modified_time
)
SELECT 1, 'PAYMENT_FILE_SYNC', '付款单文件获取', '从YonSuite获取资金结算文件 (AI Integration)', TRUE, 'MANUAL', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_erp_scenario WHERE scenario_key = 'PAYMENT_FILE_SYNC'
);
