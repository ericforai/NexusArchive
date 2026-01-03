-- Input: 数据库引擎
-- Output: sys_entity_config 表创建脚本
-- Pos: Flyway 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- ----------------------------
-- 法人配置表 (Entity Configuration)
-- ----------------------------
-- 说明: 每个法人可以有自己的独立配置（ERP接口、业务规则、合规策略等）
CREATE TABLE IF NOT EXISTS sys_entity_config (
    id VARCHAR(64) PRIMARY KEY,
    entity_id VARCHAR(64) NOT NULL,
    config_type VARCHAR(50) NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    description VARCHAR(500),
    created_by VARCHAR(64),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0,
    CONSTRAINT uk_entity_config UNIQUE (entity_id, config_type, config_key, deleted)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_entity_config_entity ON sys_entity_config(entity_id);
CREATE INDEX IF NOT EXISTS idx_entity_config_type ON sys_entity_config(config_type);
CREATE INDEX IF NOT EXISTS idx_entity_config_deleted ON sys_entity_config(deleted);

-- 列注释
COMMENT ON TABLE sys_entity_config IS '法人配置表（每个法人独立的配置）';
COMMENT ON COLUMN sys_entity_config.entity_id IS '法人ID';
COMMENT ON COLUMN sys_entity_config.config_type IS '配置类型: ERP_INTEGRATION(ERP集成), BUSINESS_RULE(业务规则), COMPLIANCE_POLICY(合规策略)';
COMMENT ON COLUMN sys_entity_config.config_key IS '配置键';
COMMENT ON COLUMN sys_entity_config.config_value IS '配置值（JSON格式）';
COMMENT ON COLUMN sys_entity_config.description IS '配置描述';
COMMENT ON COLUMN sys_entity_config.created_by IS '创建人ID';
COMMENT ON COLUMN sys_entity_config.created_time IS '创建时间';
COMMENT ON COLUMN sys_entity_config.updated_time IS '更新时间';
COMMENT ON COLUMN sys_entity_config.deleted IS '逻辑删除: 0=未删除, 1=已删除';

-- 创建触发器函数用于自动更新 updated_time
CREATE OR REPLACE FUNCTION update_sys_entity_config_updated_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 创建触发器
DROP TRIGGER IF EXISTS trigger_update_entity_config_updated_time ON sys_entity_config;
CREATE TRIGGER trigger_update_entity_config_updated_time
    BEFORE UPDATE ON sys_entity_config
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_entity_config_updated_time();

