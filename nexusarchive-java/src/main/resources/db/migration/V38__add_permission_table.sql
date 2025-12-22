-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- V28: Create sys_permission table
-- This table stores permission definitions that can be assigned to roles

CREATE TABLE IF NOT EXISTS sys_permission (
    id VARCHAR(64) PRIMARY KEY,
    perm_key VARCHAR(100) NOT NULL UNIQUE,
    label VARCHAR(100) NOT NULL,
    group_name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index for faster queries
CREATE INDEX IF NOT EXISTS idx_permission_group ON sys_permission(group_name);
CREATE INDEX IF NOT EXISTS idx_permission_key ON sys_permission(perm_key);

-- Insert default permissions
INSERT INTO sys_permission (id, perm_key, label, group_name) VALUES
-- 系统管理权限
('perm_manage_users', 'manage_users', '用户管理', '系统管理'),
('perm_manage_roles', 'manage_roles', '角色管理', '系统管理'),
('perm_manage_org', 'manage_org', '组织管理', '系统管理'),
('perm_manage_settings', 'manage_settings', '系统设置', '系统管理'),
('perm_manage_fonds', 'manage_fonds', '全宗管理', '系统管理'),

-- 导航权限
('perm_nav_all', 'nav:all', '所有导航', '导航权限'),
('perm_nav_portal', 'nav:portal', '门户首页', '导航权限'),
('perm_nav_panorama', 'nav:panorama', '全景视图', '导航权限'),
('perm_nav_pre_archive', 'nav:pre_archive', '预归档库', '导航权限'),
('perm_nav_collection', 'nav:collection', '资料收集', '导航权限'),
('perm_nav_archive_mgmt', 'nav:archive_mgmt', '档案管理', '导航权限'),
('perm_nav_query', 'nav:query', '档案查询', '导航权限'),
('perm_nav_borrowing', 'nav:borrowing', '档案借阅', '导航权限'),
('perm_nav_destruction', 'nav:destruction', '档案销毁', '导航权限'),
('perm_nav_warehouse', 'nav:warehouse', '库房管理', '导航权限'),
('perm_nav_stats', 'nav:stats', '数据统计', '导航权限'),
('perm_nav_settings', 'nav:settings', '系统设置', '导航权限'),

-- 档案操作权限
('perm_archive_create', 'archive:create', '创建档案', '档案操作'),
('perm_archive_view', 'archive:view', '查看档案', '档案操作'),
('perm_archive_edit', 'archive:edit', '编辑档案', '档案操作'),
('perm_archive_delete', 'archive:delete', '删除档案', '档案操作'),
('perm_archive_download', 'archive:download', '下载档案', '档案操作'),
('perm_archive_print', 'archive:print', '打印档案', '档案操作'),
('perm_archive_approve', 'archive:approve', '审批归档', '档案操作'),

-- 借阅权限
('perm_borrow_apply', 'borrow:apply', '申请借阅', '借阅管理'),
('perm_borrow_approve', 'borrow:approve', '审批借阅', '借阅管理'),

-- 销毁权限
('perm_destruction_apply', 'destruction:apply', '销毁鉴定', '销毁管理'),
('perm_destruction_approve', 'destruction:approve', '审批销毁', '销毁管理'),

-- 审计权限
('perm_audit_view', 'audit:view', '查看审计日志', '安全审计'),
('perm_audit_export', 'audit:export', '导出审计日志', '安全审计')

ON CONFLICT (perm_key) DO NOTHING;
