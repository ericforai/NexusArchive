-- Input: sys_role 表结构
-- Output: role_business_user 角色记录
-- Pos: 数据库迁移 V2026010903
-- 创建业务操作员角色，用于新用户默认角色分配

-- 插入业务操作员角色
-- 权限说明：
-- - nav:portal, nav:panorama: 基础查看权限
-- - nav:pre_archive, nav:collection, nav:repository, nav:operations: 归档操作权限
-- - nav:utilization: 档案借阅权限
-- - nav:stats: 数据统计权限
-- - 不包含 nav:settings (系统设置) 等管理权限
-- 使用 ON CONFLICT 避免重复插入
INSERT INTO sys_role (
    id,
    name,
    code,
    role_category,
    is_exclusive,
    description,
    permissions,
    data_scope,
    type,
    created_at,
    last_modified_time,
    deleted
) VALUES (
    'role_business_user',
    '业务操作员',
    'business_user',
    'business_user',
    false,
    '默认业务用户角色，具有基本业务操作权限，无管理权限',
    '["nav:portal","nav:panorama","nav:pre_archive","nav:collection","nav:repository","nav:operations","nav:utilization","nav:stats","archive:read","archive:view","borrowing:create","borrowing:view"]',
    'self',
    'custom',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
) ON CONFLICT (id) DO NOTHING;
