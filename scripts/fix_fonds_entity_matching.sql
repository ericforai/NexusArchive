-- ================================================================
-- 全宗与法人自动匹配修复脚本
-- Purpose: 根据全宗名称或立档单位名称自动匹配到对应的法人实体
-- Date: 2026-01-14
-- ================================================================

-- 1. 查看当前全宗的 entity_id 设置情况
SELECT 
    '当前全宗 entity_id 情况' as info,
    COUNT(*) as total_fonds,
    COUNT(entity_id) as has_entity_id,
    COUNT(*) - COUNT(entity_id) as missing_entity_id
FROM bas_fonds;

-- 2. 根据全宗名称匹配法人（精确匹配）
UPDATE bas_fonds f
SET entity_id = e.id
FROM sys_entity e
WHERE f.entity_id IS NULL
  AND (
    -- 全宗名称匹配法人名称
    f.fonds_name = e.name
    OR
    -- 立档单位名称匹配法人名称
    (f.company_name IS NOT NULL AND f.company_name = e.name)
  )
  -- 确保匹配的是法人而不是部门（有税号或名称不以"部"结尾）
  AND (
    (e.tax_id IS NOT NULL AND e.tax_id != '')
    OR (e.name NOT LIKE '%部' AND e.name NOT LIKE '%部门%')
  );

-- 3. 根据全宗名称包含法人名称匹配（模糊匹配，用于处理"XX公司档案全宗"这种情况）
UPDATE bas_fonds f
SET entity_id = e.id
FROM sys_entity e
WHERE f.entity_id IS NULL
  AND (
    -- 全宗名称包含法人名称（如"泊冉集团有限公司档案全宗"包含"泊冉集团有限公司"）
    f.fonds_name LIKE '%' || e.name || '%'
    OR
    -- 立档单位名称包含法人名称
    (f.company_name IS NOT NULL AND f.company_name LIKE '%' || e.name || '%')
  )
  -- 确保匹配的是法人而不是部门
  AND (
    (e.tax_id IS NOT NULL AND e.tax_id != '')
    OR (e.name NOT LIKE '%部' AND e.name NOT LIKE '%部门%')
  )
  -- 避免匹配到过短的名称（如"公司"）
  AND LENGTH(e.name) >= 3;

-- 4. 根据全宗号匹配（如果全宗号包含法人标识）
-- 例如：BR-GROUP 可能对应 "泊冉集团有限公司"
-- 这个需要根据实际业务规则调整

-- 5. 查看匹配结果
SELECT 
    '匹配结果' as info,
    f.fonds_code,
    f.fonds_name,
    f.company_name,
    f.entity_id,
    e.name as matched_entity_name,
    e.tax_id as entity_tax_id
FROM bas_fonds f
LEFT JOIN sys_entity e ON f.entity_id = e.id
ORDER BY f.fonds_code;

-- 6. 统计匹配情况
SELECT 
    '匹配统计' as info,
    COUNT(*) as total_fonds,
    COUNT(f.entity_id) as matched_count,
    COUNT(*) - COUNT(f.entity_id) as unmatched_count
FROM bas_fonds f;
