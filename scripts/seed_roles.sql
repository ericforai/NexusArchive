-- Input: 数据库引擎
-- Output: 演示/初始化数据写入
-- Pos: 数据初始化脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

INSERT INTO sys_role (id, name, code, description, created_at, updated_at)
VALUES 
('role_auditor', '审计员', 'auditor', '负责审计日志查看与合规检查', NOW(), NOW()),
('role_user', '普通用户', 'user', '负责档案归档与查询', NOW(), NOW())
ON CONFLICT (code) DO NOTHING;
