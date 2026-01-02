-- 手动修复用户全宗权限的SQL脚本
-- 用法：直接在数据库中执行此脚本
-- 适用于：迁移脚本还未执行，或者需要立即修复的情况

-- 1. 检查当前状态
SELECT '=== 现有数据的全宗号分布 ===' as info;
SELECT fonds_no, COUNT(*) as archive_count 
FROM acc_archive 
WHERE deleted = 0 
  AND fonds_no IS NOT NULL
  AND fonds_no <> ''
GROUP BY fonds_no 
ORDER BY fonds_no;

SELECT '=== 用户数量 ===' as info;
SELECT COUNT(*) as user_count 
FROM sys_user 
WHERE deleted = 0;

SELECT '=== 当前用户全宗权限数量 ===' as info;
SELECT COUNT(*) as permission_count 
FROM sys_user_fonds_scope 
WHERE deleted = 0;

-- 2. 执行修复：为所有用户添加现有数据全宗号的权限
INSERT INTO sys_user_fonds_scope (id, user_id, fonds_no, scope_type, created_time, last_modified_time, deleted)
SELECT 
    CONCAT(u.id, '-', fonds.fonds_no) as id,
    u.id as user_id,
    fonds.fonds_no,
    'MIGRATION' as scope_type,
    CURRENT_TIMESTAMP as created_time,
    CURRENT_TIMESTAMP as last_modified_time,
    0 as deleted
FROM (
    SELECT DISTINCT fonds_no 
    FROM acc_archive 
    WHERE fonds_no IS NOT NULL 
      AND fonds_no <> '' 
      AND deleted = 0
) fonds
CROSS JOIN sys_user u
WHERE u.deleted = 0
  AND NOT EXISTS (
      SELECT 1 
      FROM sys_user_fonds_scope s 
      WHERE s.user_id = u.id 
        AND s.fonds_no = fonds.fonds_no
        AND s.deleted = 0
  );

-- 3. 为有 org_code 的用户添加其 org_code 对应的全宗权限
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
  AND NOT EXISTS (
      SELECT 1 
      FROM sys_user_fonds_scope s 
      WHERE s.user_id = u.id 
        AND s.fonds_no = u.org_code
        AND s.deleted = 0
  );

-- 4. 验证修复结果
SELECT '=== 修复后的权限统计 ===' as info;
SELECT COUNT(DISTINCT user_id) as user_count, COUNT(*) as total_permissions 
FROM sys_user_fonds_scope 
WHERE deleted = 0;

SELECT '=== 每个全宗号的用户权限分布 ===' as info;
SELECT fonds_no, COUNT(DISTINCT user_id) as user_count 
FROM sys_user_fonds_scope 
WHERE deleted = 0 
GROUP BY fonds_no 
ORDER BY fonds_no;

SELECT '=== 验证：所有现有数据的全宗号都有对应的用户权限 ===' as info;
SELECT DISTINCT a.fonds_no
FROM acc_archive a
WHERE a.deleted = 0
  AND a.fonds_no IS NOT NULL
  AND a.fonds_no <> ''
  AND NOT EXISTS (
      SELECT 1 
      FROM sys_user_fonds_scope s 
      WHERE s.fonds_no = a.fonds_no 
        AND s.deleted = 0
  );

-- 如果上面的查询返回空结果，说明修复成功
-- 如果还有全宗号返回，说明这些全宗号没有对应的用户权限

