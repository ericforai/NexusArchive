UPDATE sys_erp_config
SET config_json = jsonb_set(
    config_json::jsonb,
    '{accbookCode}',
    '"BRYS002"'
)::text,
last_modified_time = NOW()
WHERE id = 7;
