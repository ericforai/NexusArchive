-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- AC02 (Accounting Books) - Existing
INSERT INTO acc_archive (id, archive_code, fonds_no, title, category_code, fiscal_year, retention_period, custom_metadata, created_at, created_by, status, org_name)
VALUES
('seed-book-001', 'ARC-BOOK-2024-GL', 'COMP001', '2024年总账', 'AC02', '2024', '30Y', '{"bookType": "GENERAL_LEDGER", "pageCount": 100}', NOW(), 'system', 'archived', '总公司'),
('seed-book-002', 'ARC-BOOK-2024-CASH', 'COMP001', '2024年现金日记账', 'AC02', '2024', '30Y', '{"bookType": "CASH_JOURNAL", "pageCount": 50}', NOW(), 'system', 'archived', '总公司'),
('seed-book-003', 'ARC-BOOK-2024-BANK', 'COMP001', '2024年银行存款日记账', 'AC02', '2024', '30Y', '{"bookType": "BANK_JOURNAL", "pageCount": 50}', NOW(), 'system', 'archived', '总公司'),
('seed-book-004', 'ARC-BOOK-2024-FIXED', 'COMP001', '2024年固定资产卡片', 'AC02', '2024', '30Y', '{"bookType": "FIXED_ASSETS_CARD", "pageCount": 20}', NOW(), 'system', 'archived', '总公司');

-- AC03 (Financial Reports) - Time-based
INSERT INTO acc_archive (id, archive_code, fonds_no, title, category_code, fiscal_year, retention_period, custom_metadata, created_at, created_by, status, org_name)
VALUES
('seed-c03-001', 'ARC-REP-2024-M01', 'COMP001', '2024年1月财务月报', 'AC03', '2024', '10Y', '{"reportType": "MONTHLY", "period": "2024-01"}', NOW(), 'system', 'archived', '总公司'),
('seed-c03-002', 'ARC-REP-2024-Q1', 'COMP001', '2024年第一季度财务报表', 'AC03', '2024', '10Y', '{"reportType": "QUARTERLY", "period": "2024-Q1"}', NOW(), 'system', 'archived', '总公司'),
('seed-c03-003', 'ARC-REP-2023-ANN', 'COMP001', '2023年度财务决算报告', 'AC03', '2023', 'PERMANENT', '{"reportType": "ANNUAL", "period": "2023"}', NOW(), 'system', 'archived', '总公司');

-- AC04 (Other Accounting Materials)
INSERT INTO acc_archive (id, archive_code, fonds_no, title, category_code, fiscal_year, retention_period, custom_metadata, created_at, created_by, status, org_name)
VALUES
('seed-c04-001', 'ARC-OTH-2024-BK-01', 'COMP001', '2024年1月银行对账单', 'AC04', '2024', '10Y', '{"otherType": "BANK_STATEMENT"}', NOW(), 'system', 'archived', '总公司'),
('seed-c04-002', 'ARC-OTH-2024-TAX-01', 'COMP001', '2024年1月增值税纳税申报表', 'AC04', '2024', '10Y', '{"otherType": "TAX_RETURN"}', NOW(), 'system', 'archived', '总公司'),
('seed-c04-003', 'ARC-OTH-2024-HO-01', 'COMP001', '2024年度会计档案移交清册', 'AC04', '2024', '30Y', '{"otherType": "HANDOVER_REGISTER"}', NOW(), 'system', 'archived', '档案室');
