-- Input: Flyway Migration
-- Output: Populate Demo Candidates for Matching
-- Pos: db/migration/V92

-- 1. 配置单据类型映射 (假设 company_id = 1, 对应主演示租户)
-- 确保 YS 凭证需要的类型被映射到标准 Evidence Role
INSERT INTO cfg_doc_type_mapping (company_id, customer_doc_type, evidence_role, display_name) VALUES
(1, 'VAT_INVOICE', 'INVOICE', '增值税专用发票'),
(1, 'BANK_SLIP', 'BANK_RECEIPT', '银行电子回单'),
(1, 'SALES_ORDER', 'CONTRACT', '销售订单')
ON CONFLICT (company_id, customer_doc_type) DO NOTHING;

-- 2. 插入原始凭证 (Candidates)
-- 针对 YS-2025-08-记-4 (a059d...)
-- Amount: 50.00, Date: 2025-08-09, Scene: SALES_OUT -> Need INVOICE + RECEIPT
-- 插入一张匹配的发票
INSERT INTO arc_original_voucher (
    id, voucher_no, voucher_category, voucher_type, business_date, 
    amount, currency, counterparty, summary, 
    fonds_code, fiscal_year, retention_period, archive_status, 
    created_time, last_modified_time
) VALUES (
    'ov-demo-inv-004', 'OV-2025-INV-004', 'INVOICE', 'VAT_INVOICE', '2025-08-09',
    50.00, 'CNY', 'Test Customer', '销售服务费发票',
    'BR01', '2025', '30Y', 'ARCHIVED',
    NOW(), NOW()
) ON CONFLICT (id) DO NOTHING;

-- 插入一张匹配的银行回单
INSERT INTO arc_original_voucher (
    id, voucher_no, voucher_category, voucher_type, business_date, 
    amount, currency, counterparty, summary, 
    fonds_code, fiscal_year, retention_period, archive_status, 
    created_time, last_modified_time
) VALUES (
    'ov-demo-bank-004', 'OV-2025-BANK-004', 'BANK', 'BANK_SLIP', '2025-08-09',
    50.00, 'CNY', 'Test Customer', '服务费收款回单',
    'BR01', '2025', '30Y', 'ARCHIVED',
    NOW(), NOW()
) ON CONFLICT (id) DO NOTHING;


-- 针对 YS-2025-08-记-3 (baf87...)
-- Amount: 30.00, Date: 2025-08-09, Scene: SALES_OUT (Cost?) or mismatch?
-- Roles: Debit=[], Credit=[INVENTORY] -> Inventory Out?
-- Scene likely: UNKNOWN or customized.
-- Let's provide an invoice anyway just in case it matches 30.00
INSERT INTO arc_original_voucher (
    id, voucher_no, voucher_category, voucher_type, business_date, 
    amount, currency, counterparty, summary, 
    fonds_code, fiscal_year, retention_period, archive_status, 
    created_time, last_modified_time
) VALUES (
    'ov-demo-inv-003', 'OV-2025-INV-003', 'INVOICE', 'VAT_INVOICE', '2025-08-09',
    30.00, 'CNY', 'Test Customer', '商品销售发票',
    'BR01', '2025', '30Y', 'ARCHIVED',
    NOW(), NOW()
) ON CONFLICT (id) DO NOTHING;


-- 3. 关联文件 (Mock Files)
-- 必须要有文件，否则详情页可能显示"无文件"
INSERT INTO arc_original_voucher_file (
    id, voucher_id, file_name, file_type, file_size, storage_path, file_hash
) VALUES (
    'file-ov-inv-004', 'ov-demo-inv-004', 'invoice_50.pdf', 'PDF', 102400, 'uploads/demo/invoice_50.pdf', 'hash_inv_004'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO arc_original_voucher_file (
    id, voucher_id, file_name, file_type, file_size, storage_path, file_hash
) VALUES (
    'file-ov-bank-004', 'ov-demo-bank-004', 'bank_receipt_50.pdf', 'PDF', 102400, 'uploads/demo/bank_50.pdf', 'hash_bank_004'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO arc_original_voucher_file (
    id, voucher_id, file_name, file_type, file_size, storage_path, file_hash
) VALUES (
    'file-ov-inv-003', 'ov-demo-inv-003', 'invoice_30.pdf', 'PDF', 102400, 'uploads/demo/invoice_30.pdf', 'hash_inv_003'
) ON CONFLICT (id) DO NOTHING;
