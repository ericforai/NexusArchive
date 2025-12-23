-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- V37: 新增 ERP 原始凭证号字段
-- 目的: 存储用户可读的 ERP 凭证号（如 "记-3"），与幂等性ID (business_doc_no) 区分

-- 1. 新增 erp_voucher_no 字段到 arc_file_content
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS erp_voucher_no VARCHAR(100);
COMMENT ON COLUMN arc_file_content.erp_voucher_no IS 'ERP原始凭证号（用户可读，如 记-3）';

-- 2. 更新 business_doc_no 注释，明确其用途
COMMENT ON COLUMN arc_file_content.business_doc_no IS '来源唯一标识（幂等性控制，如 YonSuite_xxx）';

-- 3. 回填历史数据: 从 archival_code 中提取凭证号（可选）
-- 对于 YS- 开头的临时档号，提取最后部分作为 erp_voucher_no
UPDATE arc_file_content 
SET erp_voucher_no = SUBSTRING(archival_code FROM 'YS-[0-9-]+(.+)$')
WHERE archival_code LIKE 'YS-%' 
  AND erp_voucher_no IS NULL;
