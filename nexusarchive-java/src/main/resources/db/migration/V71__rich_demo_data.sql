-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- =====================================================
-- V71: 丰富的演示数据 - 泊冉集团电子会计档案系统
-- 
-- 目标：为每个功能模块填充真实场景的演示数据
-- 覆盖年度：2022-2025
-- 公司实体：泊冉集团及其子公司
-- =====================================================

-- =====================================================
-- 一、全宗数据
-- =====================================================
INSERT INTO bas_fonds (id, fonds_code, fonds_name, company_name, description, created_by, created_time)
SELECT 'fonds-br-group', 'BR-GROUP', '泊冉集团有限公司', '泊冉集团有限公司', '集团总部档案全宗', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM bas_fonds WHERE fonds_code = 'BR-GROUP');

INSERT INTO bas_fonds (id, fonds_code, fonds_name, company_name, description, created_by, created_time)
SELECT 'fonds-br-sales', 'BR-SALES', '泊冉销售有限公司', '泊冉销售有限公司', '销售公司档案全宗', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM bas_fonds WHERE fonds_code = 'BR-SALES');

INSERT INTO bas_fonds (id, fonds_code, fonds_name, company_name, description, created_by, created_time)
SELECT 'fonds-br-trade', 'BR-TRADE', '泊冉国际贸易有限公司', '泊冉国际贸易有限公司', '贸易公司档案全宗', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM bas_fonds WHERE fonds_code = 'BR-TRADE');

INSERT INTO bas_fonds (id, fonds_code, fonds_name, company_name, description, created_by, created_time)
SELECT 'fonds-br-mfg', 'BR-MFG', '泊冉制造有限公司', '泊冉制造有限公司', '制造公司档案全宗', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM bas_fonds WHERE fonds_code = 'BR-MFG');

-- =====================================================
-- 二、演示用户（补充角色）
-- =====================================================
-- 档案员 张三
INSERT INTO sys_user (id, username, password_hash, full_name, org_code, email, department_id, status, created_at, updated_at, deleted)
SELECT 'user-zhangsan', 'zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '张三', 'BR-GROUP', 'zhangsan@boran.com', 'ORG_BR_GROUP_FIN', 'active', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'zhangsan');

-- 财务主管 李四
INSERT INTO sys_user (id, username, password_hash, full_name, org_code, email, department_id, status, created_at, updated_at, deleted)
SELECT 'user-lisi', 'lisi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '李四', 'BR-SALES', 'lisi@boran.com', 'ORG_BR_SALES_FIN', 'active', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'lisi');

-- 审计专员 王五
INSERT INTO sys_user (id, username, password_hash, full_name, org_code, email, department_id, status, created_at, updated_at, deleted)
SELECT 'user-wangwu', 'wangwu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '王五', 'BR-GROUP', 'wangwu@boran.com', 'ORG_BR_GROUP_AUDIT', 'active', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'wangwu');

-- 普通用户 赵六
INSERT INTO sys_user (id, username, password_hash, full_name, org_code, email, department_id, status, created_at, updated_at, deleted)
SELECT 'user-zhaoliu', 'zhaoliu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '赵六', 'BR-MFG', 'zhaoliu@boran.com', 'ORG_BR_MFG_FIN', 'active', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'zhaoliu');

-- 档案主管 钱七
INSERT INTO sys_user (id, username, password_hash, full_name, org_code, email, department_id, status, created_at, updated_at, deleted)
SELECT 'user-qianqi', 'qianqi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '钱七', 'BR-GROUP', 'qianqi@boran.com', 'ORG_BR_GROUP_FIN', 'active', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'qianqi');

-- =====================================================
-- 二.B 演示用户权限分配 (必须分配角色以通过鉴权)
-- =====================================================
-- 为所有演示用户分配 super_admin 角色 (简化演示环境配置)
INSERT INTO sys_user_role (user_id, role_id)
SELECT id, 'role_super_admin' FROM sys_user WHERE username IN ('zhangsan', 'lisi', 'wangwu', 'zhaoliu', 'qianqi')
ON CONFLICT DO NOTHING;

-- =====================================================
-- 三、会计凭证数据 (AC01) - 2022-2025年度
-- 按年度、月份创建会计凭证，支持全景视图的层级展示
-- =====================================================

-- 2024年11月凭证 (主要演示数据，详细分录)
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-11-001', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-0001', 'AC01', '采购办公用品', '2024', '11', '30Y', '泊冉集团有限公司', '张三', 'archived', 2580.00, '2024-11-05', 'JZ-202411-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"采购办公用品","accsubject":{"code":"6602","name":"管理费用-办公费"},"debit_org":2580.00,"credit_org":0},{"id":"2","description":"采购办公用品","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":2580.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-0001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-11-002', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-0002', 'AC01', '支付员工差旅费报销', '2024', '11', '30Y', '泊冉集团有限公司', '张三', 'archived', 5680.00, '2024-11-06', 'JZ-202411-0002', 'internal', 'system', NOW(),
'[{"id":"1","description":"差旅费-机票","accsubject":{"code":"6602","name":"管理费用-差旅费"},"debit_org":3200.00,"credit_org":0},{"id":"2","description":"差旅费-住宿","accsubject":{"code":"6602","name":"管理费用-差旅费"},"debit_org":1580.00,"credit_org":0},{"id":"3","description":"差旅费-市内交通","accsubject":{"code":"6602","name":"管理费用-差旅费"},"debit_org":900.00,"credit_org":0},{"id":"4","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":5680.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-0002');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-11-003', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-0003', 'AC01', '收到客户货款', '2024', '11', '30Y', '泊冉集团有限公司', '李四', 'archived', 158000.00, '2024-11-08', 'JZ-202411-0003', 'internal', 'system', NOW(),
'[{"id":"1","description":"收到华为技术有限公司货款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":158000.00,"credit_org":0},{"id":"2","description":"核销应收账款","accsubject":{"code":"1122","name":"应收账款"},"debit_org":0,"credit_org":158000.00,"aux_info":"客户:华为技术有限公司"}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-0003');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-11-004', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-0004', 'AC01', '支付阿里云服务器费用', '2024', '11', '30Y', '泊冉集团有限公司', '张三', 'archived', 12800.00, '2024-11-10', 'JZ-202411-0004', 'internal', 'system', NOW(),
'[{"id":"1","description":"阿里云ECS服务器年费","accsubject":{"code":"6602","name":"管理费用-技术服务费"},"debit_org":12800.00,"credit_org":0},{"id":"2","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":12800.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-0004');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-11-005', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-0005', 'AC01', '计提本月工资', '2024', '11', '30Y', '泊冉集团有限公司', '李四', 'archived', 856000.00, '2024-11-25', 'JZ-202411-0005', 'internal', 'system', NOW(),
'[{"id":"1","description":"管理人员工资","accsubject":{"code":"6602","name":"管理费用-工资"},"debit_org":356000.00,"credit_org":0},{"id":"2","description":"销售人员工资","accsubject":{"code":"6601","name":"销售费用-工资"},"debit_org":280000.00,"credit_org":0},{"id":"3","description":"生产人员工资","accsubject":{"code":"5001","name":"生产成本-直接人工"},"debit_org":220000.00,"credit_org":0},{"id":"4","description":"应付工资","accsubject":{"code":"2211","name":"应付职工薪酬"},"debit_org":0,"credit_org":856000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-0005');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-11-006', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-0006', 'AC01', '固定资产折旧计提', '2024', '11', '30Y', '泊冉集团有限公司', '张三', 'archived', 45600.00, '2024-11-28', 'JZ-202411-0006', 'internal', 'system', NOW(),
'[{"id":"1","description":"管理部门折旧","accsubject":{"code":"6602","name":"管理费用-折旧费"},"debit_org":18500.00,"credit_org":0},{"id":"2","description":"生产部门折旧","accsubject":{"code":"5001","name":"生产成本-制造费用"},"debit_org":27100.00,"credit_org":0},{"id":"3","description":"累计折旧","accsubject":{"code":"1602","name":"累计折旧"},"debit_org":0,"credit_org":45600.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-0006');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-11-007', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-0007', 'AC01', '支付供应商货款', '2024', '11', '30Y', '泊冉集团有限公司', '李四', 'archived', 286500.00, '2024-11-12', 'JZ-202411-0007', 'internal', 'system', NOW(),
'[{"id":"1","description":"支付苏州精密机械有限公司货款","accsubject":{"code":"2202","name":"应付账款"},"debit_org":286500.00,"credit_org":0},{"id":"2","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":286500.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-0007');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-11-008', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-0008', 'AC01', '销售商品确认收入', '2024', '11', '30Y', '泊冉集团有限公司', '李四', 'archived', 520000.00, '2024-11-15', 'JZ-202411-0008', 'internal', 'system', NOW(),
'[{"id":"1","description":"销售智能设备","accsubject":{"code":"1122","name":"应收账款"},"debit_org":587600.00,"credit_org":0},{"id":"2","description":"确认收入","accsubject":{"code":"6001","name":"主营业务收入"},"debit_org":0,"credit_org":520000.00},{"id":"3","description":"销项税额","accsubject":{"code":"2221","name":"应交税费-应交增值税(销项)"},"debit_org":0,"credit_org":67600.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-0008');

-- 2024年10月凭证
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-10-001', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-1001', 'AC01', '支付房租及物业费', '2024', '10', '30Y', '泊冉集团有限公司', '张三', 'archived', 85000.00, '2024-10-05', 'JZ-202410-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"办公楼租金","accsubject":{"code":"6602","name":"管理费用-租赁费"},"debit_org":68000.00,"credit_org":0},{"id":"2","description":"物业管理费","accsubject":{"code":"6602","name":"管理费用-物业费"},"debit_org":17000.00,"credit_org":0},{"id":"3","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":85000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-1001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-10-002', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-1002', 'AC01', '采购原材料入库', '2024', '10', '30Y', '泊冉集团有限公司', '李四', 'archived', 468000.00, '2024-10-12', 'JZ-202410-0002', 'internal', 'system', NOW(),
'[{"id":"1","description":"原材料入库-钢材","accsubject":{"code":"1403","name":"原材料"},"debit_org":410619.47,"credit_org":0},{"id":"2","description":"进项税额","accsubject":{"code":"2221","name":"应交税费-应交增值税(进项)"},"debit_org":57380.53,"credit_org":0},{"id":"3","description":"暂估应付","accsubject":{"code":"2202","name":"应付账款"},"debit_org":0,"credit_org":468000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-1002');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-10-003', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-1003', 'AC01', '计提本月工资', '2024', '10', '30Y', '泊冉集团有限公司', '李四', 'archived', 842000.00, '2024-10-25', 'JZ-202410-0003', 'internal', 'system', NOW(),
'[{"id":"1","description":"管理人员工资","accsubject":{"code":"6602","name":"管理费用-工资"},"debit_org":348000.00,"credit_org":0},{"id":"2","description":"销售人员工资","accsubject":{"code":"6601","name":"销售费用-工资"},"debit_org":276000.00,"credit_org":0},{"id":"3","description":"生产人员工资","accsubject":{"code":"5001","name":"生产成本-直接人工"},"debit_org":218000.00,"credit_org":0},{"id":"4","description":"应付工资","accsubject":{"code":"2211","name":"应付职工薪酬"},"debit_org":0,"credit_org":842000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-1003');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-10-004', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-1004', 'AC01', '支付水电费', '2024', '10', '30Y', '泊冉集团有限公司', '张三', 'archived', 28600.00, '2024-10-18', 'JZ-202410-0004', 'internal', 'system', NOW(),
'[{"id":"1","description":"办公楼水电费","accsubject":{"code":"6602","name":"管理费用-水电费"},"debit_org":8600.00,"credit_org":0},{"id":"2","description":"生产车间水电费","accsubject":{"code":"5001","name":"生产成本-制造费用"},"debit_org":20000.00,"credit_org":0},{"id":"3","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":28600.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-1004');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-10-005', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-1005', 'AC01', '结转本月成本', '2024', '10', '30Y', '泊冉集团有限公司', '李四', 'archived', 380000.00, '2024-10-30', 'JZ-202410-0005', 'internal', 'system', NOW(),
'[{"id":"1","description":"结转销售成本","accsubject":{"code":"6401","name":"主营业务成本"},"debit_org":380000.00,"credit_org":0},{"id":"2","description":"库存商品减少","accsubject":{"code":"1405","name":"库存商品"},"debit_org":0,"credit_org":380000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-1005');

-- 2024年9月凭证
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-09-001', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-2001', 'AC01', '购买固定资产-服务器', '2024', '09', '30Y', '泊冉集团有限公司', '张三', 'archived', 185000.00, '2024-09-08', 'JZ-202409-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"华为服务器采购","accsubject":{"code":"1601","name":"固定资产"},"debit_org":163716.81,"credit_org":0},{"id":"2","description":"进项税额","accsubject":{"code":"2221","name":"应交税费-应交增值税(进项)"},"debit_org":21283.19,"credit_org":0},{"id":"3","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":185000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-2001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-09-002', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-2002', 'AC01', '收到销售佣金收入', '2024', '09', '30Y', '泊冉集团有限公司', '李四', 'archived', 68000.00, '2024-09-15', 'JZ-202409-0002', 'internal', 'system', NOW(),
'[{"id":"1","description":"收到代理佣金","accsubject":{"code":"1002","name":"银行存款"},"debit_org":68000.00,"credit_org":0},{"id":"2","description":"确认其他收入","accsubject":{"code":"6051","name":"其他业务收入"},"debit_org":0,"credit_org":60176.99},{"id":"3","description":"销项税额","accsubject":{"code":"2221","name":"应交税费-应交增值税(销项)"},"debit_org":0,"credit_org":7823.01}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-2002');

-- 2024年12月凭证（部分）
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-12-001', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-3001', 'AC01', '支付年终审计费', '2024', '12', '30Y', '泊冉集团有限公司', '张三', 'archived', 88000.00, '2024-12-10', 'JZ-202412-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"年度审计服务费","accsubject":{"code":"6602","name":"管理费用-审计费"},"debit_org":77876.11,"credit_org":0},{"id":"2","description":"进项税额","accsubject":{"code":"2221","name":"应交税费-应交增值税(进项)"},"debit_org":10123.89,"credit_org":0},{"id":"3","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":88000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-3001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2024-12-002', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-3002', 'AC01', '年终奖金计提', '2024', '12', '30Y', '泊冉集团有限公司', '李四', 'archived', 2580000.00, '2024-12-20', 'JZ-202412-0002', 'internal', 'system', NOW(),
'[{"id":"1","description":"管理层年终奖","accsubject":{"code":"6602","name":"管理费用-工资"},"debit_org":1200000.00,"credit_org":0},{"id":"2","description":"销售奖金","accsubject":{"code":"6601","name":"销售费用-工资"},"debit_org":880000.00,"credit_org":0},{"id":"3","description":"生产绩效奖","accsubject":{"code":"5001","name":"生产成本-直接人工"},"debit_org":500000.00,"credit_org":0},{"id":"4","description":"应付年终奖","accsubject":{"code":"2211","name":"应付职工薪酬"},"debit_org":0,"credit_org":2580000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC01-3002');

-- 2023年凭证（历史数据）
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2023-12-001', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC01-0001', 'AC01', '结转年度利润', '2023', '12', '30Y', '泊冉集团有限公司', '李四', 'archived', 3860000.00, '2023-12-31', 'JZ-202312-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"结转收入","accsubject":{"code":"6001","name":"主营业务收入"},"debit_org":12580000.00,"credit_org":0},{"id":"2","description":"结转成本","accsubject":{"code":"6401","name":"主营业务成本"},"debit_org":0,"credit_org":7200000.00},{"id":"3","description":"结转费用","accsubject":{"code":"6602","name":"管理费用"},"debit_org":0,"credit_org":1520000.00},{"id":"4","description":"本年利润","accsubject":{"code":"3131","name":"本年利润"},"debit_org":0,"credit_org":3860000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2023-30Y-FIN-AC01-0001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2023-11-001', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC01-0011', 'AC01', '支付技术服务费', '2023', '11', '30Y', '泊冉集团有限公司', '张三', 'archived', 45800.00, '2023-11-15', 'JZ-202311-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"华为云年度服务费","accsubject":{"code":"6602","name":"管理费用-技术服务费"},"debit_org":40530.97,"credit_org":0},{"id":"2","description":"进项税额","accsubject":{"code":"2221","name":"应交税费-应交增值税(进项)"},"debit_org":5269.03,"credit_org":0},{"id":"3","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":45800.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2023-30Y-FIN-AC01-0011');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2023-10-001', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC01-0021', 'AC01', '采购生产设备', '2023', '10', '30Y', '泊冉集团有限公司', '李四', 'archived', 580000.00, '2023-10-20', 'JZ-202310-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"数控机床采购","accsubject":{"code":"1601","name":"固定资产"},"debit_org":513274.34,"credit_org":0},{"id":"2","description":"进项税额","accsubject":{"code":"2221","name":"应交税费-应交增值税(进项)"},"debit_org":66725.66,"credit_org":0},{"id":"3","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":580000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2023-30Y-FIN-AC01-0021');

-- 2022年凭证（历史数据）
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2022-12-001', 'BR-GROUP', 'BR-GROUP-2022-30Y-FIN-AC01-0001', 'AC01', '年度损益结转', '2022', '12', '30Y', '泊冉集团有限公司', '李四', 'archived', 2680000.00, '2022-12-31', 'JZ-202212-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"结转本年利润","accsubject":{"code":"3131","name":"本年利润"},"debit_org":2680000.00,"credit_org":0},{"id":"2","description":"利润分配","accsubject":{"code":"3141","name":"利润分配"},"debit_org":0,"credit_org":2680000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2022-30Y-FIN-AC01-0001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2022-06-001', 'BR-GROUP', 'BR-GROUP-2022-30Y-FIN-AC01-0011', 'AC01', '半年度奖金发放', '2022', '06', '30Y', '泊冉集团有限公司', '李四', 'archived', 1250000.00, '2022-06-30', 'JZ-202206-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"应付工资结转","accsubject":{"code":"2211","name":"应付职工薪酬"},"debit_org":1250000.00,"credit_org":0},{"id":"2","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":1250000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2022-30Y-FIN-AC01-0011');

-- 2025年凭证
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2025-01-001', 'BR-GROUP', 'BR-GROUP-2025-30Y-FIN-AC01-0001', 'AC01', '支付员工年终奖', '2025', '01', '30Y', '泊冉集团有限公司', '李四', 'archived', 2580000.00, '2025-01-15', 'JZ-202501-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"发放年终奖金","accsubject":{"code":"2211","name":"应付职工薪酬"},"debit_org":2580000.00,"credit_org":0},{"id":"2","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":2580000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2025-30Y-FIN-AC01-0001');

-- 子公司凭证 - 销售公司
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-sales-2024-11-001', 'BR-SALES', 'BR-SALES-2024-30Y-FIN-AC01-0001', 'AC01', '销售产品收入', '2024', '11', '30Y', '泊冉销售有限公司', '李四', 'archived', 1280000.00, '2024-11-18', 'JZ-SALES-202411-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"销售智能设备A型","accsubject":{"code":"1122","name":"应收账款"},"debit_org":1446400.00,"credit_org":0},{"id":"2","description":"确认收入","accsubject":{"code":"6001","name":"主营业务收入"},"debit_org":0,"credit_org":1280000.00},{"id":"3","description":"销项税额","accsubject":{"code":"2221","name":"应交税费-应交增值税(销项)"},"debit_org":0,"credit_org":166400.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-SALES-2024-30Y-FIN-AC01-0001');

-- 子公司凭证 - 贸易公司
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-trade-2024-11-001', 'BR-TRADE', 'BR-TRADE-2024-30Y-FIN-AC01-0001', 'AC01', '进口设备采购', '2024', '11', '30Y', '泊冉国际贸易有限公司', '张三', 'archived', 860000.00, '2024-11-22', 'JZ-TRADE-202411-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"进口精密仪器","accsubject":{"code":"1403","name":"原材料"},"debit_org":761061.95,"credit_org":0},{"id":"2","description":"进项税额","accsubject":{"code":"2221","name":"应交税费-应交增值税(进项)"},"debit_org":98938.05,"credit_org":0},{"id":"3","description":"应付账款","accsubject":{"code":"2202","name":"应付账款"},"debit_org":0,"credit_org":860000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-TRADE-2024-30Y-FIN-AC01-0001');

-- 子公司凭证 - 制造公司
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-mfg-2024-11-001', 'BR-MFG', 'BR-MFG-2024-30Y-FIN-AC01-0001', 'AC01', '生产材料领用', '2024', '11', '30Y', '泊冉制造有限公司', '赵六', 'archived', 156000.00, '2024-11-08', 'JZ-MFG-202411-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"领用钢材","accsubject":{"code":"5001","name":"生产成本-直接材料"},"debit_org":156000.00,"credit_org":0},{"id":"2","description":"原材料减少","accsubject":{"code":"1403","name":"原材料"},"debit_org":0,"credit_org":156000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-MFG-2024-30Y-FIN-AC01-0001');

-- =====================================================
-- 四、会计账簿数据 (AC02) - 2022-2024年度
-- =====================================================
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'ledger-2024-001', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC02-ZZ001', 'AC02', '2024年度总账', '2024', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2024-12-31', 'ZZ-2024-001', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC02-ZZ001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'ledger-2024-002', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC02-MX001', 'AC02', '2024年度银行存款明细账', '2024', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2024-12-31', 'MX-2024-001', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC02-MX001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'ledger-2024-003', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC02-RJ001', 'AC02', '2024年度现金日记账', '2024', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2024-12-31', 'RJ-2024-001', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC02-RJ001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'ledger-2024-004', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC02-YS001', 'AC02', '2024年度应收账款明细账', '2024', '00', '30Y', '泊冉集团有限公司', '李四', 'archived', NULL, '2024-12-31', 'YS-2024-001', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC02-YS001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'ledger-2024-005', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC02-GD001', 'AC02', '2024年度固定资产卡片', '2024', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2024-12-31', 'GD-2024-001', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC02-GD001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'ledger-2023-001', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC02-ZZ001', 'AC02', '2023年度总账', '2023', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2023-12-31', 'ZZ-2023-001', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2023-30Y-FIN-AC02-ZZ001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'ledger-2023-002', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC02-MX001', 'AC02', '2023年度银行存款明细账', '2023', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2023-12-31', 'MX-2023-001', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2023-30Y-FIN-AC02-MX001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'ledger-2022-001', 'BR-GROUP', 'BR-GROUP-2022-30Y-FIN-AC02-ZZ001', 'AC02', '2022年度总账', '2022', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2022-12-31', 'ZZ-2022-001', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2022-30Y-FIN-AC02-ZZ001');

-- =====================================================
-- 五、财务报告数据 (AC03) - 2022-2024年度
-- =====================================================
-- 2024年月度资产负债表
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'report-2024-zcfz-11', 'BR-GROUP', 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ11', 'AC03', '2024年11月资产负债表', '2024', '11', 'PERMANENT', '泊冉集团有限公司', '李四', 'archived', NULL, '2024-11-30', 'ZCFZ-202411', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ11');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'report-2024-zcfz-10', 'BR-GROUP', 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ10', 'AC03', '2024年10月资产负债表', '2024', '10', 'PERMANENT', '泊冉集团有限公司', '李四', 'archived', NULL, '2024-10-31', 'ZCFZ-202410', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ10');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'report-2024-zcfz-09', 'BR-GROUP', 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ09', 'AC03', '2024年9月资产负债表', '2024', '09', 'PERMANENT', '泊冉集团有限公司', '李四', 'archived', NULL, '2024-09-30', 'ZCFZ-202409', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ09');

-- 2024年月度利润表
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'report-2024-lr-11', 'BR-GROUP', 'BR-GROUP-2024-PERM-FIN-AC03-LR11', 'AC03', '2024年11月利润表', '2024', '11', 'PERMANENT', '泊冉集团有限公司', '李四', 'archived', NULL, '2024-11-30', 'LR-202411', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-PERM-FIN-AC03-LR11');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'report-2024-lr-10', 'BR-GROUP', 'BR-GROUP-2024-PERM-FIN-AC03-LR10', 'AC03', '2024年10月利润表', '2024', '10', 'PERMANENT', '泊冉集团有限公司', '李四', 'archived', NULL, '2024-10-31', 'LR-202410', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-PERM-FIN-AC03-LR10');

-- 2024年季度现金流量表
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'report-2024-xjll-q3', 'BR-GROUP', 'BR-GROUP-2024-PERM-FIN-AC03-XJLL-Q3', 'AC03', '2024年第三季度现金流量表', '2024', 'Q3', 'PERMANENT', '泊冉集团有限公司', '李四', 'archived', NULL, '2024-09-30', 'XJLL-2024Q3', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-PERM-FIN-AC03-XJLL-Q3');

-- 年度财务报告
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'report-2023-annual', 'BR-GROUP', 'BR-GROUP-2023-PERM-FIN-AC03-ANNUAL', 'AC03', '2023年度财务决算报告', '2023', '00', 'PERMANENT', '泊冉集团有限公司', '李四', 'archived', NULL, '2023-12-31', 'ANNUAL-2023', 'confidential', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2023-PERM-FIN-AC03-ANNUAL');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'report-2022-annual', 'BR-GROUP', 'BR-GROUP-2022-PERM-FIN-AC03-ANNUAL', 'AC03', '2022年度财务决算报告', '2022', '00', 'PERMANENT', '泊冉集团有限公司', '李四', 'archived', NULL, '2022-12-31', 'ANNUAL-2022', 'confidential', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2022-PERM-FIN-AC03-ANNUAL');

-- =====================================================
-- 六、其他会计资料数据 (AC04) - 银行对账单、纳税申报表等
-- =====================================================
-- 银行对账单
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'other-bank-2024-11', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC04-BANK11', 'AC04', '2024年11月招商银行对账单', '2024', '11', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2024-11-30', 'BANK-202411', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC04-BANK11');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'other-bank-2024-10', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC04-BANK10', 'AC04', '2024年10月招商银行对账单', '2024', '10', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2024-10-31', 'BANK-202410', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC04-BANK10');

-- 纳税申报表
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'other-tax-2024-11', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC04-TAX11', 'AC04', '2024年11月增值税纳税申报表', '2024', '11', '30Y', '泊冉集团有限公司', '李四', 'archived', 168500.00, '2024-12-15', 'TAX-202411', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC04-TAX11');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'other-tax-2024-q3', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC04-TAX-Q3', 'AC04', '2024年第三季度企业所得税预缴申报表', '2024', 'Q3', '30Y', '泊冉集团有限公司', '李四', 'archived', 286000.00, '2024-10-20', 'TAX-2024Q3', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC04-TAX-Q3');

-- 合同
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'other-contract-2024-001', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC04-CON001', 'AC04', '华为云年度服务合同', '2024', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', 158000.00, '2024-01-15', 'CON-2024-001', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC04-CON001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'other-contract-2024-002', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC04-CON002', 'AC04', '办公楼租赁合同', '2024', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', 816000.00, '2024-01-01', 'CON-2024-002', 'confidential', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC04-CON002');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'other-contract-2024-003', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC04-CON003', 'AC04', '年度审计服务协议', '2024', '00', '30Y', '泊冉集团有限公司', '钱七', 'archived', 88000.00, '2024-03-01', 'CON-2024-003', 'internal', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2024-30Y-FIN-AC04-CON003');

-- 审计报告
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'other-audit-2023', 'BR-GROUP', 'BR-GROUP-2023-PERM-FIN-AC04-AUDIT', 'AC04', '2023年度审计报告', '2023', '00', 'PERMANENT', '泊冉集团有限公司', '王五', 'archived', NULL, '2024-03-15', 'AUDIT-2023', 'confidential', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2023-PERM-FIN-AC04-AUDIT');

-- =====================================================
-- 七、档案关联关系（凭证与原始凭证/合同的穿透）
-- =====================================================
-- 凭证 → 合同关联
INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at)
SELECT 'rel-v2024-11-004-con', 'voucher-2024-11-004', 'other-contract-2024-001', 'BASIS', '合同依据', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_relation WHERE id = 'rel-v2024-11-004-con')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'voucher-2024-11-004')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'other-contract-2024-001');

INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at)
SELECT 'rel-v2024-10-001-con', 'voucher-2024-10-001', 'other-contract-2024-002', 'BASIS', '租赁合同依据', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_relation WHERE id = 'rel-v2024-10-001-con')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'voucher-2024-10-001')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'other-contract-2024-002');

INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at)
SELECT 'rel-v2024-12-001-con', 'voucher-2024-12-001', 'other-contract-2024-003', 'BASIS', '审计服务合同依据', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_relation WHERE id = 'rel-v2024-12-001-con')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'voucher-2024-12-001')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'other-contract-2024-003');

-- 凭证 → 报表关联
INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at)
SELECT 'rel-v2024-11-005-lr', 'voucher-2024-11-005', 'report-2024-lr-11', 'ARCHIVE', '归入月度报表', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_relation WHERE id = 'rel-v2024-11-005-lr')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'voucher-2024-11-005')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'report-2024-lr-11');

-- =====================================================
-- 八、案卷数据
-- =====================================================
INSERT INTO acc_archive_volume (id, volume_code, title, fonds_no, fiscal_year, fiscal_period, category_code, file_count, retention_period, status, reviewed_by, archived_at, created_time)
SELECT 'volume-2024-11', 'AJ-2024-11', '2024年11月会计凭证', 'BR-GROUP', '2024', '11', 'AC01', 8, '30Y', 'archived', 'user-qianqi', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_volume WHERE volume_code = 'AJ-2024-11');

INSERT INTO acc_archive_volume (id, volume_code, title, fonds_no, fiscal_year, fiscal_period, category_code, file_count, retention_period, status, reviewed_by, archived_at, created_time)
SELECT 'volume-2024-10', 'AJ-2024-10', '2024年10月会计凭证', 'BR-GROUP', '2024', '10', 'AC01', 5, '30Y', 'archived', 'user-qianqi', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_volume WHERE volume_code = 'AJ-2024-10');

INSERT INTO acc_archive_volume (id, volume_code, title, fonds_no, fiscal_year, fiscal_period, category_code, file_count, retention_period, status, reviewed_by, archived_at, created_time)
SELECT 'volume-2024-09', 'AJ-2024-09', '2024年9月会计凭证', 'BR-GROUP', '2024', '09', 'AC01', 2, '30Y', 'archived', 'user-qianqi', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_volume WHERE volume_code = 'AJ-2024-09');

INSERT INTO acc_archive_volume (id, volume_code, title, fonds_no, fiscal_year, fiscal_period, category_code, file_count, retention_period, status, reviewed_by, archived_at, created_time)
SELECT 'volume-2023-12', 'AJ-2023-12', '2023年12月会计凭证', 'BR-GROUP', '2023', '12', 'AC01', 2, '30Y', 'archived', 'user-qianqi', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_volume WHERE volume_code = 'AJ-2023-12');

-- 关联凭证到案卷
UPDATE acc_archive SET volume_id = 'volume-2024-11' WHERE id LIKE 'voucher-2024-11-%' AND volume_id IS NULL;
UPDATE acc_archive SET volume_id = 'volume-2024-10' WHERE id LIKE 'voucher-2024-10-%' AND volume_id IS NULL;
UPDATE acc_archive SET volume_id = 'volume-2024-09' WHERE id LIKE 'voucher-2024-09-%' AND volume_id IS NULL;
UPDATE acc_archive SET volume_id = 'volume-2023-12' WHERE id LIKE 'voucher-2023-12-%' AND volume_id IS NULL;

-- =====================================================
-- 九、借阅记录
-- =====================================================
INSERT INTO biz_borrowing (id, user_id, user_name, archive_id, archive_title, reason, borrow_date, expected_return_date, actual_return_date, status, approval_comment, created_at)
SELECT 'borrow-001', 'user-wangwu', '王五', 'voucher-2024-11-008', '销售商品确认收入', '年度审计核查销售收入确认', '2024-12-15', '2024-12-25', NULL, 'APPROVED', '审批通过，请于期限内归还', NOW()
WHERE NOT EXISTS (SELECT 1 FROM biz_borrowing WHERE id = 'borrow-001');

INSERT INTO biz_borrowing (id, user_id, user_name, archive_id, archive_title, reason, borrow_date, expected_return_date, actual_return_date, status, approval_comment, created_at)
SELECT 'borrow-002', 'user-lisi', '李四', 'report-2023-annual', '2023年度财务决算报告', '编制2024年预算参考', '2024-11-20', '2024-11-30', '2024-11-28', 'RETURNED', '已按期归还', NOW()
WHERE NOT EXISTS (SELECT 1 FROM biz_borrowing WHERE id = 'borrow-002');

INSERT INTO biz_borrowing (id, user_id, user_name, archive_id, archive_title, reason, borrow_date, expected_return_date, actual_return_date, status, approval_comment, created_at)
SELECT 'borrow-003', 'user-zhaoliu', '赵六', 'other-contract-2024-002', '办公楼租赁合同', '核对租金支付条款', '2024-12-18', '2024-12-22', NULL, 'PENDING', NULL, NOW()
WHERE NOT EXISTS (SELECT 1 FROM biz_borrowing WHERE id = 'borrow-003');

INSERT INTO biz_borrowing (id, user_id, user_name, archive_id, archive_title, reason, borrow_date, expected_return_date, actual_return_date, status, approval_comment, created_at)
SELECT 'borrow-004', 'user-wangwu', '王五', 'voucher-2023-10-001', '采购生产设备', '专项审计-固定资产核查', '2024-10-10', '2024-10-20', '2024-10-18', 'RETURNED', '已完成审计', NOW()
WHERE NOT EXISTS (SELECT 1 FROM biz_borrowing WHERE id = 'borrow-004');

INSERT INTO biz_borrowing (id, user_id, user_name, archive_id, archive_title, reason, borrow_date, expected_return_date, actual_return_date, status, approval_comment, created_at)
SELECT 'borrow-005', 'user-zhangsan', '张三', 'ledger-2024-002', '2024年度银行存款明细账', '月末对账核实', '2024-12-20', '2024-12-23', NULL, 'APPROVED', '同意借阅', NOW()
WHERE NOT EXISTS (SELECT 1 FROM biz_borrowing WHERE id = 'borrow-005');

-- =====================================================
-- 十、销毁鉴定数据（历史到期档案）
-- =====================================================
-- 2014年档案（已满10年保管期）
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'voucher-2014-01-001', 'BR-GROUP', 'BR-GROUP-2014-10Y-FIN-AC01-0001', 'AC01', '2014年1月记账凭证汇总', '2014', '01', '10Y', '泊冉集团有限公司', '系统', 'archived', 580000.00, '2014-01-31', 'JZ-201401-0001', 'internal', 'system', '2014-02-01'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2014-10Y-FIN-AC01-0001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'voucher-2014-02-001', 'BR-GROUP', 'BR-GROUP-2014-10Y-FIN-AC01-0002', 'AC01', '2014年2月记账凭证汇总', '2014', '02', '10Y', '泊冉集团有限公司', '系统', 'archived', 620000.00, '2014-02-28', 'JZ-201402-0001', 'internal', 'system', '2014-03-01'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2014-10Y-FIN-AC01-0002');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time)
SELECT 'bank-2014-q1', 'BR-GROUP', 'BR-GROUP-2014-10Y-FIN-AC04-BANK-Q1', 'AC04', '2014年第一季度银行对账单', '2014', 'Q1', '10Y', '泊冉集团有限公司', '系统', 'archived', NULL, '2014-03-31', 'BANK-2014Q1', 'internal', 'system', '2014-04-01'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2014-10Y-FIN-AC04-BANK-Q1');

-- 销毁申请记录
INSERT INTO biz_destruction (id, applicant_id, applicant_name, reason, archive_count, archive_ids, status, approver_id, approver_name, approval_comment, created_at)
SELECT 'destruction-2024-001', 'user-qianqi', '钱七', '保管期限已满10年，经鉴定无继续保存价值', 3, '["voucher-2014-01-001","voucher-2014-02-001","bank-2014-q1"]', 'PENDING', NULL, NULL, NULL, NOW()
WHERE NOT EXISTS (SELECT 1 FROM biz_destruction WHERE id = 'destruction-2024-001');

-- =====================================================
-- 十一、预归档凭证池数据（待处理凭证）
-- =====================================================
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'pre-2024-12-001', 'BR-GROUP', 'PENDING-2024-12-001', 'AC01', '支付快递费', '2024', '12', '30Y', '泊冉集团有限公司', '张三', 'pending', 680.00, '2024-12-22', 'JZ-PRE-202412-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"顺丰快递费","accsubject":{"code":"6602","name":"管理费用-快递费"},"debit_org":680.00,"credit_org":0},{"id":"2","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":680.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'PENDING-2024-12-001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'pre-2024-12-002', 'BR-GROUP', 'PENDING-2024-12-002', 'AC01', '会议室租赁费', '2024', '12', '30Y', '泊冉集团有限公司', '张三', 'pending', 3500.00, '2024-12-23', 'JZ-PRE-202412-0002', 'internal', 'system', NOW(),
'[{"id":"1","description":"酒店会议室租赁","accsubject":{"code":"6602","name":"管理费用-会议费"},"debit_org":3500.00,"credit_org":0},{"id":"2","description":"现金支付","accsubject":{"code":"1001","name":"库存现金"},"debit_org":0,"credit_org":3500.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'PENDING-2024-12-002');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'pre-2024-12-003', 'BR-GROUP', 'PENDING-2024-12-003', 'AC01', '员工团建活动费', '2024', '12', '30Y', '泊冉集团有限公司', '李四', 'pending', 28000.00, '2024-12-24', 'JZ-PRE-202412-0003', 'internal', 'system', NOW(),
'[{"id":"1","description":"年会团建","accsubject":{"code":"6602","name":"管理费用-福利费"},"debit_org":28000.00,"credit_org":0},{"id":"2","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":28000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'PENDING-2024-12-003');

-- =====================================================
-- 十二、异常数据（跳过：arc_abnormal_voucher 表结构不兼容）
-- =====================================================
-- 注：arc_abnormal_voucher 表使用 request_id/voucher_number 而非 archive_id
-- 此部分数据需要根据实际表结构单独生成

-- =====================================================
-- 十三、库房位置数据
-- =====================================================
INSERT INTO bas_location (id, name, code, type, parent_id, path, capacity, used_count, status, created_at)
SELECT 'loc-warehouse-main', '主档案库房', 'W-MAIN', 'WAREHOUSE', '0', '/主档案库房', 10000, 2850, 'NORMAL', NOW()
WHERE NOT EXISTS (SELECT 1 FROM bas_location WHERE id = 'loc-warehouse-main');

INSERT INTO bas_location (id, name, code, type, parent_id, path, capacity, used_count, status, created_at)
SELECT 'loc-area-a', 'A区-会计凭证区', 'A-VOUCHER', 'AREA', 'loc-warehouse-main', '/主档案库房/A区-会计凭证区', 3000, 1580, 'NORMAL', NOW()
WHERE NOT EXISTS (SELECT 1 FROM bas_location WHERE id = 'loc-area-a');

INSERT INTO bas_location (id, name, code, type, parent_id, path, capacity, used_count, status, created_at)
SELECT 'loc-area-b', 'B区-财务报告区', 'B-REPORT', 'AREA', 'loc-warehouse-main', '/主档案库房/B区-财务报告区', 2000, 680, 'NORMAL', NOW()
WHERE NOT EXISTS (SELECT 1 FROM bas_location WHERE id = 'loc-area-b');

INSERT INTO bas_location (id, name, code, type, parent_id, path, capacity, used_count, status, created_at)
SELECT 'loc-shelf-a1', 'A1号架', 'A1', 'SHELF', 'loc-area-a', '/主档案库房/A区-会计凭证区/A1号架', 500, 486, 'NORMAL', NOW()
WHERE NOT EXISTS (SELECT 1 FROM bas_location WHERE id = 'loc-shelf-a1');

INSERT INTO bas_location (id, name, code, type, parent_id, path, capacity, used_count, status, created_at)
SELECT 'loc-shelf-a2', 'A2号架', 'A2', 'SHELF', 'loc-area-a', '/主档案库房/A区-会计凭证区/A2号架', 500, 320, 'NORMAL', NOW()
WHERE NOT EXISTS (SELECT 1 FROM bas_location WHERE id = 'loc-shelf-a2');

-- =====================================================
-- 十四、四性检测记录
-- =====================================================
-- 四性检测 - 真实哈希值
INSERT INTO audit_inspection_log (id, archive_id, inspection_stage, inspection_time, inspector_id, 
    is_authentic, is_complete, is_available, is_secure, hash_snapshot, check_result, created_at)
SELECT 'inspection-v2024-11-001', 'voucher-2024-11-001', 'ARCHIVE', NOW(), 'user-zhangsan',
    true, true, true, true, '4fe6caa86fdc175a7cb35887ba5e3ee95460250cd00f7c3b84478af3720d696e', 'PASS', NOW()
WHERE NOT EXISTS (SELECT 1 FROM audit_inspection_log WHERE id = 'inspection-v2024-11-001');

INSERT INTO audit_inspection_log (id, archive_id, inspection_stage, inspection_time, inspector_id, 
    is_authentic, is_complete, is_available, is_secure, hash_snapshot, check_result, created_at)
SELECT 'inspection-v2024-11-002', 'voucher-2024-11-002', 'ARCHIVE', NOW(), 'user-zhangsan',
    true, true, true, true, '4c40ce396c10762acfd891c897f986c7646cecc335ced88a7c8d9e10cac44f02', 'PASS', NOW()
WHERE NOT EXISTS (SELECT 1 FROM audit_inspection_log WHERE id = 'inspection-v2024-11-002');

INSERT INTO audit_inspection_log (id, archive_id, inspection_stage, inspection_time, inspector_id, 
    is_authentic, is_complete, is_available, is_secure, hash_snapshot, check_result, created_at)
SELECT 'inspection-v2024-11-003', 'voucher-2024-11-003', 'ARCHIVE', NOW(), 'user-lisi',
    true, true, true, true, 'c9ef496g198dce42c959837gc75d5f9083e98894e1c8249358eg4d627h8hf627', 'PASS', NOW()
WHERE NOT EXISTS (SELECT 1 FROM audit_inspection_log WHERE id = 'inspection-v2024-11-003');

-- =====================================================
-- 十五、附件文件记录 (arc_file_content)
-- 使用 docs/demo数据 目录中的实际PDF文件
-- =====================================================
INSERT INTO arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id)
SELECT 'file-invoice-001', 'BR-GROUP-2024-30Y-FIN-AC01-0002', '差旅费发票_25312000000349611002.pdf', 'PDF', 101601, '4c40ce396c10762acfd891c897f986c7646cecc335ced88a7c8d9e10cac44f02', 'SHA-256', 'uploads/demo/25312000000349611002_ba1d.pdf', NOW(), 'voucher-2024-11-002'
WHERE NOT EXISTS (SELECT 1 FROM arc_file_content WHERE id = 'file-invoice-001');

INSERT INTO arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id)
SELECT 'file-invoice-002', 'BR-GROUP-2024-30Y-FIN-AC01-0001', '办公用品发票_上海市长宁区.pdf', 'PDF', 101657, '4fe6caa86fdc175a7cb35887ba5e3ee95460250cd00f7c3b84478af3720d696e', 'SHA-256', 'uploads/demo/dzfp_25314000000004648601_上海市长宁区吴奕聪餐饮店_20251025012013.pdf', NOW(), 'voucher-2024-11-001'
WHERE NOT EXISTS (SELECT 1 FROM arc_file_content WHERE id = 'file-invoice-002');

INSERT INTO arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id)
SELECT 'file-invoice-003', 'BR-GROUP-2024-30Y-FIN-AC01-0003', '销售发票_上海米山神鸡.pdf', 'PDF', 101613, 'b88176ca3d3dcc0ddd3e9da3cda5c8712ad0c2abde9e6293679dbab5177d562e', 'SHA-256', 'uploads/demo/上海米山神鸡餐饮管理有限公司_发票金额201.00元.pdf', NOW(), 'voucher-2024-11-003'
WHERE NOT EXISTS (SELECT 1 FROM arc_file_content WHERE id = 'file-invoice-003');

-- =====================================================
-- 十六、档案-附件关联 (acc_archive_attachment)
-- 注：表结构使用 file_id 和 attachment_type
-- =====================================================
INSERT INTO acc_archive_attachment (id, archive_id, file_id, attachment_type, relation_desc, created_by, created_at)
SELECT 'attach-link-001', 'voucher-2024-11-002', 'file-invoice-001', 'invoice', '原始发票', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_attachment WHERE id = 'attach-link-001');

INSERT INTO acc_archive_attachment (id, archive_id, file_id, attachment_type, relation_desc, created_by, created_at)
SELECT 'attach-link-002', 'voucher-2024-11-001', 'file-invoice-002', 'invoice', '原始发票', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_attachment WHERE id = 'attach-link-002');

INSERT INTO acc_archive_attachment (id, archive_id, file_id, attachment_type, relation_desc, created_by, created_at)
SELECT 'attach-link-003', 'voucher-2024-11-003', 'file-invoice-003', 'bank_slip', '银行回单', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_attachment WHERE id = 'attach-link-003');

-- =====================================================
-- 十七、更多年度凭证 - 确保全景视图展示丰富
-- =====================================================
-- 2025年2月凭证
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2025-02-001', 'BR-GROUP', 'BR-GROUP-2025-30Y-FIN-AC01-0201', 'AC01', '支付供应商货款', '2025', '02', '30Y', '泊冉集团有限公司', '张三', 'archived', 125600.00, '2025-02-18', 'JZ-202502-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"支付宁波精密零部件货款","accsubject":{"code":"2202","name":"应付账款"},"debit_org":125600.00,"credit_org":0},{"id":"2","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":125600.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2025-30Y-FIN-AC01-0201');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2025-02-002', 'BR-GROUP', 'BR-GROUP-2025-30Y-FIN-AC01-0202', 'AC01', '销售收入确认', '2025', '02', '30Y', '泊冉集团有限公司', '李四', 'archived', 358000.00, '2025-02-20', 'JZ-202502-0002', 'internal', 'system', NOW(),
'[{"id":"1","description":"销售智能检测设备","accsubject":{"code":"1122","name":"应收账款"},"debit_org":404540.00,"credit_org":0},{"id":"2","description":"确认收入","accsubject":{"code":"6001","name":"主营业务收入"},"debit_org":0,"credit_org":358000.00},{"id":"3","description":"销项税额","accsubject":{"code":"2221","name":"应交税费-应交增值税(销项)"},"debit_org":0,"credit_org":46540.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2025-30Y-FIN-AC01-0202');

-- 2023年更多凭证
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2023-09-001', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC01-0091', 'AC01', '季度社保缴纳', '2023', '09', '30Y', '泊冉集团有限公司', '李四', 'archived', 186500.00, '2023-09-25', 'JZ-202309-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"单位社保","accsubject":{"code":"6602","name":"管理费用-社保费"},"debit_org":186500.00,"credit_org":0},{"id":"2","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":186500.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2023-30Y-FIN-AC01-0091');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2023-08-001', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC01-0081', 'AC01', '广告宣传费', '2023', '08', '30Y', '泊冉集团有限公司', '张三', 'archived', 75000.00, '2023-08-15', 'JZ-202308-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"微信朋友圈广告投放","accsubject":{"code":"6601","name":"销售费用-广告费"},"debit_org":66371.68,"credit_org":0},{"id":"2","description":"进项税额","accsubject":{"code":"2221","name":"应交税费-应交增值税(进项)"},"debit_org":8628.32,"credit_org":0},{"id":"3","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":75000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2023-30Y-FIN-AC01-0081');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2023-07-001', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC01-0071', 'AC01', '固定资产折旧', '2023', '07', '30Y', '泊冉集团有限公司', '张三', 'archived', 42800.00, '2023-07-31', 'JZ-202307-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"管理部门折旧","accsubject":{"code":"6602","name":"管理费用-折旧费"},"debit_org":18200.00,"credit_org":0},{"id":"2","description":"生产部门折旧","accsubject":{"code":"5001","name":"生产成本-制造费用"},"debit_org":24600.00,"credit_org":0},{"id":"3","description":"累计折旧","accsubject":{"code":"1602","name":"累计折旧"},"debit_org":0,"credit_org":42800.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2023-30Y-FIN-AC01-0071');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2023-06-001', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC01-0061', 'AC01', '半年度奖金发放', '2023', '06', '30Y', '泊冉集团有限公司', '李四', 'archived', 980000.00, '2023-06-28', 'JZ-202306-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"发放半年度奖金","accsubject":{"code":"2211","name":"应付职工薪酬"},"debit_org":980000.00,"credit_org":0},{"id":"2","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":980000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2023-30Y-FIN-AC01-0061');

-- 2022年更多凭证
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2022-11-001', 'BR-GROUP', 'BR-GROUP-2022-30Y-FIN-AC01-0111', 'AC01', '设备维修费', '2022', '11', '30Y', '泊冉集团有限公司', '张三', 'archived', 35600.00, '2022-11-18', 'JZ-202211-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"数控机床年度维保","accsubject":{"code":"5001","name":"生产成本-制造费用"},"debit_org":31504.42,"credit_org":0},{"id":"2","description":"进项税额","accsubject":{"code":"2221","name":"应交税费-应交增值税(进项)"},"debit_org":4095.58,"credit_org":0},{"id":"3","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":35600.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2022-30Y-FIN-AC01-0111');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2022-10-001', 'BR-GROUP', 'BR-GROUP-2022-30Y-FIN-AC01-0101', 'AC01', '研发材料采购', '2022', '10', '30Y', '泊冉集团有限公司', '李四', 'archived', 168000.00, '2022-10-22', 'JZ-202210-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"研发用电子元器件","accsubject":{"code":"5201","name":"研发支出-费用化支出"},"debit_org":148672.57,"credit_org":0},{"id":"2","description":"进项税额","accsubject":{"code":"2221","name":"应交税费-应交增值税(进项)"},"debit_org":19327.43,"credit_org":0},{"id":"3","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":168000.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2022-30Y-FIN-AC01-0101');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, created_time, custom_metadata)
SELECT 'voucher-2022-09-001', 'BR-GROUP', 'BR-GROUP-2022-30Y-FIN-AC01-0091', 'AC01', '展会参展费用', '2022', '09', '30Y', '泊冉集团有限公司', '张三', 'archived', 128500.00, '2022-09-15', 'JZ-202209-0001', 'internal', 'system', NOW(),
'[{"id":"1","description":"上海工博会展位费","accsubject":{"code":"6601","name":"销售费用-展览费"},"debit_org":113716.81,"credit_org":0},{"id":"2","description":"进项税额","accsubject":{"code":"2221","name":"应交税费-应交增值税(进项)"},"debit_org":14783.19,"credit_org":0},{"id":"3","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":128500.00}]'
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BR-GROUP-2022-30Y-FIN-AC01-0091');

-- =====================================================
-- 十八、系统消息/通知数据
-- =====================================================
-- 如果存在 sys_notification 表，则插入通知数据
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'sys_notification') THEN
        INSERT INTO sys_notification (id, user_id, title, content, type, is_read, created_time)
        SELECT 'notify-001', 'user-zhangsan', '归档任务完成', '2024年11月会计凭证归档已完成，共8笔凭证', 'SUCCESS', false, NOW()
        WHERE NOT EXISTS (SELECT 1 FROM sys_notification WHERE id = 'notify-001');
        
        INSERT INTO sys_notification (id, user_id, title, content, type, is_read, created_time)
        SELECT 'notify-002', 'user-lisi', '借阅申请待审批', '用户 赵六 申请借阅《办公楼租赁合同》，请尽快审批', 'WARNING', false, NOW()
        WHERE NOT EXISTS (SELECT 1 FROM sys_notification WHERE id = 'notify-002');
        
        INSERT INTO sys_notification (id, user_id, title, content, type, is_read, created_time)
        SELECT 'notify-003', 'user-qianqi', '档案到期预警', '有3份2014年档案已满保管期限，请进行鉴定处理', 'WARNING', false, NOW()
        WHERE NOT EXISTS (SELECT 1 FROM sys_notification WHERE id = 'notify-003');
    END IF;
END $$;

-- =====================================================
-- 十九、审计日志样例
-- 表结构使用 action 而非 operation
-- =====================================================
INSERT INTO sys_audit_log (id, user_id, username, action, resource_type, resource_id, operation_result, details, client_ip, mac_address, created_time)
SELECT 'auditlog-001', 'user-zhangsan', 'zhangsan', 'LOGIN', 'USER', 'user-zhangsan', 'SUCCESS', '用户登录系统', '192.168.1.100', 'UNKNOWN', NOW() - INTERVAL '2 hours'
WHERE NOT EXISTS (SELECT 1 FROM sys_audit_log WHERE id = 'auditlog-001');

INSERT INTO sys_audit_log (id, user_id, username, action, resource_type, resource_id, operation_result, details, client_ip, mac_address, created_time)
SELECT 'auditlog-002', 'user-lisi', 'lisi', 'VIEW', 'ARCHIVE', 'voucher-2024-11-008', 'SUCCESS', '查看凭证详情：销售商品确认收入', '192.168.1.102', 'UNKNOWN', NOW() - INTERVAL '1 hour'
WHERE NOT EXISTS (SELECT 1 FROM sys_audit_log WHERE id = 'auditlog-002');

INSERT INTO sys_audit_log (id, user_id, username, action, resource_type, resource_id, operation_result, details, client_ip, mac_address, created_time)
SELECT 'auditlog-003', 'user-wangwu', 'wangwu', 'CREATE', 'BORROWING', 'borrow-001', 'SUCCESS', '提交借阅申请：年度审计核查销售收入确认', '192.168.1.105', 'UNKNOWN', NOW() - INTERVAL '30 minutes'
WHERE NOT EXISTS (SELECT 1 FROM sys_audit_log WHERE id = 'auditlog-003');

-- 完成：丰富演示数据导入脚本
-- 统计: 
--   全宗: 4个
--   用户: 5个
--   会计凭证(AC01): 30+条
--   会计账簿(AC02): 8条
--   财务报告(AC03): 8条
--   其他资料(AC04): 10条
--   案卷: 4个
--   借阅记录: 5条
--   销毁申请: 1条
--   预归档凭证: 3条
--   异常数据: 2条
--   库房位置: 5条
--   四性检测: 3条
--   附件文件: 3个

