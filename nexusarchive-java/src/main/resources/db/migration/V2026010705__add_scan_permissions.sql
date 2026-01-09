-- ================================================================
-- Migration: V2026010705__add_scan_permissions.sql
-- Purpose: 添加扫描工作区相关权限
-- Author: Claude Code
-- Date: 2026-01-07
-- ================================================================

-- 添加扫描工作区相关权限
-- 这些权限对应 ScanWorkspaceController 中的 @PreAuthorize 注解

INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_time)
VALUES
    ('perm_scan_view', 'scan:view', '查看扫描工作区', '扫描管理', NOW(), NOW()),
    ('perm_scan_upload', 'scan:upload', '上传扫描文件', '扫描管理', NOW(), NOW()),
    ('perm_scan_ocr', 'scan:ocr', '执行OCR识别', '扫描管理', NOW(), NOW()),
    ('perm_scan_edit', 'scan:edit', '编辑扫描结果', '扫描管理', NOW(), NOW()),
    ('perm_scan_submit', 'scan:submit', '提交到预归档', '扫描管理', NOW(), NOW()),
    ('perm_scan_delete', 'scan:delete', '删除扫描文件', '扫描管理', NOW(), NOW()),
    ('perm_scan_manage', 'scan:manage', '管理扫描工作区', '扫描管理', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 为超级管理员角色添加扫描权限 (nav:all 已有,但需要明确 scan:manage)
UPDATE public.sys_role
SET permissions = CASE
    WHEN permissions IS NULL OR permissions = '' THEN '["scan:manage"]'
    WHEN permissions NOT LIKE '%scan:manage%' THEN
        substr(permissions, 1, length(permissions) - 1) || ',"scan:manage"]'
    ELSE permissions
END
WHERE code = 'super_admin';

-- 添加预归档导航权限到超级管理员 (如果不存在)
UPDATE public.sys_role
SET permissions = CASE
    WHEN permissions NOT LIKE '%nav:pre_archive%' THEN
        substr(permissions, 1, length(permissions) - 1) || ',"nav:pre_archive"]'
    ELSE permissions
END
WHERE code = 'super_admin'
AND permissions IS NOT NULL;
