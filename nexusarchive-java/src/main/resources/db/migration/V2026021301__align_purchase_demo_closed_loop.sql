-- V2026021301: 对齐采购演示链路闭环（记账凭证 + 原始凭证）
-- Input: 既有 demo-purchase-* 数据
-- Output: JZ-2025-12-001 在 acc_archive 可查，且关联单据在 arc_original_voucher 可查
-- Pos: 穿透联查演示数据闭环修复

-- 1) 记账凭证编码对齐：demo-purchase-jz-001 -> JZ-2025-12-001
UPDATE public.acc_archive
SET archive_code = 'JZ-2025-12-001',
    unique_biz_id = 'JZ-2025-12-001',
    last_modified_time = NOW()
WHERE id = 'demo-purchase-jz-001'
  AND deleted = 0;

-- 2) 补齐采购链路中的“付款申请单”节点（acc_archive）
INSERT INTO public.acc_archive (
    id, fonds_no, archive_code, category_code, title,
    fiscal_year, fiscal_period, retention_period,
    org_name, creator, status, amount, doc_date,
    unique_biz_id, security_level, created_by,
    created_time, last_modified_time, deleted
)
VALUES (
    'demo-purchase-sq-001', 'BR-GROUP', 'SQ-2025-12-001', 'AC04', '付款申请单-设备采购',
    '2025', '02', '30Y',
    '泊冉集团有限公司', '采购部', 'archived', 450000.00, '2025-02-21',
    'SQ-2025-12-001', 'internal', 'system',
    NOW(), NOW(), 0
)
ON CONFLICT (id) DO UPDATE
SET archive_code = EXCLUDED.archive_code,
    title = EXCLUDED.title,
    amount = EXCLUDED.amount,
    doc_date = EXCLUDED.doc_date,
    unique_biz_id = EXCLUDED.unique_biz_id,
    last_modified_time = NOW();

-- 3) 采购主线关系补齐（记账凭证 -> 付款单 -> 付款申请 -> 合同 -> 发票 -> 回单）
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_time, deleted)
VALUES
    ('demo-rel-200', 'demo-purchase-jz-001', 'demo-purchase-fk-001', 'CASH_FLOW', '资金流向', 'system', NOW(), 0),
    ('demo-rel-201', 'demo-purchase-fk-001', 'demo-purchase-sq-001', 'BASIS', '付款申请单', 'system', NOW(), 0),
    ('demo-rel-202', 'demo-purchase-sq-001', 'demo-purchase-ht-001', 'BASIS', '申请依据合同', 'system', NOW(), 0),
    ('demo-rel-203', 'demo-purchase-ht-001', 'demo-purchase-fp-001', 'BASIS', '合同依据', 'system', NOW(), 0),
    ('demo-rel-204', 'demo-purchase-fp-001', 'demo-purchase-hd-001', 'CASH_FLOW', '发票对应回单', 'system', NOW(), 0)
ON CONFLICT (id) DO UPDATE
SET relation_type = EXCLUDED.relation_type,
    relation_desc = EXCLUDED.relation_desc,
    deleted = 0;

-- 4) 在原始凭证主表中补齐链路单据（用于“原始凭证”模块闭环查询）
INSERT INTO public.arc_original_voucher (
    id, voucher_no, voucher_category, voucher_type, business_date,
    amount, currency, counterparty, summary, creator,
    source_system, source_doc_id, fonds_code, fiscal_year,
    retention_period, archive_status, archived_time, version,
    is_latest, created_by, created_time, deleted, pool_status,
    matched_voucher_id, matched_at
)
VALUES
    ('demo-ov-purchase-fk-001', 'FK-2025-02-001', 'DOCUMENT', 'TRANSFER_VOUCHER', '2025-02-22', 450000.00, 'CNY', '阿里云', '付款单-设备采购款', '财务部', 'DEMO_SEED', 'demo-purchase-fk-001', 'BR-GROUP', '2025', '30Y', 'ARCHIVED', NOW(), 1, true, 'system', NOW(), 0, 'MATCHED', 'demo-purchase-jz-001', NOW()),
    ('demo-ov-purchase-sq-001', 'SQ-2025-12-001', 'DOCUMENT', 'TRANSFER_VOUCHER', '2025-02-21', 450000.00, 'CNY', '阿里云', '付款申请单-设备采购', '采购部', 'DEMO_SEED', 'demo-purchase-sq-001', 'BR-GROUP', '2025', '30Y', 'ARCHIVED', NOW(), 1, true, 'system', NOW(), 0, 'MATCHED', 'demo-purchase-jz-001', NOW()),
    ('demo-ov-purchase-ht-001', 'HT-2025-02-001', 'CONTRACT', 'CONTRACT', '2025-02-15', 450000.00, 'CNY', '阿里云', '服务器采购合同', '采购部', 'DEMO_SEED', 'demo-purchase-ht-001', 'BR-GROUP', '2025', '30Y', 'ARCHIVED', NOW(), 1, true, 'system', NOW(), 0, 'MATCHED', 'demo-purchase-jz-001', NOW()),
    ('demo-ov-purchase-fp-001', 'FP-2025-02-001', 'INVOICE', 'INV_PAPER', '2025-02-20', 450000.00, 'CNY', '阿里云', '服务器采购发票', '系统', 'DEMO_SEED', 'demo-purchase-fp-001', 'BR-GROUP', '2025', '30Y', 'ARCHIVED', NOW(), 1, true, 'system', NOW(), 0, 'MATCHED', 'demo-purchase-jz-001', NOW()),
    ('demo-ov-purchase-hd-001', 'HD-2025-02-001', 'BANK', 'BANK_RECEIPT', '2025-02-22', 450000.00, 'CNY', '招商银行', '银行回单-招商银行转账', '系统', 'DEMO_SEED', 'demo-purchase-hd-001', 'BR-GROUP', '2025', '30Y', 'ARCHIVED', NOW(), 1, true, 'system', NOW(), 0, 'MATCHED', 'demo-purchase-jz-001', NOW())
ON CONFLICT (id) DO UPDATE
SET voucher_no = EXCLUDED.voucher_no,
    voucher_category = EXCLUDED.voucher_category,
    voucher_type = EXCLUDED.voucher_type,
    business_date = EXCLUDED.business_date,
    amount = EXCLUDED.amount,
    summary = EXCLUDED.summary,
    source_doc_id = EXCLUDED.source_doc_id,
    matched_voucher_id = EXCLUDED.matched_voucher_id,
    matched_at = NOW(),
    deleted = 0;

-- 5) 原始凭证 <-> 记账凭证 关联补齐
INSERT INTO public.arc_voucher_relation (
    id, original_voucher_id, accounting_voucher_id, relation_type, relation_desc, created_by, created_time, deleted
)
VALUES
    ('demo-vr-200', 'demo-ov-purchase-fk-001', 'demo-purchase-jz-001', 'ORIGINAL_TO_ACCOUNTING', '付款单关联记账凭证', 'system', NOW(), 0),
    ('demo-vr-201', 'demo-ov-purchase-sq-001', 'demo-purchase-jz-001', 'ORIGINAL_TO_ACCOUNTING', '付款申请关联记账凭证', 'system', NOW(), 0),
    ('demo-vr-202', 'demo-ov-purchase-ht-001', 'demo-purchase-jz-001', 'ORIGINAL_TO_ACCOUNTING', '合同关联记账凭证', 'system', NOW(), 0),
    ('demo-vr-203', 'demo-ov-purchase-fp-001', 'demo-purchase-jz-001', 'ORIGINAL_TO_ACCOUNTING', '发票关联记账凭证', 'system', NOW(), 0),
    ('demo-vr-204', 'demo-ov-purchase-hd-001', 'demo-purchase-jz-001', 'ORIGINAL_TO_ACCOUNTING', '回单关联记账凭证', 'system', NOW(), 0)
ON CONFLICT (id) DO UPDATE
SET relation_desc = EXCLUDED.relation_desc,
    deleted = 0;

