-- Input: sys_role 表权限字段
-- Output: 为审计管理员角色添加 nav:audit 权限
-- Pos: 数据库迁移 V20260306
-- 修复审计验真菜单不可见问题
--
-- 问题说明：
-- 前端审计验真菜单配置要求 nav:audit 权限才能显示
-- 但后端审计管理员 (audit_admin) 角色权限中缺少此权限
--
-- 修复方案（遵循三员分立原则）：
-- - 仅审计管理员 (audit_admin) 添加 nav:audit 导航权限
-- - 系统管理员和安全保密员不得拥有审计验真权限（防止自证清白）
-- - 超级管理员仅用于开发/测试环境，可添加全部权限

-- 为审计管理员角色添加 nav:audit 权限（审计验真菜单访问权限）
UPDATE sys_role
SET permissions = permissions || '["nav:audit","audit:verify","audit:evidence"]'::jsonb,
    last_modified_time = CURRENT_TIMESTAMP
WHERE code = 'audit_admin'
  AND deleted = 0
  AND permissions::text NOT LIKE '%"nav:audit"%';

-- 为超级管理员添加审计验真相关权限（仅开发/测试环境）
UPDATE sys_role
SET permissions = permissions || '["nav:audit","audit:verify","audit:evidence"]'::jsonb,
    last_modified_time = CURRENT_TIMESTAMP
WHERE code = 'super_admin'
  AND deleted = 0
  AND permissions::text NOT LIKE '%"nav:audit"%';

-- 注意：系统管理员 (system_admin) 和安全保密员 (security_admin) 按三员分立原则
-- 不应拥有审计验真权限，防止"自己审核自己"的合规问题

-- 验证权限已正确添加（可选，用于调试）
-- SELECT code, name, permissions
-- FROM sys_role
-- WHERE code IN ('audit_admin', 'system_admin', 'security_admin', 'super_admin')
--   AND deleted = 0;
