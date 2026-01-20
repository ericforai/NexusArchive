-- 修复 2024 年月报的 reportType
UPDATE acc_archive
SET custom_metadata = COALESCE(custom_metadata, '{}'::jsonb) || '{"reportType": "MONTHLY"}'
WHERE category_code = 'AC03'
  AND fiscal_year = '2024'
  AND (custom_metadata->>'reportType' IS NULL OR custom_metadata->>'reportType' = '')
  AND (title LIKE '%月报%' OR title LIKE '%资产负债表%' OR title LIKE '%利润表%' OR title LIKE '%现金流量表%');

-- 修复 2023 年报告的 reportType
UPDATE acc_archive
SET custom_metadata = COALESCE(custom_metadata, '{}'::jsonb) || '{"reportType": "MONTHLY"}'
WHERE category_code = 'AC03'
  AND fiscal_year = '2023'
  AND (custom_metadata->>'reportType' IS NULL OR custom_metadata->>'reportType' = '')
  AND title LIKE '%科目余额表%';

-- 修复 2022 年报告的 reportType
UPDATE acc_archive
SET custom_metadata = COALESCE(custom_metadata, '{}'::jsonb) || '{"reportType": "ANNUAL"}'
WHERE category_code = 'AC03'
  AND fiscal_year = '2022'
  AND (custom_metadata->>'reportType' IS NULL OR custom_metadata->>'reportType' = '')
  AND title LIKE '%决算报告%';

-- 查看修复结果
SELECT
    fiscal_year,
    title,
    custom_metadata->>'reportType' as report_type
FROM acc_archive
WHERE category_code = 'AC03' AND fiscal_year != '2025'
ORDER BY fiscal_year DESC, title;
