-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- V4: 创建审计日志表 sys_audit_log (修复原 ALTER 脚本执行失败的问题)
-- 替代了原有的 ALTER TABLE acc_archive 和 ALTER TABLE sys_audit_log 逻辑

-- 1. 确保 acc_archive 相关操作不报错（原逻辑已废弃，直接忽略）
-- (ArcFileContent 对应 arc_file_content，且字段名已正确)

-- 2. 创建 sys_audit_log 表 (若不存在)
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64),
    username VARCHAR(255),
    role_type VARCHAR(50),
    action VARCHAR(50) NOT NULL, -- 操作类型
    resource_type VARCHAR(50),
    resource_id VARCHAR(64),
    operation_result VARCHAR(50),
    risk_level VARCHAR(20),
    details TEXT,
    data_before TEXT,
    data_after TEXT,
    session_id VARCHAR(64),
    ip_address VARCHAR(50) NOT NULL,
    mac_address VARCHAR(64) DEFAULT 'UNKNOWN' NOT NULL,
    object_digest VARCHAR(128),
    user_agent VARCHAR(500),
    prev_log_hash VARCHAR(128),
    log_hash VARCHAR(128),
    device_fingerprint VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. 添加列注释
COMMENT ON TABLE sys_audit_log IS '安全审计日志表';
COMMENT ON COLUMN sys_audit_log.action IS '操作类型: CAPTURE, ARCHIVE, MODIFY_META, DESTROY, PRINT, DOWNLOAD';
COMMENT ON COLUMN sys_audit_log.ip_address IS '客户端IP地址';
COMMENT ON COLUMN sys_audit_log.mac_address IS 'MAC地址';
COMMENT ON COLUMN sys_audit_log.data_before IS '操作前数据快照';
COMMENT ON COLUMN sys_audit_log.data_after IS '操作后数据快照';
COMMENT ON COLUMN sys_audit_log.object_digest IS '被操作对象的哈希值';
COMMENT ON COLUMN sys_audit_log.log_hash IS '本条日志的防篡改哈希';
COMMENT ON COLUMN sys_audit_log.prev_log_hash IS '上一条日志的哈希(链式存储)';
