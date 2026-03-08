-- Input: sys_role 表中的管理员角色 permissions 文本
-- Output: 修复被历史迁移拼坏的管理员权限 JSON
-- Pos: 数据库迁移 V20260309
--
-- 问题背景：
-- 早期若干迁移对 text 类型的 permissions 字段使用了 jsonb 拼接语法，
-- 最终把权限内容写成了多段数组字符串（例如: [][][]），导致 Jackson 无法解析。
--
-- 修复策略：
-- 直接将受影响管理员角色的 permissions 重写为合法、去重后的 JSON 数组字符串。

UPDATE sys_role
SET permissions = '["nav:portal","nav:settings","manage_settings","system:backup","system:restore","system:monitor","archive:read","archive:view","audit:view","entity:view","entity:manage","fonds:view","nav:all"]',
    last_modified_time = CURRENT_TIMESTAMP
WHERE code = 'system_admin'
  AND deleted = 0;

UPDATE sys_role
SET permissions = '["nav:portal","nav:panorama","nav:pre_archive","nav:collection","nav:archive_mgmt","nav:query","nav:borrowing","nav:destruction","nav:warehouse","nav:stats","nav:settings","nav:all","system_admin","manage_users","archive:read","archive:view","archive:manage","audit:view","scan:manage","entity:view","entity:manage","fonds:view","nav:audit","audit:verify","audit:evidence"]',
    last_modified_time = CURRENT_TIMESTAMP
WHERE code = 'super_admin'
  AND deleted = 0;
