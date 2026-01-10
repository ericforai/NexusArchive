-- Input: sys_user_role, sys_role 表
-- Output: 为没有有效角色的用户分配 business_user 角色
-- Pos: 数据库迁移 V2026010904
-- 修复已创建但没有有效角色的用户

-- 删除无效的用户-角色关联（role_id 在 sys_role 中不存在的）
DELETE FROM sys_user_role
WHERE role_id NOT IN (SELECT id FROM sys_role WHERE deleted = 0);

-- 为没有角色的用户分配默认 business_user 角色
-- 只分配给状态为 active 且没有任何有效角色的用户
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, 'role_business_user'
FROM sys_user u
WHERE u.status = 'active'
  AND u.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur
    INNER JOIN sys_role r ON ur.role_id = r.id
    WHERE ur.user_id = u.id AND r.deleted = 0
  )
ON CONFLICT (user_id, role_id) DO NOTHING;
