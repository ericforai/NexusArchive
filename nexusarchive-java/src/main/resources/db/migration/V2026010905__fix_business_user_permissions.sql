-- Input: sys_role 表
-- Output: 更新 role_business_user 的权限
-- Pos: 数据库迁移 V2026010905
-- 修复业务操作员角色的权限配置

-- 更新 role_business_user 的权限，添加档案利用等业务权限
UPDATE sys_role
SET permissions = '["nav:portal","nav:panorama","nav:pre_archive","nav:collection","nav:repository","nav:archive_mgmt","nav:operations","nav:utilization","nav:stats","archive:read","archive:view","borrowing:create","borrowing:view"]',
    last_modified_time = CURRENT_TIMESTAMP
WHERE id = 'role_business_user';
