-- Input: sys_role 表中的 system_admin 角色权限
-- Output: 恢复 system_admin 的完整导航可见性
-- Pos: 数据库迁移 V20260308
--
-- 问题背景：
-- V20260307 重新分配用户角色后，admin 账号仅持有 system_admin 角色。
-- 该角色默认只包含 nav:portal / nav:settings，导致登录后侧边菜单被裁剪为两个入口。
--
-- 修复策略：
-- 为 system_admin 追加 nav:all，使现有前端菜单过滤逻辑恢复完整导航可见性。
-- 该变更仅恢复导航入口，不影响已有业务权限键。

UPDATE sys_role
SET permissions = permissions || '["nav:all"]'::jsonb,
    last_modified_time = CURRENT_TIMESTAMP
WHERE code = 'system_admin'
  AND deleted = 0
  AND permissions::text NOT LIKE '%"nav:all"%';
