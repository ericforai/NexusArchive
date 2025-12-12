-- ============================================
-- V27: ERP 配置表
-- 支持多 ERP 系统配置管理
-- Agent D (基础设施工程师) - 2025-12-07
-- ============================================

CREATE TABLE IF NOT EXISTS bas_erp_config (
    id              VARCHAR(32) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,       -- 配置名称
    adapter_type    VARCHAR(50) NOT NULL,        -- 适配器类型 (yonsuite, kingdee, generic)
    base_url        VARCHAR(500) NOT NULL,       -- API 基础 URL
    app_key         VARCHAR(200),                -- AppKey (SM4 加密)
    app_secret      VARCHAR(500),                -- AppSecret (SM4 加密)
    tenant_id       VARCHAR(100),                -- 租户ID (如适用)
    accbook_code    VARCHAR(100),                -- 账套代码 (如适用)
    extra_config    TEXT,                        -- 额外配置 (JSON)
    enabled         BOOLEAN DEFAULT TRUE,
    created_by      VARCHAR(32),
    created_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_erp_config_name UNIQUE (name)
);

-- 适配器类型索引
CREATE INDEX idx_erp_config_type ON bas_erp_config(adapter_type);

-- 启用状态索引
CREATE INDEX idx_erp_config_enabled ON bas_erp_config(enabled);

COMMENT ON TABLE bas_erp_config IS 'ERP 配置表';
COMMENT ON COLUMN bas_erp_config.adapter_type IS '适配器类型: yonsuite=用友, kingdee=金蝶, generic=通用';
COMMENT ON COLUMN bas_erp_config.app_secret IS '应用密钥 (SM4 加密存储)';
COMMENT ON COLUMN bas_erp_config.extra_config IS '额外配置 (JSON格式，用于通用适配器字段映射等)';
