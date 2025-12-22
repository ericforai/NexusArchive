-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- Add business_doc_no column to arc_file_content table
-- 对应实体: ArcFileContent.businessDocNo

ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS business_doc_no VARCHAR(100);
COMMENT ON COLUMN arc_file_content.business_doc_no IS '业务单据号 (来自 ERP 的凭证号)';
