-- Input: 数据库引擎
-- Output: 角色/权限数据更新
-- Pos: 权限数据脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

UPDATE sys_role SET permissions = '["audit:view", "audit_logs", "nav:portal"]' WHERE code = 'auditor';
UPDATE sys_role SET permissions = '["archive:read", "archive:manage", "nav:archive_mgmt", "nav:query", "nav:portal"]' WHERE code = 'user';
