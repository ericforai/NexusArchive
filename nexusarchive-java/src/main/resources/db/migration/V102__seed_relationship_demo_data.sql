-- V102: 穿透联查功能 Demo 数据
-- 为穿透联查功能创建全面的、模拟真实业务的 demo 数据
-- 重点覆盖报销场景的完整业务链路

-- ============================================
-- 场景一：差旅费报销完整链路（核心场景）
-- ============================================

-- 1. 出差申请单
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-reimb-sq-001', 'BR-GROUP', 'SQ-2025-01-001', 'AC04', '出差申请单-张三-北京出差', '2025', '01', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2025-01-05', 'SQ-2025-01-001', NULL, '{"applicant": "张三", "destination": "北京", "purpose": "参加技术交流会", "startDate": "2025-01-06", "endDate": "2025-01-09"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- 2. 报销单
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-reimb-bx-001', 'BR-GROUP', 'BX-2025-01-001', 'AC01', '差旅费报销单-张三', '2025', '01', '30Y', '泊冉集团有限公司', '张三', 'archived', 3280.00, '2025-01-10', 'BX-2025-01-001', NULL, '{"applicant": "张三", "totalAmount": 3280.00, "items": [{"type": "交通费", "amount": 553.00}, {"type": "住宿费", "amount": 1200.00}, {"type": "餐饮费", "amount": 450.00}, {"type": "出租车费", "amount": 87.00}]}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- 3. 各类发票（原始凭证）
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-reimb-fp-001', 'BR-GROUP', 'FP-2025-01-001', 'AC01', '高铁票发票-北京南至上海虹桥', '2025', '01', '30Y', '泊冉集团有限公司', '张三', 'archived', 553.00, '2025-01-06', 'FP-2025-01-001', NULL, '{"invoiceType": "交通费", "vendor": "中国铁路", "route": "北京南-上海虹桥"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-reimb-fp-002', 'BR-GROUP', 'FP-2025-01-002', 'AC01', '酒店住宿费发票-北京希尔顿酒店', '2025', '01', '30Y', '泊冉集团有限公司', '张三', 'archived', 1200.00, '2025-01-07', 'FP-2025-01-002', NULL, '{"invoiceType": "住宿费", "vendor": "北京希尔顿酒店", "nights": 3}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-reimb-fp-003', 'BR-GROUP', 'FP-2025-01-003', 'AC01', '餐饮费发票-北京全聚德烤鸭店', '2025', '01', '30Y', '泊冉集团有限公司', '张三', 'archived', 450.00, '2025-01-08', 'FP-2025-01-003', NULL, '{"invoiceType": "餐饮费", "vendor": "北京全聚德烤鸭店"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-reimb-fp-004', 'BR-GROUP', 'FP-2025-01-004', 'AC01', '出租车发票-上海强生出租汽车', '2025', '01', '30Y', '泊冉集团有限公司', '张三', 'archived', 87.00, '2025-01-09', 'FP-2025-01-004', NULL, '{"invoiceType": "出租车费", "vendor": "上海强生出租汽车有限公司"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- 4. 付款单
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-reimb-fk-001', 'BR-GROUP', 'FK-2025-01-001', 'AC01', '付款单-差旅费报销', '2025', '01', '30Y', '泊冉集团有限公司', '财务部', 'archived', 3280.00, '2025-01-12', 'FK-2025-01-001', NULL, '{"payee": "张三", "paymentMethod": "银行转账", "bank": "招商银行"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- 5. 银行回单
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-reimb-hd-001', 'BR-GROUP', 'HD-2025-01-001', 'AC04', '银行回单-招商银行转账', '2025', '01', '30Y', '泊冉集团有限公司', '系统', 'archived', 3280.00, '2025-01-12', 'HD-2025-01-001', NULL, '{"bank": "招商银行", "accountFrom": "6225881234567890", "accountTo": "6225889876543210", "transactionType": "转账"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- 6. 记账凭证
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-reimb-jz-001', 'BR-GROUP', 'JZ-2025-01-001', 'AC01', '记账凭证-差旅费报销', '2025', '01', '30Y', '泊冉集团有限公司', '会计', 'archived', 3280.00, '2025-01-12', 'JZ-2025-01-001', NULL, '[{"id": "1", "debit_org": 3280.00, "accsubject": {"code": "6602", "name": "管理费用-差旅费"}, "credit_org": 0, "description": "差旅费报销"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 3280.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- 7. 月度报表
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-reimb-bb-001', 'BR-GROUP', 'BB-2025-01', 'AC03', '2025年1月科目余额表', '2025', '01', '30Y', '泊冉集团有限公司', '财务部', 'archived', NULL, '2025-01-31', 'BB-2025-01', NULL, '{"reportType": "科目余额表", "period": "2025-01"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- ============================================
-- 场景二：设备采购完整链路
-- ============================================

-- 1. 采购合同
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-purchase-ht-001', 'BR-GROUP', 'HT-2025-02-001', 'AC04', '服务器采购合同-阿里云', '2025', '02', '30Y', '泊冉集团有限公司', '采购部', 'archived', 450000.00, '2025-02-15', 'HT-2025-02-001', NULL, '{"contractType": "采购合同", "vendor": "阿里云", "product": "云服务器ECS", "quantity": 10}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- 2. 采购发票
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-purchase-fp-001', 'BR-GROUP', 'FP-2025-02-001', 'AC01', '服务器采购发票-阿里云', '2025', '02', '30Y', '泊冉集团有限公司', '系统', 'archived', 450000.00, '2025-02-20', 'FP-2025-02-001', NULL, '{"invoiceType": "增值税专用发票", "vendor": "阿里云", "taxRate": 0.13}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- 3. 记账凭证
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-purchase-jz-001', 'BR-GROUP', 'JZ-2025-02-001', 'AC01', '记账凭证-设备采购', '2025', '02', '30Y', '泊冉集团有限公司', '会计', 'archived', 450000.00, '2025-02-20', 'JZ-2025-02-001', NULL, '[{"id": "1", "debit_org": 398230.09, "accsubject": {"code": "1601", "name": "固定资产"}, "credit_org": 0, "description": "服务器采购"}, {"id": "2", "debit_org": 51769.91, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 450000.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- 4. 付款单
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-purchase-fk-001', 'BR-GROUP', 'FK-2025-02-001', 'AC01', '付款单-设备采购款', '2025', '02', '30Y', '泊冉集团有限公司', '财务部', 'archived', 450000.00, '2025-02-22', 'FK-2025-02-001', NULL, '{"payee": "阿里云", "paymentMethod": "银行转账", "bank": "招商银行"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- 5. 银行回单
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-purchase-hd-001', 'BR-GROUP', 'HD-2025-02-001', 'AC04', '银行回单-招商银行转账', '2025', '02', '30Y', '泊冉集团有限公司', '系统', 'archived', 450000.00, '2025-02-22', 'HD-2025-02-001', NULL, '{"bank": "招商银行", "accountFrom": "6225881234567890", "accountTo": "6225881111111111", "transactionType": "转账"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- ============================================
-- 场景三：办公用品采购（简化链路）
-- ============================================

-- 1. 采购发票
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-office-fp-001', 'BR-GROUP', 'FP-2025-03-001', 'AC01', '办公用品采购发票-京东', '2025', '03', '30Y', '泊冉集团有限公司', '系统', 'archived', 2580.00, '2025-03-10', 'FP-2025-03-001', NULL, '{"invoiceType": "电子发票", "vendor": "京东", "items": ["打印纸", "文件夹", "笔"]}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- 2. 记账凭证
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-office-jz-001', 'BR-GROUP', 'JZ-2025-03-001', 'AC01', '记账凭证-办公用品', '2025', '03', '30Y', '泊冉集团有限公司', '会计', 'archived', 2580.00, '2025-03-10', 'JZ-2025-03-001', NULL, '[{"id": "1", "debit_org": 2283.19, "accsubject": {"code": "6602", "name": "管理费用-办公费"}, "credit_org": 0, "description": "办公用品"}, {"id": "2", "debit_org": 296.81, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 2580.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- 3. 银行回单
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-office-hd-001', 'BR-GROUP', 'HD-2025-03-001', 'AC04', '银行回单-工商银行转账', '2025', '03', '30Y', '泊冉集团有限公司', '系统', 'archived', 2580.00, '2025-03-10', 'HD-2025-03-001', NULL, '{"bank": "工商银行", "accountFrom": "6225881234567890", "accountTo": "6225882222222222", "transactionType": "转账"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- ============================================
-- 场景四：服务费支付（多发票场景）
-- ============================================

-- 1. 服务合同
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-service-ht-001', 'BR-GROUP', 'HT-2025-04-001', 'AC04', '年度审计服务合同', '2025', '04', '30Y', '泊冉集团有限公司', '财务部', 'archived', 120000.00, '2025-04-01', 'HT-2025-04-001', NULL, '{"contractType": "服务合同", "vendor": "XX会计师事务所", "serviceType": "年度审计", "period": "2025年度"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- 2. 分期发票（3张）
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-service-fp-001', 'BR-GROUP', 'FP-2025-04-001', 'AC01', '审计服务费发票-Q1', '2025', '04', '30Y', '泊冉集团有限公司', '系统', 'archived', 30000.00, '2025-04-05', 'FP-2025-04-001', NULL, '{"invoiceType": "增值税专用发票", "vendor": "XX会计师事务所", "period": "Q1"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-service-fp-002', 'BR-GROUP', 'FP-2025-05-001', 'AC01', '审计服务费发票-Q2', '2025', '05', '30Y', '泊冉集团有限公司', '系统', 'archived', 30000.00, '2025-05-05', 'FP-2025-05-001', NULL, '{"invoiceType": "增值税专用发票", "vendor": "XX会计师事务所", "period": "Q2"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-service-fp-003', 'BR-GROUP', 'FP-2025-06-001', 'AC01', '审计服务费发票-Q3', '2025', '06', '30Y', '泊冉集团有限公司', '系统', 'archived', 30000.00, '2025-06-05', 'FP-2025-06-001', NULL, '{"invoiceType": "增值税专用发票", "vendor": "XX会计师事务所", "period": "Q3"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- 3. 记账凭证（汇总3张发票）
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-service-jz-001', 'BR-GROUP', 'JZ-2025-06-001', 'AC01', '记账凭证-审计服务费', '2025', '06', '30Y', '泊冉集团有限公司', '会计', 'archived', 90000.00, '2025-06-10', 'JZ-2025-06-001', NULL, '[{"id": "1", "debit_org": 79646.02, "accsubject": {"code": "6602", "name": "管理费用-审计费"}, "credit_org": 0, "description": "审计服务费"}, {"id": "2", "debit_org": 10353.98, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 90000.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- 4. 银行回单
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) 
VALUES ('demo-service-hd-001', 'BR-GROUP', 'HD-2025-06-001', 'AC04', '银行回单-建设银行转账', '2025', '06', '30Y', '泊冉集团有限公司', '系统', 'archived', 90000.00, '2025-06-10', 'HD-2025-06-001', NULL, '{"bank": "建设银行", "accountFrom": "6225881234567890", "accountTo": "6225883333333333", "transactionType": "转账"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, NOW(), NOW(), 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);

-- ============================================
-- 关系数据（acc_archive_relation）
-- ============================================

-- 场景一：差旅费报销完整链路关系
-- 申请单 → 报销单
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-001', 'demo-reimb-sq-001', 'demo-reimb-bx-001', 'BASIS', '出差申请依据', 'system', NOW(), 0);

-- 报销单 ← 各类发票（原始凭证）
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-002', 'demo-reimb-fp-001', 'demo-reimb-bx-001', 'ORIGINAL_VOUCHER', '交通费原始凭证', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-003', 'demo-reimb-fp-002', 'demo-reimb-bx-001', 'ORIGINAL_VOUCHER', '住宿费原始凭证', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-004', 'demo-reimb-fp-003', 'demo-reimb-bx-001', 'ORIGINAL_VOUCHER', '餐饮费原始凭证', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-005', 'demo-reimb-fp-004', 'demo-reimb-bx-001', 'ORIGINAL_VOUCHER', '出租车费原始凭证', 'system', NOW(), 0);

-- 报销单 → 付款单 → 银行回单（资金流）
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-006', 'demo-reimb-bx-001', 'demo-reimb-fk-001', 'CASH_FLOW', '报销付款', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-007', 'demo-reimb-fk-001', 'demo-reimb-hd-001', 'CASH_FLOW', '银行转账', 'system', NOW(), 0);

-- 付款单 → 记账凭证 → 报表（归档）
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-008', 'demo-reimb-fk-001', 'demo-reimb-jz-001', 'ARCHIVE', '凭证归档', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-009', 'demo-reimb-jz-001', 'demo-reimb-bb-001', 'ARCHIVE', '报表归档', 'system', NOW(), 0);

-- 补充：发票 → 记账凭证（原始凭证关系，用于穿透联查）
-- 这样以凭证为中心查询时，可以同时看到所有发票
-- 注意：这些关系使得以凭证为中心时可以展示完整的业务链路
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-030', 'demo-reimb-fp-001', 'demo-reimb-jz-001', 'ORIGINAL_VOUCHER', '交通费原始凭证', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-031', 'demo-reimb-fp-002', 'demo-reimb-jz-001', 'ORIGINAL_VOUCHER', '住宿费原始凭证', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-032', 'demo-reimb-fp-003', 'demo-reimb-jz-001', 'ORIGINAL_VOUCHER', '餐饮费原始凭证', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-033', 'demo-reimb-fp-004', 'demo-reimb-jz-001', 'ORIGINAL_VOUCHER', '出租车费原始凭证', 'system', NOW(), 0);

-- 补充：报销单 → 记账凭证（业务依据关系）
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-034', 'demo-reimb-bx-001', 'demo-reimb-jz-001', 'BASIS', '报销单依据', 'system', NOW(), 0);

-- 补充：申请单 → 记账凭证（业务依据关系）
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-035', 'demo-reimb-sq-001', 'demo-reimb-jz-001', 'BASIS', '出差申请依据', 'system', NOW(), 0);

-- 场景二：设备采购完整链路关系
-- 合同 → 发票 → 凭证 → 付款单 → 银行回单
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-010', 'demo-purchase-ht-001', 'demo-purchase-fp-001', 'BASIS', '合同依据', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-011', 'demo-purchase-fp-001', 'demo-purchase-jz-001', 'ORIGINAL_VOUCHER', '原始凭证', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-012', 'demo-purchase-jz-001', 'demo-purchase-fk-001', 'CASH_FLOW', '资金流', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-013', 'demo-purchase-fk-001', 'demo-purchase-hd-001', 'CASH_FLOW', '银行转账', 'system', NOW(), 0);

-- 场景三：办公用品采购关系
-- 发票 → 凭证 → 银行回单
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-014', 'demo-office-fp-001', 'demo-office-jz-001', 'ORIGINAL_VOUCHER', '原始凭证', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-015', 'demo-office-jz-001', 'demo-office-hd-001', 'CASH_FLOW', '银行转账', 'system', NOW(), 0);

-- 场景四：服务费支付关系
-- 合同 → 发票（3张）→ 凭证 → 银行回单
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-016', 'demo-service-ht-001', 'demo-service-fp-001', 'BASIS', '合同依据', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-017', 'demo-service-ht-001', 'demo-service-fp-002', 'BASIS', '合同依据', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-018', 'demo-service-ht-001', 'demo-service-fp-003', 'BASIS', '合同依据', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-019', 'demo-service-fp-001', 'demo-service-jz-001', 'ORIGINAL_VOUCHER', '原始凭证', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-020', 'demo-service-fp-002', 'demo-service-jz-001', 'ORIGINAL_VOUCHER', '原始凭证', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-021', 'demo-service-fp-003', 'demo-service-jz-001', 'ORIGINAL_VOUCHER', '原始凭证', 'system', NOW(), 0);

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) 
VALUES ('demo-rel-022', 'demo-service-jz-001', 'demo-service-hd-001', 'CASH_FLOW', '银行转账', 'system', NOW(), 0);
