-- V3: 修复 sys_audit_log 表 Schema 不一致问题
-- 2025-12-29: 添加 client_ip 列以匹配 Entity 和查询需求

DO $$
BEGIN
    -- 检查并添加 client_ip 列
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'sys_audit_log' AND column_name = 'client_ip'
    ) THEN
        ALTER TABLE sys_audit_log ADD COLUMN client_ip VARCHAR(50);
        COMMENT ON COLUMN sys_audit_log.client_ip IS '客户端IP地址 (DA/T 94 要求)';
        
        -- 数据迁移：如果 ip_address 有值，拷贝到 client_ip
        UPDATE sys_audit_log SET client_ip = ip_address WHERE client_ip IS NULL;
    END IF;

    -- 确保 mac_address 允许 NULL 或有默认值
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'sys_audit_log' AND column_name = 'mac_address' AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE sys_audit_log ALTER COLUMN mac_address DROP NOT NULL;
    END IF;

END $$;
