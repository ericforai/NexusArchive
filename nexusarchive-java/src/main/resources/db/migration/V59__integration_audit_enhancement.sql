-- V59: 集成中心合规增强 - 同步历史审计补全
-- 符合 GB/T 39362 对采集过程的审计要求

-- 1. 为 sys_sync_history 添加审计字段
ALTER TABLE sys_sync_history ADD COLUMN IF NOT EXISTS operator_id BIGINT;
ALTER TABLE sys_sync_history ADD COLUMN IF NOT EXISTS client_ip VARCHAR(50);

COMMENT ON COLUMN sys_sync_history.operator_id IS '操作人ID';
COMMENT ON COLUMN sys_sync_history.client_ip IS '操作客户端IP';

-- 2. 为审计字段创建索引加速追溯
CREATE INDEX IF NOT EXISTS idx_sync_history_operator ON sys_sync_history(operator_id);
