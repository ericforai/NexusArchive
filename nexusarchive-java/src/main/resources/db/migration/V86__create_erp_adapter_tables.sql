-- ERP AI 适配器表
-- 用于存储 AI 生成的 ERP 适配器信息

-- 适配器主表
CREATE TABLE IF NOT EXISTS sys_erp_adapter (
    adapter_id VARCHAR(100) PRIMARY KEY,
    adapter_name VARCHAR(200) NOT NULL,
    erp_type VARCHAR(50) NOT NULL,
    base_url VARCHAR(500),
    enabled BOOLEAN DEFAULT TRUE,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 适配器场景映射表
CREATE TABLE IF NOT EXISTS sys_erp_adapter_scenario (
    id SERIAL PRIMARY KEY,
    adapter_id VARCHAR(100) NOT NULL,
    scenario_code VARCHAR(50) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (adapter_id, scenario_code),
    CONSTRAINT fk_adapter_scenario FOREIGN KEY (adapter_id)
        REFERENCES sys_erp_adapter(adapter_id)
        ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_erp_adapter_type ON sys_erp_adapter(erp_type);
CREATE INDEX IF NOT EXISTS idx_erp_adapter_enabled ON sys_erp_adapter(enabled);
CREATE INDEX IF NOT EXISTS idx_erp_adapter_scenario_code ON sys_erp_adapter_scenario(scenario_code);

-- 注释
COMMENT ON TABLE sys_erp_adapter IS 'ERP AI 适配器主表';
COMMENT ON TABLE sys_erp_adapter_scenario IS 'ERP 适配器业务场景映射表';
COMMENT ON COLUMN sys_erp_adapter.adapter_id IS '适配器唯一标识';
COMMENT ON COLUMN sys_erp_adapter.adapter_name IS '适配器名称';
COMMENT ON COLUMN sys_erp_adapter.erp_type IS 'ERP 系统类型（yonsuite, kingdee 等）';
COMMENT ON COLUMN sys_erp_adapter.base_url IS 'API 基础URL';
COMMENT ON COLUMN sys_erp_adapter_scenario.scenario_code IS '场景代码（SALES_OUT, RECEIPT 等）';
