-- Input: Flyway Migration
-- Output: Populate Candidates for TEST and BRJT vouchers
-- Pos: db/migration/V93

-- =================================================================
-- 1. 补全 TEST Vouchers 的 custom_metadata (使其能被识别为付款/报销)
-- =================================================================

-- 1.1 TEST-测试档案_e68eb687 (100.00) -> 设定为 "办公费报销"
UPDATE acc_archive 
SET custom_metadata = '[{"id":"t1","description":"购买办公用品","accsubject":{"code":"6602","name":"管理费用-办公费"},"debit_org":100.00,"credit_org":0},{"id":"t2","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"credit_org":100.00,"debit_org":0}]'
WHERE archive_code = 'TEST-测试档案_e68eb687';

-- 1.2 TEST-测试档案_55e000ec (100.00) -> 设定为 "招待费报销"
UPDATE acc_archive 
SET custom_metadata = '[{"id":"t3","description":"业务招待","accsubject":{"code":"6602","name":"管理费用-业务招待费"},"debit_org":100.00,"credit_org":0},{"id":"t4","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"credit_org":100.00,"debit_org":0}]'
WHERE archive_code = 'TEST-测试档案_55e000ec';

-- 1.3 BRJT-2024-30Y-FIN-AC01-0010 (45,000.00) -> 设定为 "技术服务费收入"
-- 借：银行存款 45000 (Role: BANK)
-- 贷：主营业务收入 45000 (Role: REVENUE) -> Scene: SALES_OUT
UPDATE acc_archive 
SET custom_metadata = '[{"id":"t5","description":"技术服务费收到","accsubject":{"code":"1002","name":"银行存款"},"debit_org":45000.00,"credit_org":0},{"id":"t6","description":"确认收入","accsubject":{"code":"6001","name":"主营业务收入"},"credit_org":45000.00,"debit_org":0}]'
WHERE archive_code = 'BRJT-2024-30Y-FIN-AC01-0010';


-- =================================================================
-- 2. 插入原始凭证 Candidates
-- =================================================================

-- 2.1 为 TEST_e68eb687 插入发票 (100.00)
INSERT INTO arc_original_voucher (
    id, voucher_no, voucher_category, voucher_type, business_date, 
    amount, currency, counterparty, summary, 
    fonds_code, fiscal_year, retention_period, archive_status, 
    created_time, last_modified_time
) VALUES (
    'ov-test-100-1', 'OV-2025-TEST-01', 'INVOICE', 'VAT_INVOICE', '2025-12-22',
    100.00, 'CNY', '京东办公', '办公用品采购',
    'default', '2025', '30Y', 'ARCHIVED',
    NOW(), NOW()
) ON CONFLICT (id) DO NOTHING;

-- 2.2 为 TEST_55e000ec 插入发票 (100.00)
INSERT INTO arc_original_voucher (
    id, voucher_no, voucher_category, voucher_type, business_date, 
    amount, currency, counterparty, summary, 
    fonds_code, fiscal_year, retention_period, archive_status, 
    created_time, last_modified_time
) VALUES (
    'ov-test-100-2', 'OV-2025-TEST-02', 'INVOICE', 'VAT_INVOICE', '2025-12-22',
    100.00, 'CNY', '海底捞餐饮', '客户接待',
    'default', '2025', '30Y', 'ARCHIVED',
    NOW(), NOW()
) ON CONFLICT (id) DO NOTHING;

-- 2.3 为 BRJT-2024... 插入银行回单 (45,000.00)
-- Scene: SALES_OUT -> 需要匹配 银行回单/发票
INSERT INTO arc_original_voucher (
    id, voucher_no, voucher_category, voucher_type, business_date, 
    amount, currency, counterparty, summary, 
    fonds_code, fiscal_year, retention_period, archive_status, 
    created_time, last_modified_time
) VALUES (
    'ov-brjt-45k-1', 'OV-2024-BANK-45K', 'BANK', 'BANK_SLIP', '2024-03-15',
    45000.00, 'CNY', '某大客户', '服务费收款',
    'BRJT', '2024', '30Y', 'ARCHIVED',
    NOW(), NOW()
) ON CONFLICT (id) DO NOTHING;


-- =================================================================
-- 3. 关联文件
-- =================================================================

INSERT INTO arc_original_voucher_file (
    id, voucher_id, file_name, file_type, file_size, storage_path, file_hash
) VALUES (
    'f-test-100-1', 'ov-test-100-1', 'office_supplies_100.pdf', 'PDF', 20480, 'uploads/demo/office_100.pdf', 'hash_test_1'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO arc_original_voucher_file (
    id, voucher_id, file_name, file_type, file_size, storage_path, file_hash
) VALUES (
    'f-test-100-2', 'ov-test-100-2', 'food_receipt_100.pdf', 'PDF', 20480, 'uploads/demo/food_100.pdf', 'hash_test_2'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO arc_original_voucher_file (
    id, voucher_id, file_name, file_type, file_size, storage_path, file_hash
) VALUES (
    'f-brjt-45k-1', 'ov-brjt-45k-1', 'bank_receipt_45000.pdf', 'PDF', 51200, 'uploads/demo/bank_45k.pdf', 'hash_brjt_1'
) ON CONFLICT (id) DO NOTHING;
