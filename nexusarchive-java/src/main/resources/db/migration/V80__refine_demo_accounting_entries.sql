-- Input: Flyway 迁移引擎
-- Output: 细化会计分录 - 严格遵循会计准则（价税分离 & 票单一致）
-- Pos: 数据库迁移脚本（通过 V80 执行）

-- =====================================================
-- 凭证 #1001 (吴奕聪餐饮店)
-- 原始金额：657.00
-- 修正逻辑：
--   根据发票显示，税率为 1% (小规模纳税人/餐饮服务)，税额 6.50
--   不含税金额 = 650.50
--   进项税额 = 6.50
-- =====================================================
UPDATE acc_archive 
SET custom_metadata = '[{"id":"1","description":"支付业务招待费-客户接待餐费","accsubject":{"code":"6602","name":"管理费用-业务招待费"},"debit_org":650.50,"credit_org":0},{"id":"2","description":"进项税额","accsubject":{"code":"2221","name":"应交税费-应交增值税(进项税额)"},"debit_org":6.50,"credit_org":0},{"id":"3","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"credit_org":657.00,"debit_org":0}]'
WHERE id = 'voucher-2024-11-001';

-- =====================================================
-- 凭证 #1002 (员工差旅费报销)
-- 原始金额：10,000.00
-- 修正逻辑：
--   金额调整为 10,000.00
--   分录调整为 管理费用-交通费 10,000.00
--   移除旧的"差旅费发票"(file-invoice-001)
--   新增"银行回单"和"报销单"
-- =====================================================

-- 1. 更新凭证主表信息
UPDATE acc_archive 
SET 
    amount = 10000.00,
    doc_date = '2022-09-20',
    title = '支付强生交通费用',
    custom_metadata = '[{"id":"1","description":"支付交通费","accsubject":{"code":"6602","name":"管理费用-交通费"},"debit_org":10000.00,"credit_org":0},{"id":"2","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"credit_org":10000.00,"debit_org":0}]'
WHERE id = 'voucher-2024-11-002';

-- 2. 移除旧附件 (差旅费发票)
DELETE FROM arc_file_content WHERE id = 'file-invoice-001';

-- 3. 插入新附件：银行回单
INSERT INTO arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id)
SELECT 'file-bank-receipt-1002', 'BR-GROUP-2025-30Y-FIN-AC01-1002', '银行回单_10000元.pdf', 'PDF', 102400, 'hash_placeholder_1', 'SHA-256', 'uploads/demo/bank_receipt_1002.pdf', NOW(), 'voucher-2024-11-002'
WHERE NOT EXISTS (SELECT 1 FROM arc_file_content WHERE id = 'file-bank-receipt-1002');

-- 4. 插入新附件：报销单
INSERT INTO arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id)
SELECT 'file-reimbursement-1002', 'BR-GROUP-2025-30Y-FIN-AC01-1002', '员工报销单.pdf', 'PDF', 51200, 'hash_placeholder_2', 'SHA-256', 'uploads/demo/reimbursement_1002.pdf', NOW(), 'voucher-2024-11-002'
WHERE NOT EXISTS (SELECT 1 FROM arc_file_content WHERE id = 'file-reimbursement-1002');
