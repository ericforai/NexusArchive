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
    entity_id VARCHAR(64) NOT NULL COMMENT '法人ID',
    config_type VARCHAR(50) NOT NULL COMMENT '配置类型: ERP_INTEGRATION, BUSINESS_RULE, COMPLIANCE_POLICY',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值（JSON格式）',
    description VARCHAR(500) COMMENT '配置描述',
    created_by VARCHAR(64) COMMENT '创建人ID',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    UNIQUE KEY uk_entity_config (entity_id, config_type, config_key, deleted)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_entity_config_entity ON sys_entity_config(entity_id);
CREATE INDEX IF NOT EXISTS idx_entity_config_type ON sys_entity_config(config_type);
CREATE INDEX IF NOT EXISTS idx_entity_config_deleted ON sys_entity_config(deleted);

-- 表注释
COMMENT ON TABLE sys_entity_config IS '法人配置表（每个法人独立的配置）';
COMMENT ON COLUMN sys_entity_config.config_type IS '配置类型: ERP_INTEGRATION(ERP集成), BUSINESS_RULE(业务规则), COMPLIANCE_POLICY(合规策略)';

