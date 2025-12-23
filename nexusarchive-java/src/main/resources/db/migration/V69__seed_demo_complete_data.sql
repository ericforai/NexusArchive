-- Input: Flyway 迁移引擎
-- Output: 完整演示数据初始化
-- Pos: 演示环境数据种子脚本

-- =====================================================
-- V69: 泊冉集团演示数据
-- =====================================================

-- ==================== 1. 基础数据 ====================

-- 1.1 全宗数据
INSERT INTO bas_fonds (id, fonds_code, fonds_name, company_name, description, created_by)
SELECT 'fonds-boran-001', 'BRJT', '泊冉集团总部', '泊冉集团有限公司', '集团总部财务档案全宗', 'system'
WHERE NOT EXISTS (SELECT 1 FROM bas_fonds WHERE fonds_code = 'BRJT');

INSERT INTO bas_fonds (id, fonds_code, fonds_name, company_name, description, created_by)
SELECT 'fonds-boran-002', 'BRKJ', '泊冉科技', '泊冉科技股份有限公司', '科技子公司财务档案全宗', 'system'
WHERE NOT EXISTS (SELECT 1 FROM bas_fonds WHERE fonds_code = 'BRKJ');

INSERT INTO bas_fonds (id, fonds_code, fonds_name, company_name, description, created_by)
SELECT 'fonds-boran-003', 'BRWL', '泊冉物流', '泊冉物流有限公司', '物流子公司财务档案全宗', 'system'
WHERE NOT EXISTS (SELECT 1 FROM bas_fonds WHERE fonds_code = 'BRWL');

-- 1.2 组织机构
INSERT INTO sys_org (id, name, code, parent_id, type, order_num, deleted)
SELECT 'org-brjt', '泊冉集团', 'BRJT', NULL, 'company', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_org WHERE code = 'BRJT');

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, deleted)
SELECT 'org-brjt-fin', '财务部', 'BRJT-FIN', 'org-brjt', 'department', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_org WHERE code = 'BRJT-FIN');

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, deleted)
SELECT 'org-brjt-arc', '档案室', 'BRJT-ARC', 'org-brjt', 'department', 2, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_org WHERE code = 'BRJT-ARC');

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, deleted)
SELECT 'org-brjt-audit', '审计部', 'BRJT-AUDIT', 'org-brjt', 'department', 3, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_org WHERE code = 'BRJT-AUDIT');

-- 1.3 存储位置
INSERT INTO bas_location (id, name, code, type, parent_id, deleted)
SELECT 'loc-a', 'A区档案库', 'A', 'zone', NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM bas_location WHERE code = 'A');

INSERT INTO bas_location (id, name, code, type, parent_id, deleted)
SELECT 'loc-a01', 'A-01密集架', 'A-01', 'cabinet', 'loc-a', 0
WHERE NOT EXISTS (SELECT 1 FROM bas_location WHERE code = 'A-01');

INSERT INTO bas_location (id, name, code, type, parent_id, deleted)
SELECT 'loc-b', 'B区档案库', 'B', 'zone', NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM bas_location WHERE code = 'B');

-- ==================== 2. 档案核心数据 ====================

-- 2.1 案卷数据
INSERT INTO acc_archive_volume (id, volume_code, title, fonds_no, fiscal_year, fiscal_period, category_code, file_count, retention_period, status, deleted)
SELECT 'vol-2024-01', 'BRJT-AC01-2024-V001', '2024年1月记账凭证', 'BRJT', '2024', '01', 'AC01', 35, '30Y', 'archived', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_volume WHERE volume_code = 'BRJT-AC01-2024-V001');

INSERT INTO acc_archive_volume (id, volume_code, title, fonds_no, fiscal_year, fiscal_period, category_code, file_count, retention_period, status, deleted)
SELECT 'vol-2024-02', 'BRJT-AC01-2024-V002', '2024年2月记账凭证', 'BRJT', '2024', '02', 'AC01', 42, '30Y', 'archived', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_volume WHERE volume_code = 'BRJT-AC01-2024-V002');

INSERT INTO acc_archive_volume (id, volume_code, title, fonds_no, fiscal_year, fiscal_period, category_code, file_count, retention_period, status, deleted)
SELECT 'vol-2024-03', 'BRJT-AC01-2024-V003', '2024年3月记账凭证', 'BRJT', '2024', '03', 'AC01', 38, '30Y', 'pending', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_volume WHERE volume_code = 'BRJT-AC01-2024-V003');

INSERT INTO acc_archive_volume (id, volume_code, title, fonds_no, fiscal_year, fiscal_period, category_code, file_count, retention_period, status, deleted)
SELECT 'vol-2024-q1-report', 'BRJT-AC03-2024-R001', '2024年第一季度财务报告', 'BRJT', '2024', 'Q1', 'AC03', 3, 'PERM', 'archived', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_volume WHERE volume_code = 'BRJT-AC03-2024-R001');

INSERT INTO acc_archive_volume (id, volume_code, title, fonds_no, fiscal_year, fiscal_period, category_code, file_count, retention_period, status, deleted)
SELECT 'vol-2023-ledger', 'BRJT-AC02-2023-L001', '2023年度总账', 'BRJT', '2023', '全年', 'AC02', 1, '30Y', 'archived', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_volume WHERE volume_code = 'BRJT-AC02-2023-L001');

-- 2.2 归档档案 - 记账凭证类 (AC01)
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, volume_id, created_by, deleted)
SELECT 'arc-2024-001', 'BRJT', 'BRJT-2024-30Y-FIN-AC01-0001', 'AC01', '付款凭证-上海米山神鸡餐饮管理有限公司', '2024', '01', '30Y', '泊冉集团', '刘芳', 'archived', 201.00, '2024-01-15', 'JZ-202401-0001', 'internal', 'vol-2024-01', 'user-liufang', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BRJT-2024-30Y-FIN-AC01-0001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, volume_id, created_by, deleted)
SELECT 'arc-2024-002', 'BRJT', 'BRJT-2024-30Y-FIN-AC01-0002', 'AC01', '付款凭证-上海市长宁区吴奕聪餐饮店', '2024', '01', '30Y', '泊冉集团', '刘芳', 'archived', 156.00, '2024-01-18', 'JZ-202401-0002', 'internal', 'vol-2024-01', 'user-liufang', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BRJT-2024-30Y-FIN-AC01-0002');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, volume_id, created_by, deleted)
SELECT 'arc-2024-003', 'BRJT', 'BRJT-2024-30Y-FIN-AC01-0003', 'AC01', '收款凭证-软件开发服务收入', '2024', '01', '30Y', '泊冉集团', '刘芳', 'archived', 35600.00, '2024-01-20', 'JZ-202401-0003', 'internal', 'vol-2024-01', 'user-liufang', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BRJT-2024-30Y-FIN-AC01-0003');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, volume_id, created_by, deleted)
SELECT 'arc-2024-004', 'BRJT', 'BRJT-2024-30Y-FIN-AC01-0004', 'AC01', '转账凭证-员工差旅费报销', '2024', '01', '30Y', '泊冉集团', '刘芳', 'archived', 3280.00, '2024-01-22', 'JZ-202401-0004', 'internal', 'vol-2024-01', 'user-liufang', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BRJT-2024-30Y-FIN-AC01-0004');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, volume_id, created_by, deleted)
SELECT 'arc-2024-005', 'BRJT', 'BRJT-2024-30Y-FIN-AC01-0005', 'AC01', '付款凭证-阿里云服务器费用', '2024', '02', '30Y', '泊冉集团', '刘芳', 'archived', 12800.00, '2024-02-05', 'JZ-202402-0001', 'internal', 'vol-2024-02', 'user-liufang', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BRJT-2024-30Y-FIN-AC01-0005');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, volume_id, created_by, deleted)
SELECT 'arc-2024-006', 'BRJT', 'BRJT-2024-30Y-FIN-AC01-0006', 'AC01', '付款凭证-办公用品采购', '2024', '02', '30Y', '泊冉集团', '刘芳', 'archived', 2350.00, '2024-02-08', 'JZ-202402-0002', 'internal', 'vol-2024-02', 'user-liufang', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BRJT-2024-30Y-FIN-AC01-0006');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, volume_id, created_by, deleted)
SELECT 'arc-2024-007', 'BRJT', 'BRJT-2024-30Y-FIN-AC01-0007', 'AC01', '收款凭证-咨询服务收入', '2024', '02', '30Y', '泊冉集团', '刘芳', 'archived', 18500.00, '2024-02-15', 'JZ-202402-0003', 'internal', 'vol-2024-02', 'user-liufang', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BRJT-2024-30Y-FIN-AC01-0007');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, volume_id, created_by, deleted)
SELECT 'arc-2024-008', 'BRJT', 'BRJT-2024-30Y-FIN-AC01-0008', 'AC01', '付款凭证-团队聚餐费用', '2024', '03', '30Y', '泊冉集团', '刘芳', 'pending', 1580.00, '2024-03-05', 'JZ-202403-0001', 'internal', 'vol-2024-03', 'user-liufang', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BRJT-2024-30Y-FIN-AC01-0008');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, volume_id, created_by, deleted)
SELECT 'arc-2024-009', 'BRJT', 'BRJT-2024-30Y-FIN-AC01-0009', 'AC01', '付款凭证-快递物流费用', '2024', '03', '30Y', '泊冉集团', '刘芳', 'pending', 680.00, '2024-03-08', 'JZ-202403-0002', 'internal', 'vol-2024-03', 'user-liufang', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BRJT-2024-30Y-FIN-AC01-0009');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, deleted)
SELECT 'arc-2024-010', 'BRJT', 'BRJT-2024-30Y-FIN-AC01-0010', 'AC01', '收款凭证-技术服务收入', '2024', '03', '30Y', '泊冉集团', '刘芳', 'draft', 45000.00, '2024-03-15', 'JZ-202403-0003', 'internal', 'user-liufang', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BRJT-2024-30Y-FIN-AC01-0010');

-- 2.3 归档档案 - 合同类 (AC04)
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, deleted)
SELECT 'arc-2024-c01', 'BRJT', 'BRJT-2024-30Y-FIN-AC04-0001', 'AC04', '年度技术服务协议-华为云', '2024', '01', '30Y', '泊冉集团', '张伟', 'archived', 58000.00, '2024-01-10', 'CON-202401-001', 'internal', 'user-zhangwei', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BRJT-2024-30Y-FIN-AC04-0001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, security_level, created_by, deleted)
SELECT 'arc-2024-c02', 'BRJT', 'BRJT-2024-30Y-FIN-AC04-0002', 'AC04', '办公室租赁合同', '2024', '01', '30Y', '泊冉集团', '张伟', 'archived', 36000.00, '2024-01-05', 'CON-202401-002', 'internal', 'user-zhangwei', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BRJT-2024-30Y-FIN-AC04-0002');

-- 2.4 归档档案 - 财务报告类 (AC03)  
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, doc_date, unique_biz_id, security_level, created_by, deleted)
SELECT 'arc-2023-r01', 'BRJT', 'BRJT-2023-PERM-FIN-AC03-0001', 'AC03', '2023年度财务决算报告', '2023', '全年', 'PERM', '泊冉集团', '刘芳', 'archived', '2024-03-31', 'REP-2023-FINAL', 'secret', 'user-liufang', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BRJT-2023-PERM-FIN-AC03-0001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, doc_date, unique_biz_id, security_level, volume_id, created_by, deleted)
SELECT 'arc-2024-r01', 'BRJT', 'BRJT-2024-PERM-FIN-AC03-0001', 'AC03', '2024年第一季度财务报告', '2024', 'Q1', 'PERM', '泊冉集团', '刘芳', 'archived', '2024-04-15', 'REP-2024-Q1', 'internal', 'vol-2024-q1-report', 'user-liufang', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BRJT-2024-PERM-FIN-AC03-0001');

-- 2.5 归档档案 - 会计账簿类 (AC02)
INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, doc_date, unique_biz_id, security_level, volume_id, created_by, deleted)
SELECT 'arc-2023-l01', 'BRJT', 'BRJT-2023-30Y-FIN-AC02-0001', 'AC02', '2023年度总账', '2023', '全年', '30Y', '泊冉集团', '刘芳', 'archived', '2023-12-31', 'LED-2023-MAIN', 'internal', 'vol-2023-ledger', 'user-liufang', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BRJT-2023-30Y-FIN-AC02-0001');

INSERT INTO acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, doc_date, unique_biz_id, security_level, created_by, deleted)
SELECT 'arc-2023-l02', 'BRJT', 'BRJT-2023-30Y-FIN-AC02-0002', 'AC02', '2023年现金日记账', '2023', '全年', '30Y', '泊冉集团', '刘芳', 'archived', '2023-12-31', 'LED-2023-CASH', 'internal', 'user-liufang', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive WHERE archive_code = 'BRJT-2023-30Y-FIN-AC02-0002');

-- 2.6 档案关联关系
INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, deleted)
SELECT 'rel-001', 'arc-2024-001', 'arc-2024-c01', 'BASIS', '合同依据', 'user-zhangwei', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_relation WHERE id = 'rel-001')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'arc-2024-001')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'arc-2024-c01');

INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, deleted)
SELECT 'rel-002', 'arc-2024-003', 'arc-2024-c01', 'CASH_FLOW', '款项往来', 'user-liufang', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_relation WHERE id = 'rel-002')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'arc-2024-003')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'arc-2024-c01');

INSERT INTO acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, deleted)
SELECT 'rel-003', 'arc-2024-001', 'arc-2024-r01', 'ARCHIVE', '归档至季度报告', 'user-liufang', 0
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_relation WHERE id = 'rel-003')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'arc-2024-001')
  AND EXISTS (SELECT 1 FROM acc_archive WHERE id = 'arc-2024-r01');

-- ==================== 3. 业务流程数据 ====================

-- 3.1 借阅记录
INSERT INTO biz_borrowing (id, archive_id, archive_title, user_id, user_name, reason, borrow_date, expected_return_date, actual_return_date, status, approval_comment, deleted)
SELECT 'bor-001', 'arc-2023-r01', '2023年度财务决算报告', 'user-wangqiang', '王强', '年度审计工作', '2024-04-10', '2024-04-20', '2024-04-18', 'RETURNED', '同意借阅', 0
WHERE NOT EXISTS (SELECT 1 FROM biz_borrowing WHERE id = 'bor-001');

INSERT INTO biz_borrowing (id, archive_id, archive_title, user_id, user_name, reason, borrow_date, expected_return_date, status, approval_comment, deleted)
SELECT 'bor-002', 'arc-2024-c01', '年度技术服务协议-华为云', 'user-liufang', '刘芳', '合同续签对账', '2024-05-15', '2024-05-25', 'APPROVED', '同意', 0
WHERE NOT EXISTS (SELECT 1 FROM biz_borrowing WHERE id = 'bor-002');

INSERT INTO biz_borrowing (id, archive_id, archive_title, user_id, user_name, reason, borrow_date, expected_return_date, status, deleted)
SELECT 'bor-003', 'arc-2024-001', '付款凭证-上海米山神鸡餐饮管理有限公司', 'user-wangqiang', '王强', '费用核查', '2024-06-01', '2024-06-10', 'PENDING', 0
WHERE NOT EXISTS (SELECT 1 FROM biz_borrowing WHERE id = 'bor-003');

INSERT INTO biz_borrowing (id, archive_id, archive_title, user_id, user_name, reason, borrow_date, expected_return_date, status, approval_comment, deleted)
SELECT 'bor-004', 'arc-2023-l01', '2023年度总账', 'user-wangqiang', '王强', '年度审计复核', '2024-03-20', '2024-04-05', 'REJECTED', '请说明具体审计事项', 0
WHERE NOT EXISTS (SELECT 1 FROM biz_borrowing WHERE id = 'bor-004');


-- 3.2 归档审批
INSERT INTO biz_archive_approval (id, archive_id, archive_code, archive_title, applicant_id, applicant_name, application_reason, status, approver_id, approver_name, approval_comment, deleted)
SELECT 'appr-001', 'arc-2024-001', 'BRJT-2024-30Y-FIN-AC01-0001', '付款凭证-上海米山神鸡餐饮管理有限公司', 'user-liufang', '刘芳', '凭证整理完成，申请归档', 'APPROVED', 'user-zhangwei', '张伟', '符合归档条件', 0
WHERE NOT EXISTS (SELECT 1 FROM biz_archive_approval WHERE id = 'appr-001');

INSERT INTO biz_archive_approval (id, archive_id, archive_code, archive_title, applicant_id, applicant_name, application_reason, status, approver_id, approver_name, approval_comment, deleted)
SELECT 'appr-002', 'arc-2024-002', 'BRJT-2024-30Y-FIN-AC01-0002', '付款凭证-上海市长宁区吴奕聪餐饮店', 'user-liufang', '刘芳', '凭证整理完成，申请归档', 'APPROVED', 'user-zhangwei', '张伟', '通过', 0
WHERE NOT EXISTS (SELECT 1 FROM biz_archive_approval WHERE id = 'appr-002');

INSERT INTO biz_archive_approval (id, archive_id, archive_code, archive_title, applicant_id, applicant_name, application_reason, status, deleted)
SELECT 'appr-003', 'arc-2024-008', 'BRJT-2024-30Y-FIN-AC01-0008', '付款凭证-团队聚餐费用', 'user-liufang', '刘芳', '凭证整理完成，申请归档', 'PENDING', 0
WHERE NOT EXISTS (SELECT 1 FROM biz_archive_approval WHERE id = 'appr-003');

INSERT INTO biz_archive_approval (id, archive_id, archive_code, archive_title, applicant_id, applicant_name, application_reason, status, deleted)
SELECT 'appr-004', 'arc-2024-009', 'BRJT-2024-30Y-FIN-AC01-0009', '付款凭证-快递物流费用', 'user-liufang', '刘芳', '凭证整理完成，申请归档', 'PENDING', 0
WHERE NOT EXISTS (SELECT 1 FROM biz_archive_approval WHERE id = 'appr-004');

INSERT INTO biz_archive_approval (id, archive_id, archive_code, archive_title, applicant_id, applicant_name, application_reason, status, approver_id, approver_name, approval_comment, deleted)
SELECT 'appr-005', 'arc-2024-010', 'BRJT-2024-30Y-FIN-AC01-0010', '收款凭证-技术服务收入', 'user-liufang', '刘芳', '凭证整理完成，申请归档', 'REJECTED', 'user-zhangwei', '张伟', '缺少合同附件，请补充', 0
WHERE NOT EXISTS (SELECT 1 FROM biz_archive_approval WHERE id = 'appr-005');

-- 3.3 开放鉴定
INSERT INTO biz_open_appraisal (id, archive_id, archive_code, archive_title, retention_period, current_security_level, status, appraisal_result, appraiser_id, appraiser_name, appraisal_date, open_level, reason, deleted)
SELECT 'oa-001', 'arc-2023-r01', 'BRJT-2023-PERM-FIN-AC03-0001', '2023年度财务决算报告', 'PERM', 'secret', 'COMPLETED', 'RESTRICTED', 'user-zhangwei', '张伟', '2024-04-01', 'RESTRICTED', '涉及敏感财务数据，继续限制访问', 0
WHERE NOT EXISTS (SELECT 1 FROM biz_open_appraisal WHERE id = 'oa-001');

INSERT INTO biz_open_appraisal (id, archive_id, archive_code, archive_title, retention_period, current_security_level, status, deleted)
SELECT 'oa-002', 'arc-2024-r01', 'BRJT-2024-PERM-FIN-AC03-0001', '2024年第一季度财务报告', 'PERM', 'internal', 'PENDING', 0
WHERE NOT EXISTS (SELECT 1 FROM biz_open_appraisal WHERE id = 'oa-002');

INSERT INTO biz_open_appraisal (id, archive_id, archive_code, archive_title, retention_period, current_security_level, status, appraisal_result, appraiser_id, appraiser_name, appraisal_date, open_level, reason, deleted)
SELECT 'oa-003', 'arc-2023-l01', 'BRJT-2023-30Y-FIN-AC02-0001', '2023年度总账', '30Y', 'internal', 'COMPLETED', 'OPEN', 'user-zhangwei', '张伟', '2024-05-15', 'PUBLIC', '保管期限已过10年，可公开查阅', 0
WHERE NOT EXISTS (SELECT 1 FROM biz_open_appraisal WHERE id = 'oa-003');

-- 3.4 销毁管理
INSERT INTO biz_destruction (id, applicant_id, applicant_name, reason, archive_count, archive_ids, status, deleted)
SELECT 'dest-001', 'user-zhangwei', '张伟', '档案已超过法定保管期限，申请销毁', 2, 'arc-2024-001,arc-2024-002', 'PENDING', 0
WHERE NOT EXISTS (SELECT 1 FROM biz_destruction WHERE id = 'dest-001');

INSERT INTO biz_destruction (id, applicant_id, applicant_name, reason, archive_count, archive_ids, status, approver_id, approver_name, approval_comment, deleted)
SELECT 'dest-002', 'user-zhangwei', '张伟', '临时文件已完成归档，可销毁原始副本', 1, 'arc-2024-004', 'APPROVED', 'user_admin_001', '系统管理员', '同意销毁', 0
WHERE NOT EXISTS (SELECT 1 FROM biz_destruction WHERE id = 'dest-002');

INSERT INTO biz_destruction (id, applicant_id, applicant_name, reason, archive_count, archive_ids, status, approver_id, approver_name, approval_comment, deleted)
SELECT 'dest-003', 'user-zhangwei', '张伟', '重复归档的凭证副本，需要清理', 3, 'arc-2024-005,arc-2024-006,arc-2024-007', 'REJECTED', 'user_admin_001', '系统管理员', '请先确认原件完整性', 0
WHERE NOT EXISTS (SELECT 1 FROM biz_destruction WHERE id = 'dest-003');

-- ==================== 4. 技术日志数据 ====================

-- 4.1 巡检日志 (四性检测记录)
INSERT INTO audit_inspection_log (id, archive_id, inspection_stage, inspection_time, inspector_id, is_authentic, is_complete, is_available, is_secure, check_result, is_compliant)
SELECT 'insp-001', 'arc-2024-001', 'ARCHIVE', '2024-01-16 03:00:00', 'SYSTEM', true, true, true, true, 'PASS', true
WHERE NOT EXISTS (SELECT 1 FROM audit_inspection_log WHERE id = 'insp-001');

INSERT INTO audit_inspection_log (id, archive_id, inspection_stage, inspection_time, inspector_id, is_authentic, is_complete, is_available, is_secure, check_result, is_compliant)
SELECT 'insp-002', 'arc-2024-002', 'ARCHIVE', '2024-01-19 03:00:00', 'SYSTEM', true, true, true, true, 'PASS', true
WHERE NOT EXISTS (SELECT 1 FROM audit_inspection_log WHERE id = 'insp-002');

INSERT INTO audit_inspection_log (id, archive_id, inspection_stage, inspection_time, inspector_id, is_authentic, is_complete, is_available, is_secure, check_result, is_compliant)
SELECT 'insp-003', 'arc-2024-003', 'ARCHIVE', '2024-01-21 03:00:00', 'SYSTEM', true, true, true, true, 'PASS', true
WHERE NOT EXISTS (SELECT 1 FROM audit_inspection_log WHERE id = 'insp-003');

INSERT INTO audit_inspection_log (id, archive_id, inspection_stage, inspection_time, inspector_id, is_authentic, is_complete, is_available, is_secure, check_result, is_compliant, compliance_violations)
SELECT 'insp-004', 'arc-2024-008', 'PRE_ARCHIVE', '2024-03-06 03:00:00', 'SYSTEM', true, false, true, true, 'FAIL', false, '元数据不完整：缺少附件清单'
WHERE NOT EXISTS (SELECT 1 FROM audit_inspection_log WHERE id = 'insp-004');

INSERT INTO audit_inspection_log (id, archive_id, inspection_stage, inspection_time, inspector_id, is_authentic, is_complete, is_available, is_secure, check_result, is_compliant, compliance_warnings)
SELECT 'insp-005', 'arc-2024-c01', 'ARCHIVE', '2024-01-12 03:00:00', 'SYSTEM', true, true, true, true, 'PASS', true, '建议补充合同附件扫描件'
WHERE NOT EXISTS (SELECT 1 FROM audit_inspection_log WHERE id = 'insp-005');

INSERT INTO audit_inspection_log (id, archive_id, inspection_stage, inspection_time, inspector_id, is_authentic, is_complete, is_available, is_secure, check_result, is_compliant)
SELECT 'insp-006', 'arc-2023-r01', 'PERIODIC', '2024-06-01 03:00:00', 'SYSTEM', true, true, true, true, 'PASS', true
WHERE NOT EXISTS (SELECT 1 FROM audit_inspection_log WHERE id = 'insp-006');


-- 4.2 格式转换日志
INSERT INTO arc_convert_log (id, archive_id, source_format, target_format, source_path, target_path, status, duration_ms)
SELECT 'conv-001', 'arc-2024-001', 'PDF', 'OFD', '/uploads/2024/01/invoice_001.pdf', '/archives/2024/01/invoice_001.ofd', 'SUCCESS', 1250
WHERE NOT EXISTS (SELECT 1 FROM arc_convert_log WHERE id = 'conv-001');

INSERT INTO arc_convert_log (id, archive_id, source_format, target_format, source_path, target_path, status, duration_ms)
SELECT 'conv-002', 'arc-2024-002', 'PDF', 'OFD', '/uploads/2024/01/invoice_002.pdf', '/archives/2024/01/invoice_002.ofd', 'SUCCESS', 980
WHERE NOT EXISTS (SELECT 1 FROM arc_convert_log WHERE id = 'conv-002');

INSERT INTO arc_convert_log (id, archive_id, source_format, target_format, source_path, status, error_message, duration_ms)
SELECT 'conv-003', 'arc-2024-008', 'PDF', 'OFD', '/uploads/2024/03/receipt_001.pdf', 'FAILED', 'PDF文件损坏无法解析', 350
WHERE NOT EXISTS (SELECT 1 FROM arc_convert_log WHERE id = 'conv-003');

-- ==================== 5. ERP集成数据 ====================

-- 5.1 同步状态记录
INSERT INTO sys_ingest_request_status (request_id, status, message)
SELECT 'req-2024-001', 'SUCCESS', '用友YonSuite凭证同步成功，共15条'
WHERE NOT EXISTS (SELECT 1 FROM sys_ingest_request_status WHERE request_id = 'req-2024-001');

INSERT INTO sys_ingest_request_status (request_id, status, message)
SELECT 'req-2024-002', 'SUCCESS', '用友YonSuite凭证同步成功，共22条'
WHERE NOT EXISTS (SELECT 1 FROM sys_ingest_request_status WHERE request_id = 'req-2024-002');

INSERT INTO sys_ingest_request_status (request_id, status, message)
SELECT 'req-2024-003', 'PROCESSING', '正在同步用友YonSuite凭证...'
WHERE NOT EXISTS (SELECT 1 FROM sys_ingest_request_status WHERE request_id = 'req-2024-003');

INSERT INTO sys_ingest_request_status (request_id, status, message)
SELECT 'req-2024-004', 'FAILED', 'ERP连接超时，请检查网络'
WHERE NOT EXISTS (SELECT 1 FROM sys_ingest_request_status WHERE request_id = 'req-2024-004');

-- 5.2 异常凭证池
INSERT INTO arc_abnormal_voucher (id, request_id, source_system, voucher_number, sip_data, fail_reason, status)
SELECT 'abnor-001', 'req-2024-003', 'YONSUITE', 'JZ-202403-ERR01', '{"voucherNo":"JZ-202403-ERR01","amount":0}', '金额为空', 'PENDING'
WHERE NOT EXISTS (SELECT 1 FROM arc_abnormal_voucher WHERE id = 'abnor-001');

INSERT INTO arc_abnormal_voucher (id, request_id, source_system, voucher_number, sip_data, fail_reason, status)
SELECT 'abnor-002', 'req-2024-004', 'YONSUITE', 'JZ-202403-ERR02', '{"voucherNo":"JZ-202403-ERR02","date":null}', '业务日期缺失', 'PENDING'
WHERE NOT EXISTS (SELECT 1 FROM arc_abnormal_voucher WHERE id = 'abnor-002');

-- 完成
