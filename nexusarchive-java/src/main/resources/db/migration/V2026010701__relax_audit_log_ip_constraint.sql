-- V82: 修复审计日志表 Schema 不一致及其非空约束问题
-- 2026-01-07: 移除 client_ip 的 NOT NULL 约束，确保登录审计日志持久化不被阻断

DO $$
BEGIN
    -- 1. 移除 client_ip 的 NOT NULL 约束 (如果存在)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'sys_audit_log' AND column_name = 'client_ip' AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE sys_audit_log ALTER COLUMN client_ip DROP NOT NULL;
    END IF;

    -- 2. 移除 ip_address 的 NOT NULL 约束 (如果存在)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'sys_audit_log' AND column_name = 'ip_address' AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE sys_audit_log ALTER COLUMN ip_address DROP NOT NULL;
    END IF;

    -- 3. 确保 client_ip 列存在，作为 DA/T 94 合规标准列名
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'sys_audit_log' AND column_name = 'client_ip'
    ) THEN
        ALTER TABLE sys_audit_log ADD COLUMN client_ip VARCHAR(64);
        COMMENT ON COLUMN sys_audit_log.client_ip IS '客户端IP地址 (DA/T 94 标准列)';
    END IF;

END $$;
