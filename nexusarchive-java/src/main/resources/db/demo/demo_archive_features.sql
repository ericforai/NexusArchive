-- Input: 数据库引擎
-- Output: 演示/初始化数据写入
-- Pos: 数据初始化脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- =====================================================
-- 档案审批与开放鉴定演示数据（直接SQL插入）
-- Demo Data for Archive Approval and Open Appraisal (Direct SQL)
-- =====================================================

-- 插入档案审批演示数据
-- 插入档案审批演示数据
INSERT INTO biz_archive_approval (
    id, archive_id, archive_code, archive_title,
    applicant_id, applicant_name, application_reason,
    status, created_at, updated_at, deleted
) VALUES
('AA-2024-001', 'ARCH-2024-001', 'QZ-2024-KJ-001', '2024年1月记账凭证',
 'user001', '张三', '完成四性检测，申请正式归档',
 'PENDING', NOW(), NOW(), 0),

('AA-2024-002', 'ARCH-2024-002', 'QZ-2024-KJ-002', '2024年2月记账凭证',
 'user002', '李四', '凭证已完成关联，申请归档',
 'PENDING', NOW(), NOW(), 0),

('AA-2024-003', 'ARCH-2024-003', 'QZ-2024-BB-001', '2024年第一季度财务报告',
 'user003', '王五', '季度报告已审核，申请归档',
 'PENDING', NOW(), NOW(), 0),

('AA-2024-004', 'ARCH-2024-004', 'QZ-2024-KJ-003', '2024年3月记账凭证',
 'user004', '赵六', '月度凭证整理完成，申请归档',
 'PENDING', NOW(), NOW(), 0),

('AA-2024-005', 'ARCH-2024-005', 'QZ-2024-ZB-001', '2024年总账（1-3月）',
 'user002', '李四', '季度总账，申请归档',
 'PENDING', NOW(), NOW(), 0)
ON CONFLICT (id) DO UPDATE SET
    archive_id = EXCLUDED.archive_id,
    archive_code = EXCLUDED.archive_code,
    archive_title = EXCLUDED.archive_title,
    applicant_id = EXCLUDED.applicant_id,
    applicant_name = EXCLUDED.applicant_name,
    application_reason = EXCLUDED.application_reason,
    status = EXCLUDED.status, -- Reset status to PENDING
    updated_at = NOW(),
    deleted = 0;

-- 插入开放鉴定演示数据
INSERT INTO biz_open_appraisal (
    id, archive_id, archive_code, archive_title,
    retention_period, current_security_level,
    status, created_at, updated_at, deleted
) VALUES
('OA-2014-001', 'ARCH-2014-001', 'QZ-2014-KJ-001', '2014年1月记账凭证',
 '10Y', 'INTERNAL',
 'PENDING', NOW(), NOW(), 0),

('OA-2014-002', 'ARCH-2014-002', 'QZ-2014-KJ-002', '2014年2月记账凭证',
 '10Y', 'INTERNAL',
 'PENDING', NOW(), NOW(), 0),

('OA-2014-003', 'ARCH-2014-003', 'QZ-2014-BB-001', '2014年第一季度财务报告',
 '10Y', 'INTERNAL',
 'PENDING', NOW(), NOW(), 0),

('OA-2013-004', 'ARCH-2013-004', 'QZ-2013-HT-001', '2013年设备采购合同',
 '10Y', 'INTERNAL',
 'PENDING', NOW(), NOW(), 0),

('OA-2013-005', 'ARCH-2013-005', 'QZ-2013-KJ-012', '2013年12月记账凭证',
 '10Y', 'INTERNAL',
 'PENDING', NOW(), NOW(), 0),

('OA-2013-006', 'ARCH-2013-006', 'QZ-2013-BB-004', '2013年度财务决算报告',
 '10Y', 'SECRET',
 'PENDING', NOW(), NOW(), 0)
ON CONFLICT (id) DO UPDATE SET
    archive_id = EXCLUDED.archive_id,
    archive_code = EXCLUDED.archive_code,
    archive_title = EXCLUDED.archive_title,
    retention_period = EXCLUDED.retention_period,
    current_security_level = EXCLUDED.current_security_level,
    status = EXCLUDED.status, -- Reset status to PENDING
    updated_at = NOW(),
    deleted = 0;

-- =====================================================
-- 插入会计凭证演示数据 (Restore Lost Demo Data)
-- =====================================================
INSERT INTO acc_archive (
    id, fonds_no, archive_code, category_code, title,
    fiscal_year, fiscal_period, retention_period, org_name,
    status, security_level, amount, doc_date, custom_metadata,
    created_by, created_at, updated_at, deleted
) VALUES
('DEMO-ARC-001', 'COMP001', 'COMP001-2023-10Y-FIN-AC01-V0051', 'AC01', '付款凭证-1002 银行存款',
 '2023', '11', '10Y', '总公司',
 'archived', 'INTERNAL', 45200.00, '2023-11-03', '{"subject":"1002 银行存款","pageCount":2}'::jsonb,
 'user_admin', '2023-11-03 09:00:00', NOW(), 0),

('DEMO-ARC-002', 'COMP001', 'COMP001-2023-10Y-FIN-AC01-V0052', 'AC01', '收款凭证-5001 主营业务收入',
 '2023', '11', '10Y', '分公司A',
 'archived', 'INTERNAL', 125000.00, '2023-11-02', '{"subject":"5001 主营业务收入","pageCount":2}'::jsonb,
 'user_admin', '2023-11-02 10:30:00', NOW(), 0),

('DEMO-ARC-003', 'COMP001', 'COMP001-2023-10Y-FIN-AC01-V0053', 'AC01', '转账凭证-6001 主营业务成本',
 '2023', '11', '10Y', '总公司',
 'archived', 'INTERNAL', 28500.00, '2023-11-01', '{"subject":"6001 主营业务成本","pageCount":2}'::jsonb,
 'user_admin', '2023-11-01 14:20:00', NOW(), 0),

('DEMO-ARC-004', 'COMP001', 'COMP001-2023-10Y-FIN-AC01-V0098', 'AC01', '收款凭证-2001 短期借款',
 '2023', '10', '10Y', '分公司B',
 'archived', 'INTERNAL', 500000.00, '2023-10-28', '{"subject":"2001 短期借款"}'::jsonb,
 'user_admin', '2023-10-28 11:15:00', NOW(), 0),

('DEMO-ARC-005', 'COMP001', 'COMP001-2023-10Y-FIN-AC01-V0095', 'AC01', '付款凭证-1001 库存现金',
 '2023', '10', '10Y', '总公司',
 'archived', 'INTERNAL', 5600.00, '2023-10-25', '{"subject":"1001 库存现金"}'::jsonb,
 'user_admin', '2023-10-25 16:45:00', NOW(), 0),

-- 会计账簿 (AC02)
('DEMO-ARC-006', 'COMP001', 'COMP001-2023-30Y-FIN-AC02-L0001', 'AC02', '2023年度总账',
 '2023', '全年', '30Y', '总公司',
 'archived', 'INTERNAL', NULL, '2023-12-31', '{"pageCount":120,"subject":"总账"}'::jsonb,
 'user_admin', '2023-12-31 09:00:00', NOW(), 0),
('DEMO-ARC-007', 'COMP001', 'COMP001-2023-30Y-FIN-AC02-L0002', 'AC02', '2023年现金日记账',
 '2023', '全年', '30Y', '分公司A',
 'archived', 'INTERNAL', NULL, '2023-12-31', '{"pageCount":36,"subject":"现金日记账"}'::jsonb,
 'user_admin', '2023-12-31 09:30:00', NOW(), 0),

-- 财务报告 (AC03)
('DEMO-ARC-008', 'COMP001', 'COMP001-2023-PERM-FIN-AC03-R0001', 'AC03', '2023年度财务决算报告',
 '2023', '2023', 'PERM', '总公司',
 'archived', 'INTERNAL', NULL, '2024-03-31', '{"pageCount":80,"reportType":"年度报告","totalAssets":12000000}'::jsonb,
 'user_admin', '2024-03-31 09:00:00', NOW(), 0),
('DEMO-ARC-009', 'COMP001', 'COMP001-2023-Q1-FIN-AC03-R0002', 'AC03', '2023年第一季度财务报告',
 '2023', 'Q1', 'PERM', '分公司A',
 'archived', 'INTERNAL', NULL, '2023-04-15', '{"pageCount":35,"reportType":"季度报告","revenue":3500000}'::jsonb,
 'user_admin', '2023-04-15 14:00:00', NOW(), 0)
ON CONFLICT (id) DO UPDATE SET
    archive_code = EXCLUDED.archive_code,
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    amount = EXCLUDED.amount,
    doc_date = EXCLUDED.doc_date,
    custom_metadata = EXCLUDED.custom_metadata,
    updated_at = NOW(),
    deleted = 0;
