-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

DO $$
DECLARE
    v_config_id BIGINT;
BEGIN
    -- 1. Get or Create Config (Ensure at least one yonsuite config exists)
    SELECT id INTO v_config_id FROM sys_erp_config WHERE erp_type ILIKE 'yonsuite' LIMIT 1;
    
    IF v_config_id IS NULL THEN
        INSERT INTO sys_erp_config (id, erp_type, name, config_json, is_active, created_time, last_modified_time)
        VALUES (1, 'YONSUITE', 'YonSuite Config', '{}', TRUE, NOW(), NOW())
        RETURNING id INTO v_config_id;
    END IF;

    -- 2. Update YonSuite Config with User Provided AppKey and AppSecret
    UPDATE sys_erp_config
    SET config_json = '{
        "baseUrl": "https://dbox.yonyoucloud.com/iuap-api-gateway",
        "appKey": "96a95c00982446cba484ccc4936b221b",
        "appSecret": "e9a58fd35f3ca3f0a46d27b8859758b1ed35f0b6",
        "accbookCode": "BR01",
        "extraConfig": ""
    }'
    WHERE id = v_config_id;

    -- 3. Add PAYMENT_FILE_SYNC Scenario (Idempotent)
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
    SELECT v_config_id, 'PAYMENT_FILE_SYNC', '付款单文件获取', '从YonSuite获取资金结算文件 (AI Integration)', TRUE, 'MANUAL', NOW(), NOW()
    WHERE NOT EXISTS (
        SELECT 1 FROM sys_erp_scenario WHERE scenario_key = 'PAYMENT_FILE_SYNC'
    );
END $$;
