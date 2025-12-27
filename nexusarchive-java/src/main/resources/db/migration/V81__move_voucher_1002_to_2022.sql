-- Input: Flyway 迁移引擎
-- Output: 修正演示数据 - 将 Voucher 1002 彻底归档到 2022 年
-- Pos: 数据库迁移脚本

-- 由于 Voucher 1002 (差旅费) 业务日期为 2022-09-20，
-- 为了避免在 2025 年目录下造成的混淆，将其彻底移动到 2022 年 9 月。

-- 更新主表
UPDATE acc_archive 
SET 
    fiscal_year = '2022',
    fiscal_period = '09',
    archive_code = 'BR-GROUP-2022-30Y-FIN-AC01-1002' -- 修改档号为 2022
WHERE id = 'voucher-2024-11-002';

-- 更新附件表的关联档号
UPDATE arc_file_content 
SET archival_code = 'BR-GROUP-2022-30Y-FIN-AC01-1002'
WHERE item_id = 'voucher-2024-11-002';
