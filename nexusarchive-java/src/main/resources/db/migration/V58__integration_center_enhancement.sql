-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- V58: 集成中心增强 - 同步历史与子接口配置
-- 支持：同步历史记录、子接口级别开关控制

-- 1. 同步历史记录表
CREATE TABLE IF NOT EXISTS sys_sync_history (
    id BIGSERIAL PRIMARY KEY,
    scenario_id BIGINT NOT NULL,
    sync_start_time TIMESTAMP,
    sync_end_time TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING', -- RUNNING, SUCCESS, FAIL
    total_count INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    fail_count INTEGER DEFAULT 0,
    error_message TEXT,
    sync_params TEXT, -- JSON: 同步参数如日期范围
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_sync_history IS '同步历史记录表';
COMMENT ON COLUMN sys_sync_history.scenario_id IS '关联的场景ID';
COMMENT ON COLUMN sys_sync_history.status IS '同步状态: RUNNING/SUCCESS/FAIL';
COMMENT ON COLUMN sys_sync_history.sync_params IS 'JSON格式的同步参数';

-- 创建索引加速查询
CREATE INDEX IF NOT EXISTS idx_sync_history_scenario ON sys_sync_history(scenario_id);
CREATE INDEX IF NOT EXISTS idx_sync_history_time ON sys_sync_history(sync_start_time DESC);

-- 2. 子接口配置表 (支持场景内子接口开关控制)
CREATE TABLE IF NOT EXISTS sys_erp_sub_interface (
    id BIGSERIAL PRIMARY KEY,
    scenario_id BIGINT NOT NULL,
    interface_key VARCHAR(100) NOT NULL, -- 如: LIST_QUERY, DETAIL_QUERY, ATTACHMENT_QUERY
    interface_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INTEGER DEFAULT 0,
    config_json TEXT, -- JSON: 接口特定配置参数
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_erp_sub_interface IS '场景子接口配置表';
COMMENT ON COLUMN sys_erp_sub_interface.interface_key IS '接口标识如LIST_QUERY/DETAIL_QUERY';
COMMENT ON COLUMN sys_erp_sub_interface.config_json IS 'JSON格式的接口配置参数';

-- 创建唯一索引确保场景内接口不重复
CREATE UNIQUE INDEX IF NOT EXISTS idx_sub_interface_unique ON sys_erp_sub_interface(scenario_id, interface_key);

-- 3. 为场景表添加参数配置字段
ALTER TABLE sys_erp_scenario ADD COLUMN IF NOT EXISTS params_json TEXT;
COMMENT ON COLUMN sys_erp_scenario.params_json IS 'JSON格式的场景参数配置';

-- 4. 初始化 YonSuite 凭证同步场景的子接口
-- 先检查是否存在凭证同步场景
INSERT INTO sys_erp_sub_interface (scenario_id, interface_key, interface_name, description, sort_order, is_active)
SELECT s.id, 'LIST_QUERY', '凭证列表查询', '查询指定期间的凭证列表', 1, TRUE
FROM sys_erp_scenario s
WHERE s.scenario_key = 'VOUCHER_SYNC' AND NOT EXISTS (
    SELECT 1 FROM sys_erp_sub_interface sub 
    WHERE sub.scenario_id = s.id AND sub.interface_key = 'LIST_QUERY'
);

INSERT INTO sys_erp_sub_interface (scenario_id, interface_key, interface_name, description, sort_order, is_active)
SELECT s.id, 'DETAIL_QUERY', '凭证详情查询', '获取单个凭证的完整信息', 2, TRUE
FROM sys_erp_scenario s
WHERE s.scenario_key = 'VOUCHER_SYNC' AND NOT EXISTS (
    SELECT 1 FROM sys_erp_sub_interface sub 
    WHERE sub.scenario_id = s.id AND sub.interface_key = 'DETAIL_QUERY'
);

INSERT INTO sys_erp_sub_interface (scenario_id, interface_key, interface_name, description, sort_order, is_active)
SELECT s.id, 'PDF_GENERATE', 'PDF版式生成', '将凭证数据转换为PDF文件', 3, TRUE
FROM sys_erp_scenario s
WHERE s.scenario_key = 'VOUCHER_SYNC' AND NOT EXISTS (
    SELECT 1 FROM sys_erp_sub_interface sub 
    WHERE sub.scenario_id = s.id AND sub.interface_key = 'PDF_GENERATE'
);

-- 5. 初始化收款单同步场景的子接口
INSERT INTO sys_erp_sub_interface (scenario_id, interface_key, interface_name, description, sort_order, is_active)
SELECT s.id, 'LIST_QUERY', '收款单列表查询', '查询指定期间的收款单列表', 1, TRUE
FROM sys_erp_scenario s
WHERE s.scenario_key = 'COLLECTION_FILE_SYNC' AND NOT EXISTS (
    SELECT 1 FROM sys_erp_sub_interface sub 
    WHERE sub.scenario_id = s.id AND sub.interface_key = 'LIST_QUERY'
);

INSERT INTO sys_erp_sub_interface (scenario_id, interface_key, interface_name, description, sort_order, is_active)
SELECT s.id, 'FILE_DOWNLOAD', '收款单文件下载', '下载收款单关联的文件', 2, TRUE
FROM sys_erp_scenario s
WHERE s.scenario_key = 'COLLECTION_FILE_SYNC' AND NOT EXISTS (
    SELECT 1 FROM sys_erp_sub_interface sub 
    WHERE sub.scenario_id = s.id AND sub.interface_key = 'FILE_DOWNLOAD'
);

-- 6. 初始化付款单同步场景的子接口
INSERT INTO sys_erp_sub_interface (scenario_id, interface_key, interface_name, description, sort_order, is_active)
SELECT s.id, 'LIST_QUERY', '付款单列表查询', '查询指定期间的付款单列表', 1, TRUE
FROM sys_erp_scenario s
WHERE s.scenario_key = 'PAYMENT_FILE_SYNC' AND NOT EXISTS (
    SELECT 1 FROM sys_erp_sub_interface sub 
    WHERE sub.scenario_id = s.id AND sub.interface_key = 'LIST_QUERY'
);

INSERT INTO sys_erp_sub_interface (scenario_id, interface_key, interface_name, description, sort_order, is_active)
SELECT s.id, 'FILE_DOWNLOAD', '付款单文件下载', '下载付款单关联的文件', 2, TRUE
FROM sys_erp_scenario s
WHERE s.scenario_key = 'PAYMENT_FILE_SYNC' AND NOT EXISTS (
    SELECT 1 FROM sys_erp_sub_interface sub 
    WHERE sub.scenario_id = s.id AND sub.interface_key = 'FILE_DOWNLOAD'
);
