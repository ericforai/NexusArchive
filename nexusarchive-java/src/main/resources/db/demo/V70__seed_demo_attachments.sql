-- Input: Flyway 迁移引擎
-- Output: Demo 数据附件关联初始化
-- Pos: 演示环境附件种子脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- =====================================================
-- V70: Demo 数据文件与档案关联
-- 将 docs/demo数据 中的 PDF 文件关联到现有归档档案
-- =====================================================

-- ==================== 1. 插入文件记录 ====================

INSERT INTO arc_file_content (
    id, archival_code, file_name, file_type, file_size,
    file_hash, hash_algorithm, storage_path, created_time,
    voucher_type, fiscal_year, fonds_code, source_system, creator
) VALUES 
-- 1.1 上海米山神鸡餐饮管理有限公司 发票 (201元) -> arc-2024-001
('demo-file-001', 'BRJT-2024-30Y-FIN-AC01-0001', '上海米山神鸡餐饮管理有限公司_发票金额201.00元.pdf', 'pdf', 101613,
    'demo_hash_001', 'SHA-256', 'demo/上海米山神鸡餐饮管理有限公司_发票金额201.00元.pdf', NOW(),
    'ATTACHMENT', '2024', 'BRJT', 'DEMO', '系统管理员'),
-- 1.2 上海市长宁区吴奕聪餐饮店 发票 -> arc-2024-002
('demo-file-002', 'BRJT-2024-30Y-FIN-AC01-0002', 'dzfp_25314000000004648601_上海市长宁区吴奕聪餐饮店_20251025012013.pdf', 'pdf', 101657,
    'demo_hash_002', 'SHA-256', 'demo/dzfp_25314000000004648601_上海市长宁区吴奕聪餐饮店_20251025012013.pdf', NOW(),
    'ATTACHMENT', '2024', 'BRJT', 'DEMO', '系统管理员'),
-- 1.3 报销单 -> arc-2024-004 (员工差旅费报销)
('demo-file-003', 'BRJT-2024-30Y-FIN-AC01-0004', '报销.pdf', 'pdf', 107783,
    'demo_hash_003', 'SHA-256', 'demo/报销.pdf', NOW(),
    'ATTACHMENT', '2024', 'BRJT', 'DEMO', '系统管理员'),
-- 1.4 电子发票 25312000000349611002 -> arc-2024-003 (收款凭证)
('demo-file-004', 'BRJT-2024-30Y-FIN-AC01-0003', '25312000000349611002_ba1d.pdf', 'pdf', 101601,
    'demo_hash_004', 'SHA-256', 'demo/25312000000349611002_ba1d.pdf', NOW(),
    'ATTACHMENT', '2024', 'BRJT', 'DEMO', '系统管理员'),
-- 1.5 出租车发票 -> arc-2024-008 (团队聚餐费用)
('demo-file-005', 'BRJT-2024-30Y-FIN-AC01-0008', '20220927580001302018_上海强生出租汽车有限公司第一分公司_上海强生交通（集团）有限公司_20220920_10000.00.pdf', 'pdf', 25399,
    'demo_hash_005', 'SHA-256', 'demo/20220927580001302018_上海强生出租汽车有限公司第一分公司_上海强生交通（集团）有限公司_20220920_10000.00.pdf', NOW(),
    'ATTACHMENT', '2024', 'BRJT', 'DEMO', '系统管理员')
ON CONFLICT (id) DO NOTHING;

-- ==================== 2. 插入附件关联记录 ====================

INSERT INTO acc_archive_attachment (id, archive_id, file_id, attachment_type, relation_desc, created_by, created_at)
VALUES 
-- 2.1 arc-2024-002 <- demo-file-002 (吴奕聪餐饮店发票)
('demo-att-002', 'arc-2024-002', 'demo-file-002', 'invoice', '电子发票', 'system', NOW()),
-- 2.2 arc-2024-003 <- demo-file-004 (收款发票)
('demo-att-004', 'arc-2024-003', 'demo-file-004', 'invoice', '软件服务收款发票', 'system', NOW()),
-- 2.3 arc-2024-004 <- demo-file-003 (报销单)
('demo-att-003', 'arc-2024-004', 'demo-file-003', 'other', '报销申请单', 'system', NOW()),
-- 2.4 arc-2024-008 <- demo-file-005 (出租车票)
('demo-att-005', 'arc-2024-008', 'demo-file-005', 'other', '出租车费用发票', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- 完成
