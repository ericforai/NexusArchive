-- Add business_doc_no column to arc_file_content table
-- 对应实体: ArcFileContent.businessDocNo

ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS business_doc_no VARCHAR(100);
COMMENT ON COLUMN arc_file_content.business_doc_no IS '业务单据号 (来自 ERP 的凭证号)';
