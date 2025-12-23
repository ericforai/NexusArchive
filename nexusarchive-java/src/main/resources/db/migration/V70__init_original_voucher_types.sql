-- V70: Initialize standard original voucher types
-- Reference: DA/T 94-2022 & Common Accounting Practices

-- Ensure we don't have duplicates before inserting
DELETE FROM sys_original_voucher_type WHERE type_code IN (
    'SALES_ORDER', 'DELIVERY_ORDER', 'PURCHASE_ORDER', 'RECEIPT_ORDER', 
    'PAYMENT_REQ', 'EXPENSE_REPORT', 'GEN_INVOICE', 'VAT_INVOICE', 
    'BANK_SLIP', 'BANK_STATEMENT', 'CONTRACT'
);

-- Insert common types
INSERT INTO sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES
-- 单据类 (DOCUMENT)
('OVT-DOC-001', 'DOCUMENT', '单据类', 'SALES_ORDER', '销售订单', '30Y', 10, TRUE, NOW(), NOW()),
('OVT-DOC-002', 'DOCUMENT', '单据类', 'DELIVERY_ORDER', '出库单', '30Y', 20, TRUE, NOW(), NOW()),
('OVT-DOC-003', 'DOCUMENT', '单据类', 'PURCHASE_ORDER', '采购订单', '30Y', 30, TRUE, NOW(), NOW()),
('OVT-DOC-004', 'DOCUMENT', '单据类', 'RECEIPT_ORDER', '入库单', '30Y', 40, TRUE, NOW(), NOW()),
('OVT-DOC-005', 'DOCUMENT', '单据类', 'PAYMENT_REQ', '付款申请单', '30Y', 50, TRUE, NOW(), NOW()),
('OVT-DOC-006', 'DOCUMENT', '单据类', 'EXPENSE_REPORT', '报销单', '30Y', 60, TRUE, NOW(), NOW()),

-- 发票类 (INVOICE)
('OVT-INV-001', 'INVOICE', '发票类', 'GEN_INVOICE', '普通发票', '30Y', 10, TRUE, NOW(), NOW()),
('OVT-INV-002', 'INVOICE', '发票类', 'VAT_INVOICE', '增值税专票', '30Y', 20, TRUE, NOW(), NOW()),

-- 银行类 (BANK)
('OVT-BNK-001', 'BANK', '银行类', 'BANK_SLIP', '银行回单', '30Y', 10, TRUE, NOW(), NOW()),
('OVT-BNK-002', 'BANK', '银行类', 'BANK_STATEMENT', '银行对账单', '30Y', 20, TRUE, NOW(), NOW()),

-- 合同类 (CONTRACT)
('OVT-CON-001', 'CONTRACT', '合同类', 'CONTRACT', '合同协议', '30Y', 10, TRUE, NOW(), NOW());
