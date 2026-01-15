-- ================================================================
-- Migration: V101__sys_entity_add_parent_id.sql
-- Purpose: 将 sys_org 表合并到 sys_entity 表，添加层级支持
-- Author: System
-- Date: 2026-01-11
-- ================================================================

-- 1. 备份提醒（执行前请手动备份）
-- pg_dump -t sys_org -t sys_entity > backup_before_v101_$(date +%Y%m%d).sql

-- 2. 添加 parent_id 字段，建立法人层级关系
ALTER TABLE sys_entity ADD COLUMN IF NOT EXISTS parent_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_entity_parent_id ON sys_entity(parent_id);

-- 3. 添加 order_num 字段，支持排序
ALTER TABLE sys_entity ADD COLUMN IF NOT EXISTS order_num INTEGER DEFAULT 0;

-- 4. 添加注释
COMMENT ON COLUMN sys_entity.parent_id IS '父法人ID（用于集团层级：母公司-子公司）';
COMMENT ON COLUMN sys_entity.order_num IS '排序号';

-- 5. 迁移 sys_org 数据到 sys_entity
-- 使用 INSERT ... ON CONFLICT 避免主键冲突
INSERT INTO sys_entity (
    id,
    name,
    parent_id,
    order_num,
    status,
    description,
    created_time,
    updated_time,
    deleted
)
SELECT
    o.id,
    o.name,
    o.parent_id,
    o.order_num,
    'ACTIVE'::VARCHAR(20) AS status,
    o.name || '（从组织迁移）' AS description,
    o.created_time,
    o.updated_time,
    o.deleted
FROM sys_org o
WHERE o.deleted = 0
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    order_num = EXCLUDED.order_num;

-- 6. 记录迁移结果到审计日志
-- 注意：sys_audit_log 使用不同的列名
INSERT INTO sys_audit_log (
    id,
    user_id,
    username,
    action,
    resource_type,
    operation_result,
    risk_level,
    details,
    data_before,
    data_after,
    client_ip,
    created_time
)
SELECT
    gen_random_uuid()::VARCHAR(64),
    'SYSTEM'::VARCHAR(64),
    'SYSTEM'::VARCHAR(255),
    'MIGRATION'::VARCHAR(50),
    'sys_entity'::VARCHAR(50),
    'SUCCESS'::VARCHAR(50),
    'LOW'::VARCHAR(20),
    'Migrated from sys_org: ' || o.name,
    'sys_org:' || o.name,
    'Migrated to sys_entity',
    '127.0.0.1'::VARCHAR(64),
    CURRENT_TIMESTAMP
FROM sys_org o
WHERE o.deleted = 0;

-- 7. 删除 sys_org 表
DROP TABLE IF EXISTS sys_org CASCADE;

-- 8. 删除 sys_erp_config.org_id 字段（经代码审查确认未被使用）
ALTER TABLE sys_erp_config DROP COLUMN IF EXISTS org_id;
