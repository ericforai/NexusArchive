-- V24__enhance_audit_log.sql
-- 审计日志表增强 - 添加哈希链防篡改字段
-- 
-- 合规要求:
-- - GB/T 39784-2021 表36: 审计日志必须防篡改
-- - 使用哈希链确保日志不可被修改
--
-- @author Agent B - 合规开发工程师

-- 添加前一条日志哈希字段
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS prev_log_hash VARCHAR(64);

-- 添加当前日志哈希字段
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS log_hash VARCHAR(64);

-- 添加设备指纹字段
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS device_fingerprint VARCHAR(200);

-- 创建哈希索引，便于链条验证
CREATE INDEX IF NOT EXISTS idx_audit_log_hash ON sys_audit_log(log_hash);

-- 创建时间索引，便于按时间范围查询验证
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON sys_audit_log(created_at);

-- 添加注释
COMMENT ON COLUMN sys_audit_log.prev_log_hash IS '前一条日志的SM3哈希值';
COMMENT ON COLUMN sys_audit_log.log_hash IS '当前日志的SM3哈希值';
COMMENT ON COLUMN sys_audit_log.device_fingerprint IS '客户端设备指纹';
