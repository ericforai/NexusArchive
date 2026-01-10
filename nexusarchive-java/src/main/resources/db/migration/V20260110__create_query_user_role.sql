-- Input: sys_role 表结构
-- Output: query_user 角色记录
-- Pos: 数据库迁移 V20260110
-- 创建查询用户角色 (query_user)，仅提供档案借阅只读权限

-- 插入查询用户角色
-- 权限说明：
-- - nav:utilization: 借阅管理入口访问
-- - borrowing:create: 创建借阅申请
-- - borrowing:view: 查看自己的借阅记录
-- - archive:read: 浏览档案目录（只读）
-- - 不包含创建/编辑/审批/导出/打印权限
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
    'role_query_user',
    '查询用户',
    'query_user',
    'query_user',
    false,
    '查询用户角色，仅能通过借阅审批访问特定档案，具有最小权限',
    '["nav:utilization","borrowing:create","borrowing:view","archive:read"]',
    'self',
    'custom',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
) ON CONFLICT (id) DO NOTHING;
