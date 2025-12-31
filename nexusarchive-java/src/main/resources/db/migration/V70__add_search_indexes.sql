-- Input: Archive Search Indexes
-- Output: Schema change for search enhancement
-- Pos: db/migration/V70
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 1. 结构化字段扩展 (Extraction from JSONB)
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS counterparty VARCHAR(255);
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS voucher_no VARCHAR(100);
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS invoice_no VARCHAR(100);

COMMENT ON COLUMN acc_archive.counterparty IS '对方单位 (结构化检索)';
COMMENT ON COLUMN acc_archive.voucher_no IS '凭证号 (结构化检索)';
COMMENT ON COLUMN acc_archive.invoice_no IS '发票号 (结构化检索)';

-- 2. 金额索引 (BTree)
CREATE INDEX IF NOT EXISTS idx_archive_amount 
    ON acc_archive(fonds_no, fiscal_year, amount);

-- 3. 凭证日期索引 (BTree)
CREATE INDEX IF NOT EXISTS idx_archive_doc_date 
    ON acc_archive(fonds_no, fiscal_year, doc_date);

-- 4. 对方单位索引 (BTree)
CREATE INDEX IF NOT EXISTS idx_archive_counterparty 
    ON acc_archive(fonds_no, fiscal_year, counterparty);

-- 5. 凭证号索引 (BTree)
CREATE INDEX IF NOT EXISTS idx_archive_voucher_no 
    ON acc_archive(fonds_no, fiscal_year, voucher_no);

-- 6. 发票号索引 (BTree)
CREATE INDEX IF NOT EXISTS idx_archive_invoice_no 
    ON acc_archive(fonds_no, fiscal_year, invoice_no);

