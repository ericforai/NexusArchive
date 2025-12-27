-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- V36: 演示数据转为种子数据
-- 移除演示模式后的初始种子数据，供系统展示和测试使用

-- 0. 插入演示全宗 (如果不存在)
INSERT INTO bas_fonds (id, fonds_code, fonds_name, company_name, description, created_by, created_time)
SELECT 'demo-fonds-001', 'DEMO', '演示全宗', '演示公司', '系统初始演示数据', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM bas_fonds WHERE fonds_code = 'DEMO');

-- 1. 插入档案种子数据 (acc_archive)
-- 1.1 合同类档案
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_at)
SELECT 'seed-contract-001', 'DEMO', 'CON-2023-098', 'AC04', '年度技术服务协议', '2023', '01', '30Y', '演示公司', '系统', 'archived', 150000.00, '2023-01-15', 'CON-2023-098', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'CON-2023-098');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_at)
SELECT 'seed-contract-002', 'DEMO', 'C-202511-002', 'AC04', '服务器采购合同', '2025', '11', '30Y', '演示公司', '系统', 'archived', 450000.00, '2025-11-15', 'C-202511-002', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'C-202511-002');

-- 1.2 发票类档案
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_at)
SELECT 'seed-invoice-001', 'DEMO', 'INV-202311-089', 'AC01', '阿里云计算服务费发票', '2023', '11', '30Y', '演示公司', '系统', 'archived', 12800.00, '2023-11-02', 'INV-202311-089', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'INV-202311-089');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_at)
SELECT 'seed-invoice-002', 'DEMO', 'INV-202311-092', 'AC01', '服务器采购发票', '2023', '11', '30Y', '演示公司', '系统', 'archived', 45200.00, '2023-11-03', 'INV-202311-092', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'INV-202311-092');

-- 1.3 会计凭证类档案
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_at)
SELECT 'seed-voucher-001', 'DEMO', 'JZ-202311-0052', 'AC01', '11月技术部费用报销', '2023', '11', '30Y', '演示公司', '系统', 'archived', 58000.00, '2023-11-05', 'JZ-202311-0052', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'JZ-202311-0052');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_at)
SELECT 'seed-voucher-002', 'DEMO', 'V-202511-TEST', 'AC01', '报销差旅费', '2025', '11', '30Y', '演示公司', '张三', 'archived', 5280.00, '2025-11-07', 'V-202511-TEST', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'V-202511-TEST');

-- 1.4 银行回单类档案
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_at)
SELECT 'seed-receipt-001', 'DEMO', 'B-20231105-003', 'AC04', '招商银行付款回单', '2023', '11', '30Y', '演示公司', '系统', 'archived', 58000.00, '2023-11-05', 'B-20231105-003', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'B-20231105-003');

-- 1.5 财务报告类档案
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_at)
SELECT 'seed-report-001', 'DEMO', 'REP-2023-11', 'AC03', '11月科目余额表', '2023', '11', '30Y', '演示公司', '系统', 'archived', NULL, '2023-11-30', 'REP-2023-11', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'REP-2023-11');

-- 2. 插入档案关联关系 (acc_archive_relation)
-- 合同 → 凭证 (依据)
INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at)
SELECT 'seed-rel-001', 'seed-contract-001', 'seed-voucher-001', 'BASIS', '合同依据', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_relation WHERE id = 'seed-rel-001')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'seed-contract-001')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'seed-voucher-001');

-- 发票 → 凭证 (原始凭证)
INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at)
SELECT 'seed-rel-002', 'seed-invoice-001', 'seed-voucher-001', 'ORIGINAL_VOUCHER', '原始凭证', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_relation WHERE id = 'seed-rel-002')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'seed-invoice-001')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'seed-voucher-001');

INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at)
SELECT 'seed-rel-003', 'seed-invoice-002', 'seed-voucher-001', 'ORIGINAL_VOUCHER', '原始凭证', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_relation WHERE id = 'seed-rel-003')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'seed-invoice-002')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'seed-voucher-001');

-- 凭证 → 银行回单 (资金流)
INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at)
SELECT 'seed-rel-004', 'seed-voucher-001', 'seed-receipt-001', 'CASH_FLOW', '资金流', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_relation WHERE id = 'seed-rel-004')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'seed-voucher-001')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'seed-receipt-001');

-- 凭证 → 报告 (归档)
INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at)
SELECT 'seed-rel-005', 'seed-voucher-001', 'seed-report-001', 'ARCHIVE', '归档', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_relation WHERE id = 'seed-rel-005')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'seed-voucher-001')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'seed-report-001');

-- 3. 插入 ERP 集成通道配置 (sys_erp_config)
-- YonSuite 凭证同步
INSERT INTO sys_erp_config (name, erp_type, config_json, is_active, created_time)
SELECT '用友 YonSuite (生产环境)', 'YONSUITE', 
       '{"endpoint":"/integration/yonsuite/vouchers/sync","accbookCode":"BR01","frequency":"manual","description":"用友YonSuite会计凭证自动采集接口"}',
       1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_erp_config WHERE name = '用友 YonSuite (生产环境)');

-- SAP ERP 凭证同步
INSERT INTO sys_erp_config (name, erp_type, config_json, is_active, created_time)
SELECT 'SAP ERP (生产环境)', 'GENERIC',
       '{"system":"SAP ERP","frequency":"实时","description":"SAP 财务凭证自动同步接口"}',
       1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_erp_config WHERE name = 'SAP ERP (生产环境)');

-- 金蝶云星空 存货同步
INSERT INTO sys_erp_config (name, erp_type, config_json, is_active, created_time)
SELECT '金蝶云星空 (生产环境)', 'KINGDEE',
       '{"system":"金蝶云星空","frequency":"每日 23:00","description":"存货核算数据同步"}',
       1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_erp_config WHERE name = '金蝶云星空 (生产环境)');

-- 泛微OA 报销同步
INSERT INTO sys_erp_config (name, erp_type, config_json, is_active, created_time)
SELECT '泛微 OA (生产环境)', 'GENERIC',
       '{"system":"泛微OA","frequency":"每小时","description":"员工报销单据同步"}',
       1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_erp_config WHERE name = '泛微 OA (生产环境)');

-- 易快报 差旅同步
INSERT INTO sys_erp_config (name, erp_type, config_json, is_active, created_time)
SELECT '易快报 (生产环境)', 'GENERIC',
       '{"system":"易快报","frequency":"每小时","description":"差旅费用数据同步"}',
       1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_erp_config WHERE name = '易快报 (生产环境)');

-- 汇联易 报销同步
INSERT INTO sys_erp_config (name, erp_type, config_json, is_active, created_time)
SELECT '汇联易 (生产环境)', 'GENERIC',
       '{"system":"汇联易","frequency":"每小时","description":"费用报销同步","status":"error"}',
       0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_erp_config WHERE name = '汇联易 (生产环境)');

-- 完成
