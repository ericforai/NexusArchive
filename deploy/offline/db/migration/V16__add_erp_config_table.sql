-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- ERP对接配置表
CREATE TABLE IF NOT EXISTS sys_erp_config (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    erp_type VARCHAR(50) NOT NULL,
    config_json TEXT NOT NULL,
    is_active SMALLINT DEFAULT 1,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_erp_config IS 'ERP对接配置表';
COMMENT ON COLUMN sys_erp_config.name IS '配置名称';
COMMENT ON COLUMN sys_erp_config.erp_type IS 'ERP类型: YONSUITE/KINGDEE/GENERIC';
COMMENT ON COLUMN sys_erp_config.config_json IS '配置参数JSON';
COMMENT ON COLUMN sys_erp_config.is_active IS '是否启用';
