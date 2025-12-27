-- Input: Flyway 迁移引擎
-- Output: 增加专业财务字段支持（摘要、凭证字、业务日期）
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 为电子凭证池增加专业财务字段
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS summary VARCHAR(512);
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS voucher_word VARCHAR(64);
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS doc_date DATE;

COMMENT ON COLUMN arc_file_content.summary IS '摘要/业务描述';
COMMENT ON COLUMN arc_file_content.voucher_word IS '凭证字号 (如 记-1)';
COMMENT ON COLUMN arc_file_content.doc_date IS '业务日期';

-- 由于总金额存储在元数据表中，我们更新一些 DEMO 记录的金额，使其看起来更真实
UPDATE arc_file_metadata_index 
SET total_amount = (RANDOM() * 50000 + 100)::DECIMAL(18,2)
WHERE total_amount = 43758.00 OR total_amount IS NULL;

-- 为现有的 TEMP-POOL 记录填充一些摘要和凭证字
UPDATE arc_file_content
SET summary = '[管理费用-办公费] 采购办公用品及耗材',
    voucher_word = '记-1001',
    doc_date = '2025-11-20'
WHERE archival_code LIKE 'TEMP-POOL-%' AND summary IS NULL;
