-- Input: Flyway 迁移引擎
-- Output: 清理 title 为哈希值的异常档案数据
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 清理 QZ- 开头的异常数据 (如 QZ-2025-30Y-AC04-551BB2)
DELETE FROM acc_archive 
WHERE archive_code LIKE 'QZ-%';

-- 清理 BR01- 开头的异常数据 (如 BR01-2025-30Y-AC01-000011)
-- 注意：正常的 BR 全宗是 BR-GROUP, BR-SALES 等，BR01 是测试生成的错误数据
DELETE FROM acc_archive 
WHERE archive_code LIKE 'BR01-%';

-- 清理 Title 看起来像哈希值的数据 (兜底方案)
-- 简单判断：长度 >= 32 且只包含 hex 字符 (0-9, a-f)
-- 且 2025 年的数据
DELETE FROM acc_archive 
WHERE fiscal_year = '2025'
  AND length(title) >= 32
  AND title ~ '^[0-9a-fA-F]+$';
