-- Input: 数据库引擎
-- Output: 演示/初始化数据写入
-- Pos: 数据初始化脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 插入主凭证 (Main Voucher)
-- 注意：business_doc_no = 'MOCK_V_001'
-- fixed: added storage_path (required)
INSERT INTO arc_file_content (
    id, archival_code, business_doc_no, 
    file_name, file_type, file_size, 
    voucher_type, 
    fiscal_year, pre_archive_status, 
    created_time, creator, fonds_code, source_system, erp_voucher_no,
    storage_path
) VALUES (
    'MOCK_MAIN_ID_001', 'ARC-2023-V001', 'MOCK_V_001',
    '记账凭证-2023001.pdf', 'pdf', 102400,
    'AC01',
    '2023', 'PENDING_CHECK',
    NOW(), '系统管理员', 'COMP001', 'MOCK', '记-2023001',
    'mock/2023/v001.pdf'
);

-- 插入关联附件 1 (Attachment 1 - Invoice Image)
-- 命名规则：主单号 + "_ATT_" + 序号 -> 'MOCK_V_001_ATT_1'
-- fixed: added archival_code (required) and storage_path
INSERT INTO arc_file_content (
    id, archival_code, business_doc_no, 
    file_name, file_type, file_size, 
    voucher_type, 
    fiscal_year, pre_archive_status, 
    created_time, creator, fonds_code, source_system, erp_voucher_no,
    storage_path
) VALUES (
    'MOCK_ATT_ID_001', 'ARC-2023-V001-ATT1', 'MOCK_V_001_ATT_1',
    '发票-INV88888.jpg', 'jpg', 51200,
    'ATTACHMENT',
    '2023', 'PENDING_CHECK',
    NOW(), '系统管理员', 'COMP001', 'MOCK', '记-2023001',
    'mock/2023/inv88888.jpg'
);

-- 插入关联附件 2 (Attachment 2 - Contract PDF)
-- 命名规则：主单号 + "_ATT_" + 序号 -> 'MOCK_V_001_ATT_2'
-- fixed: added archival_code (required) and storage_path
INSERT INTO arc_file_content (
    id, archival_code, business_doc_no, 
    file_name, file_type, file_size, 
    voucher_type, 
    fiscal_year, pre_archive_status, 
    created_time, creator, fonds_code, source_system, erp_voucher_no,
    storage_path
) VALUES (
    'MOCK_ATT_ID_002', 'ARC-2023-V001-ATT2', 'MOCK_V_001_ATT_2',
    '采购合同.pdf', 'pdf', 204800,
    'ATTACHMENT',
    '2023', 'PENDING_CHECK',
    NOW(), '系统管理员', 'COMP001', 'MOCK', '记-2023001',
    'mock/2023/contract.pdf'
);
