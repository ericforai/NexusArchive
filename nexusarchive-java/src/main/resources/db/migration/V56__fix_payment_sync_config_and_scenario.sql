DO $$
DECLARE
    v_yonsuite_id BIGINT;
BEGIN
    -- 1. Find the correct YonSuite config ID (e.g., id=7)
    SELECT id INTO v_yonsuite_id FROM sys_erp_config WHERE erp_type ILIKE 'yonsuite' LIMIT 1;

    IF v_yonsuite_id IS NOT NULL THEN
        -- 2. Apply the intended credentials to the actual YonSuite config
        -- (These were previously incorrectly applied to Kingdee config by V53)
        UPDATE sys_erp_config
        SET config_json = '{
            "baseUrl": "https://dbox.yonyoucloud.com/iuap-api-gateway",
            "appKey": "96a95c00982446cba484ccc4936b221b",
            "appSecret": "e9a58fd35f3ca3f0a46d27b8859758b1ed35f0b6",
            "accbookCode": "BR01",
            "extraConfig": ""
        }',
        last_modified_time = NOW()
        WHERE id = v_yonsuite_id;

        -- 3. Repoint the PAYMENT_FILE_SYNC scenario to the YonSuite config
        UPDATE sys_erp_scenario
        SET config_id = v_yonsuite_id,
            last_modified_time = NOW()
        WHERE scenario_key = 'PAYMENT_FILE_SYNC';
    END IF;
END $$;
