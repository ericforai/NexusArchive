-- Input: Flyway 迁移引擎
-- Output: 修复超级管理员权限
-- Pos: 数据库迁移脚本

-- Fix 403 Forbidden: Grant archive:read explicit permission to super_admin
-- The code uses @PreAuthorize("hasAuthority('archive:read')"), but super_admin only had "system_admin"
UPDATE sys_role 
SET permissions = (
    SELECT jsonb_set(
        permissions::jsonb, 
        '{100}', -- append to end (simplified, assuming array)
        '"archive:read"', 
        true
    )
)
WHERE code = 'super_admin' AND NOT permissions::text LIKE '%"archive:read"%';

-- Better approach: Overwrite with a full list ensuring all necessary perms are present including legacy ones
UPDATE sys_role
SET permissions = '["nav:portal","nav:panorama","nav:pre_archive","nav:collection","nav:archive_mgmt","nav:query","nav:borrowing","nav:destruction","nav:warehouse","nav:stats","nav:settings","nav:all","system_admin","manage_users","archive:read","archive:view","archive:manage","audit:view"]'
WHERE code = 'super_admin';
