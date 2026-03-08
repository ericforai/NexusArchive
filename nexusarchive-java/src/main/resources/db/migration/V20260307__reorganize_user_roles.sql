-- Input: sys_user, sys_user_role 表
-- Output: 重新分配用户角色，符合三员分立原则
-- Pos: 数据库迁移 V20260307
-- 重新组织用户角色分配
--
-- 规划方案：
-- ├── admin      → system_admin   (系统管理员：运维、配置)
-- ├── security   → security_admin (安全保密员：用户、角色管理)
-- ├── auditor    → audit_admin    (安全审计员：审计验真)
-- ├── zhangsan   → business_user  (业务操作员)
-- ├── lisi       → business_user  (业务操作员)
-- ├── wangwu     → business_user  (业务操作员)
-- ├── zhaoliu    → business_user  (业务操作员)
-- └── qianqi     → query_user     (查询用户：仅借阅)

-- 第一步：清除现有所有用户-角色关联
DELETE FROM sys_user_role;

-- 第二步：重新分配角色

-- 1. admin → system_admin (系统管理员)
INSERT INTO sys_user_role (user_id, role_id)
VALUES ('user_admin_001', 'role_system_admin')
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 2. 创建安全保密员账号 security (如果不存在) 并分配角色
INSERT INTO sys_user (id, username, password_hash, full_name, email, status, org_code, deleted, created_time, last_modified_time)
VALUES (
    'user_security_001',
    'security',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5NU9xKZF1yVJW',  -- 密码: Security@123
    '安全保密员',
    'security@nexusarchive.local',
    'active',
    'ORG001',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id)
VALUES ('user_security_001', 'role_security_admin')
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 3. 创建审计管理员账号 auditor 并分配角色
INSERT INTO sys_user (id, username, password_hash, full_name, email, status, org_code, deleted, created_time, last_modified_time)
VALUES (
    'user_auditor_001',
    'auditor',
    '$2a$12$FjZ4tG4yH8YqKmN3pL9X8OYz6TtxMQJqhN8/LewY5NU9xKZF1yVJW',  -- 密码: Auditor@123
    '安全审计员',
    'auditor@nexusarchive.local',
    'active',
    'ORG001',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id)
VALUES ('user_auditor_001', 'role_audit_admin')
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 4. 业务操作员：zhangsan, lisi, wangwu, zhaoliu
INSERT INTO sys_user_role (user_id, role_id)
VALUES
    ('user-zhangsan', 'role_business_user'),
    ('user-lisi', 'role_business_user'),
    ('user-wangwu', 'role_business_user'),
    ('user-zhaoliu', 'role_business_user')
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 5. 查询用户：qianqi
INSERT INTO sys_user_role (user_id, role_id)
VALUES ('user-qianqi', 'role_query_user')
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 确保新用户也有全宗权限（使用默认全宗 F001）
INSERT INTO sys_user_fonds_scope (id, user_id, fonds_no, scope_type, deleted)
VALUES
    ('ufs_security_001', 'user_security_001', 'F001', 'all', 0),
    ('ufs_auditor_001', 'user_auditor_001', 'F001', 'all', 0)
ON CONFLICT (id) DO NOTHING;

-- 验证结果
-- SELECT u.username, u.full_name, r.code as role_code, r.name as role_name
-- FROM sys_user u
-- JOIN sys_user_role ur ON u.id = ur.user_id
-- JOIN sys_role r ON ur.role_id = r.id
-- WHERE u.deleted = 0
-- ORDER BY r.code, u.username;
