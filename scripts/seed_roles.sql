INSERT INTO sys_role (id, name, code, description, created_at, updated_at)
VALUES 
('role_auditor', '审计员', 'auditor', '负责审计日志查看与合规检查', NOW(), NOW()),
('role_user', '普通用户', 'user', '负责档案归档与查询', NOW(), NOW())
ON CONFLICT (code) DO NOTHING;
