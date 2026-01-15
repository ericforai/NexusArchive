-- ================================================================
-- 全宗与法人关联关系检查脚本
-- Purpose: 检查全宗的 entity_id 是否正确关联到法人实体
-- Date: 2026-01-14
-- ================================================================

-- 1. 检查全宗 entity_id 为空的情况
SELECT 
    '全宗 entity_id 为空' as issue_type,
    COUNT(*) as count,
    STRING_AGG(fonds_code, ', ') as fonds_codes
FROM bas_fonds 
WHERE (entity_id IS NULL OR entity_id = '')
  AND deleted = 0;

-- 2. 检查全宗 entity_id 指向部门的情况（名称以"部"结尾且无税号）
SELECT 
    '全宗关联到部门' as issue_type,
    f.id as fonds_id,
    f.fonds_code,
    f.fonds_name,
    f.entity_id,
    e.name as entity_name,
    e.tax_id as entity_tax_id
FROM bas_fonds f
LEFT JOIN sys_entity e ON f.entity_id = e.id
WHERE f.entity_id IS NOT NULL
  AND f.deleted = 0
  AND e.deleted = 0
  AND (
    (e.name LIKE '%部' AND (e.tax_id IS NULL OR e.tax_id = ''))
    OR e.name LIKE '%部门%'
  );

-- 3. 检查全宗 entity_id 指向不存在的法人
SELECT 
    '全宗关联到不存在的法人' as issue_type,
    f.id as fonds_id,
    f.fonds_code,
    f.fonds_name,
    f.entity_id
FROM bas_fonds f
LEFT JOIN sys_entity e ON f.entity_id = e.id
WHERE f.entity_id IS NOT NULL
  AND f.deleted = 0
  AND e.id IS NULL;

-- 4. 统计每个法人的全宗数量
SELECT 
    e.id as entity_id,
    e.name as entity_name,
    e.tax_id,
    COUNT(f.id) as fonds_count,
    STRING_AGG(f.fonds_code, ', ') as fonds_codes
FROM sys_entity e
LEFT JOIN bas_fonds f ON e.id = f.entity_id AND f.deleted = 0
WHERE e.deleted = 0
  AND (e.tax_id IS NOT NULL AND e.tax_id != '' OR e.name NOT LIKE '%部' AND e.name NOT LIKE '%部门%')
GROUP BY e.id, e.name, e.tax_id
ORDER BY fonds_count DESC, e.name;

-- 5. 检查是否有全宗关联到多个法人（数据异常）
SELECT 
    '全宗关联异常' as issue_type,
    f.fonds_code,
    f.fonds_name,
    COUNT(DISTINCT f.entity_id) as entity_count
FROM bas_fonds f
WHERE f.entity_id IS NOT NULL
  AND f.deleted = 0
GROUP BY f.fonds_code, f.fonds_name
HAVING COUNT(DISTINCT f.entity_id) > 1;
