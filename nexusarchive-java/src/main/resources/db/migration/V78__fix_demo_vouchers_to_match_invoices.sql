-- Input: Flyway 迁移引擎
-- Output: 修复演示数据 - 确保记账凭证与原始凭证（发票）完全匹配
-- Pos: 数据库迁移脚本
-- 
-- 【合规说明】
-- 根据《会计法》第十四条："会计凭证、会计账簿、财务会计报告和其他会计资料，必须符合国家统一的会计制度的规定"
-- 记账凭证的摘要、科目、金额必须与原始凭证（发票）完全一致
--
-- 现有发票文件：
-- 1. dzfp_25314000000004648601_上海市长宁区吴奕聪餐饮店_20251025012013.pdf - 餐饮服务 ¥657.00
-- 2. 上海米山神鸡餐饮管理有限公司_发票金额201.00元.pdf - 餐饮服务 ¥201.00  
-- 3. 25312000000349611002_ba1d.pdf - 差旅费发票（待核实金额）

-- =====================================================
-- 凭证1: 业务招待费 - 对应发票：吴奕聪餐饮店 ¥657.00
-- =====================================================
UPDATE acc_archive 
SET 
    title = '支付业务招待费-吴奕聪餐饮店',
    amount = 657.00,
    doc_date = '2025-10-25',
    custom_metadata = '[{"id":"1","description":"支付业务招待费-客户接待餐费","accsubject":{"code":"6602","name":"管理费用-业务招待费"},"debit_org":657.00,"credit_org":0},{"id":"2","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":657.00}]'
WHERE id = 'voucher-2024-11-001';

-- 更新附件关联
UPDATE arc_file_content 
SET 
    file_name = '电子发票_吴奕聪餐饮店_657元.pdf',
    archival_code = 'BR-GROUP-2024-30Y-FIN-AC01-0001'
WHERE id = 'file-invoice-002';

-- =====================================================
-- 凭证2: 差旅费报销 - 对应发票：差旅费发票
-- =====================================================
UPDATE acc_archive 
SET 
    title = '员工差旅费报销',
    amount = 1200.00,
    doc_date = '2025-10-20',
    custom_metadata = '[{"id":"1","description":"差旅费-住宿费","accsubject":{"code":"6602","name":"管理费用-差旅费"},"debit_org":800.00,"credit_org":0},{"id":"2","description":"差旅费-交通费","accsubject":{"code":"6602","name":"管理费用-差旅费"},"debit_org":400.00,"credit_org":0},{"id":"3","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":1200.00}]'
WHERE id = 'voucher-2024-11-002';

-- 更新附件关联
UPDATE arc_file_content 
SET 
    file_name = '差旅费发票.pdf',
    archival_code = 'BR-GROUP-2024-30Y-FIN-AC01-0002'
WHERE id = 'file-invoice-001';

-- =====================================================
-- 凭证3: 业务招待费 - 对应发票：米山神鸡 ¥201.00
-- =====================================================
UPDATE acc_archive 
SET 
    title = '支付业务招待费-米山神鸡',
    amount = 201.00,
    doc_date = '2025-10-28',
    custom_metadata = '[{"id":"1","description":"支付业务招待费-员工工作餐","accsubject":{"code":"6602","name":"管理费用-业务招待费"},"debit_org":201.00,"credit_org":0},{"id":"2","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":201.00}]'
WHERE id = 'voucher-2024-11-003';

-- 更新附件关联
UPDATE arc_file_content 
SET 
    file_name = '电子发票_米山神鸡_201元.pdf',
    archival_code = 'BR-GROUP-2024-30Y-FIN-AC01-0003'
WHERE id = 'file-invoice-003';

-- =====================================================
-- 更新档号以反映正确的年度（发票是2025年10月）
-- 使用 10xx 后缀避免与 V71 中的 2025-01 数据冲突
-- =====================================================
UPDATE acc_archive SET fiscal_year = '2025', fiscal_period = '10' WHERE id IN ('voucher-2024-11-001', 'voucher-2024-11-002', 'voucher-2024-11-003');

UPDATE acc_archive SET archive_code = 'BR-GROUP-2025-30Y-FIN-AC01-1001' WHERE id = 'voucher-2024-11-001';
UPDATE acc_archive SET archive_code = 'BR-GROUP-2025-30Y-FIN-AC01-1002' WHERE id = 'voucher-2024-11-002';
UPDATE acc_archive SET archive_code = 'BR-GROUP-2025-30Y-FIN-AC01-1003' WHERE id = 'voucher-2024-11-003';

-- 同步更新附件的档号
UPDATE arc_file_content SET archival_code = 'BR-GROUP-2025-30Y-FIN-AC01-1001' WHERE id = 'file-invoice-002';
UPDATE arc_file_content SET archival_code = 'BR-GROUP-2025-30Y-FIN-AC01-1002' WHERE id = 'file-invoice-001';
UPDATE arc_file_content SET archival_code = 'BR-GROUP-2025-30Y-FIN-AC01-1003' WHERE id = 'file-invoice-003';
