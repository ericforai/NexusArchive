-- Input: 多组织数据隔离修复需求
-- Output: 修复用户全宗权限，确保现有数据可见
-- Pos: db/migration/V84
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 修复用户全宗权限，确保所有用户都能访问现有数据的全宗号
-- 问题：多组织改造后，数据隔离基于 fonds_no，如果用户没有对应的全宗权限，就看不到数据

-- 1. 为所有现有用户添加现有数据全宗号的权限
-- 策略：
--   a) 收集所有现有数据的唯一 fonds_no
--   b) 为每个现有用户添加这些全宗号的权限
--   c) 如果用户有 org_code，也确保添加该全宗号的权限

INSERT INTO sys_user_fonds_scope (id, user_id, fonds_no, scope_type, created_time, last_modified_time, deleted)
SELECT 
    CONCAT(u.id, '-', fonds.fonds_no) as id,
    u.id as user_id,
    fonds.fonds_no,
    'MIGRATION' as scope_type,  -- 标记为迁移来源
    CURRENT_TIMESTAMP as created_time,
    CURRENT_TIMESTAMP as last_modified_time,
    0 as deleted
FROM (
    -- 获取所有现有数据的唯一全宗号
    SELECT DISTINCT fonds_no 
    FROM acc_archive 
    WHERE fonds_no IS NOT NULL 
      AND fonds_no <> '' 
      AND deleted = 0
) fonds
CROSS JOIN sys_user u
WHERE u.deleted = 0
  -- 避免重复插入（使用 NOT EXISTS 检查）
  AND NOT EXISTS (
      SELECT 1 
      FROM sys_user_fonds_scope s 
      WHERE s.user_id = u.id 
        AND s.fonds_no = fonds.fonds_no
        AND s.deleted = 0
  );

-- 2. 为有 org_code 的用户添加其 org_code 对应的全宗权限（如果还没有的话）
-- 这确保用户至少可以访问与其组织代码匹配的全宗数据
INSERT INTO sys_user_fonds_scope (id, user_id, fonds_no, scope_type, created_time, last_modified_time, deleted)
SELECT 
    CONCAT(u.id, '-', u.org_code) as id,
    u.id as user_id,
    u.org_code as fonds_no,
    'DIRECT' as scope_type,
    CURRENT_TIMESTAMP as created_time,
    CURRENT_TIMESTAMP as last_modified_time,
    0 as deleted
FROM sys_user u
WHERE u.org_code IS NOT NULL
  AND u.org_code <> ''
  AND u.deleted = 0
  -- 避免重复插入
  AND NOT EXISTS (
      SELECT 1 
      FROM sys_user_fonds_scope s 
      WHERE s.user_id = u.id 
        AND s.fonds_no = u.org_code
        AND s.deleted = 0
  );

-- 3. 统计信息（注释，用于验证）
-- 执行后可以通过以下查询验证：
-- SELECT COUNT(DISTINCT user_id) as user_count, COUNT(*) as total_permissions 
-- FROM sys_user_fonds_scope WHERE deleted = 0;
--
-- SELECT fonds_no, COUNT(DISTINCT user_id) as user_count 
-- FROM sys_user_fonds_scope 
-- WHERE deleted = 0 
-- GROUP BY fonds_no 
-- ORDER BY fonds_no;

