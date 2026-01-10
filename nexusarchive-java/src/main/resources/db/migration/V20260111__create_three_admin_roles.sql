-- Input: sys_role 表结构
-- Output: 三员角色记录 (system_admin, security_admin, audit_admin)
-- Pos: 数据库迁移 V20260111
-- 创建等保2.0三级要求的"三员分立"角色
--
-- 根据 GB/T 39784-2021《电子档案管理系统通用要求》：
-- - 系统管理员：负责系统运维和配置管理
-- - 安全保密员：负责用户权限分配、角色管理、密钥管理
-- - 安全审计员：负责查看和审计系统日志
--
-- 三员角色互斥：同一用户不能同时拥有多个三员角色

-- 系统管理员角色
-- 职责：系统运维、配置管理、备份恢复、性能监控
-- 权限：系统设置、数据备份、日志查看（只读）、基础监控
-- 不包含：用户管理、角色管理（由安全保密员负责）
INSERT INTO sys_role (
    id,
    name,
    code,
    role_category,
    is_exclusive,
    description,
    permissions,
    data_scope,
    type,
    created_at,
    last_modified_time,
    deleted
) VALUES (
    'role_system_admin',
    '系统管理员',
    'system_admin',
    'system_admin',
    true,
    '负责系统运维和配置管理（三员分立之一）',
    '["nav:portal","nav:settings","manage_settings","system:backup","system:restore","system:monitor","archive:read","archive:view","audit:view"]',
    'all',
    'system',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
) ON CONFLICT (id) DO NOTHING;

-- 安全保密员角色
-- 职责：用户权限分配、角色管理、密钥管理、组织架构管理
-- 权限：用户管理、角色管理、权限分配、组织架构管理
-- 不包含：系统配置修改（由系统管理员负责）、审计日志修改
INSERT INTO sys_role (
    id,
    name,
    code,
    role_category,
    is_exclusive,
    description,
    permissions,
    data_scope,
    type,
    created_at,
    last_modified_time,
    deleted
) VALUES (
    'role_security_admin',
    '安全保密员',
    'security_admin',
    'security_admin',
    true,
    '负责用户权限分配、角色管理、密钥管理（三员分立之一）',
    '["nav:portal","nav:settings","manage_users","manage_roles","manage_positions","manage_org","reset_password","view_users","view_roles","archive:read","archive:view"]',
    'all',
    'system',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
) ON CONFLICT (id) DO NOTHING;

-- 安全审计员角色
-- 职责：查看和审计系统日志、生成审计报告
-- 权限：审计日志查看、审计报表导出、操作追踪
-- 不包含：任何修改权限
INSERT INTO sys_role (
    id,
    name,
    code,
    role_category,
    is_exclusive,
    description,
    permissions,
    data_scope,
    type,
    created_at,
    last_modified_time,
    deleted
) VALUES (
    'role_audit_admin',
    '安全审计员',
    'audit_admin',
    'audit_admin',
    true,
    '负责查看和审计系统日志（三员分立之一）',
    '["nav:portal","audit:view","audit:export","audit:trace","archive:read","archive:view"]',
    'all',
    'system',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
) ON CONFLICT (id) DO NOTHING;

-- 添加注释说明
COMMENT ON COLUMN sys_role.is_exclusive IS '是否为互斥角色（三员角色必须互斥，同一用户不能同时拥有多个互斥角色）';
COMMENT ON COLUMN sys_role.role_category IS '角色类别：system_admin-系统管理员, security_admin-安全保密员, audit_admin-安全审计员, business_user-业务操作员';
