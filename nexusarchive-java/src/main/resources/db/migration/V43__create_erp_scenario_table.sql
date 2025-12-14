-- Create ERP Scenario table (Layer 2)
CREATE TABLE IF NOT EXISTS sys_erp_scenario (
    id BIGSERIAL PRIMARY KEY,
    config_id BIGINT NOT NULL,
    scenario_key VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN DEFAULT FALSE,
    sync_strategy VARCHAR(50) DEFAULT 'MANUAL', -- MANUAL, CRON, REALTIME
    cron_expression VARCHAR(100), -- For CRON strategy
    last_sync_time TIMESTAMP,
    last_sync_status VARCHAR(50), -- SUCCESS, FAIL, PARTIAL
    last_sync_msg TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (config_id) REFERENCES sys_erp_config(id) ON DELETE CASCADE,
    CONSTRAINT uk_config_scenario UNIQUE (config_id, scenario_key)
);

-- Comments
COMMENT ON TABLE sys_erp_scenario IS 'ERP业务场景配置表 (Layer 2)';
COMMENT ON COLUMN sys_erp_scenario.scenario_key IS '场景唯一标识 (如 VOUCHER_SYNC)';
COMMENT ON COLUMN sys_erp_scenario.sync_strategy IS '同步策略: MANUAL=手动, CRON=定时, REALTIME=实时';
