-- 添加缺失的权限定义
-- 这些权限被 EntityController 和 EnterpriseArchitectureController 使用

-- entity:view - 查看法人实体权限
INSERT INTO sys_permission (id, perm_key, label, group_name) VALUES
('perm_view_entity', 'entity:view', '查看法人', 'entity')
ON CONFLICT (perm_key) DO NOTHING;

-- entity:manage - 管理法人实体权限
INSERT INTO sys_permission (id, perm_key, label, group_name) VALUES
('perm_manage_entity', 'entity:manage', '管理法人', 'entity')
ON CONFLICT (perm_key) DO NOTHING;

-- fonds:view - 查看全宗权限
INSERT INTO sys_permission (id, perm_key, label, group_name) VALUES
('perm_view_fonds', 'fonds:view', '查看全宗', 'fonds')
ON CONFLICT (perm_key) DO NOTHING;

-- 为系统管理员角色添加这些权限 (permissions 字段是 JSON 数组)
UPDATE sys_role
SET permissions = permissions || '["entity:view","entity:manage","fonds:view"]'::jsonb
WHERE code = 'system_admin';

-- 为超级管理员角色添加这些权限
UPDATE sys_role
SET permissions = permissions || '["entity:view","entity:manage","fonds:view"]'::jsonb
WHERE code = 'super_admin';
