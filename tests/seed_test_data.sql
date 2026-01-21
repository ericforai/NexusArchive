-- E2E 测试种子数据 (Happy Path)

-- 清理（为了幂等性）
DELETE FROM arc_file_content WHERE id LIKE 'E2E-%';
DELETE FROM arc_original_voucher_file WHERE id LIKE 'E2E-%';
DELETE FROM arc_original_voucher WHERE id LIKE 'E2E-%';
DELETE FROM collection_batch_file WHERE original_filename LIKE 'E2E-%';
DELETE FROM collection_batch WHERE batch_no LIKE 'E2E-%';

-- 1. 插入 Collection Batch
INSERT INTO collection_batch (
    batch_no, batch_name, fonds_id, fonds_code, fiscal_year, fiscal_period, 
    archival_category, source_channel, status, total_files, uploaded_files, 
    created_by, created_time
) VALUES (
    'E2E-BATCH-001', 'E2E Test Batch Happy Path', 'fonds-brjt', 'BRJT', '2025', '2025-01',
    'VOUCHER', 'MANUAL_UPLOAD', 'VALIDATED', 2, 2,
    'admin', NOW()
);

-- 2. 插入 Original Voucher
INSERT INTO arc_original_voucher (
    id, voucher_no, archival_category, source_type, voucher_type, 
    business_date, amount, currency, counterparty, summary,
    creator, auditor, bookkeeper, approver, 
    source_system, source_doc_id, fonds_code, fiscal_year, retention_period,
    archive_status, version, is_latest, created_by, created_time
) VALUES (
    'E2E-OV-001', 'SAP-202501-0001', 'VOUCHER', 'API_SYNC', 'TRANSFER_VOUCHER',
    '2025-01-15', 1000.00, 'CNY', 'Supplier A', 'Office Supplies',
    'ZhangSan', 'LiSi', 'WangWu', 'ZhaoLiu',
    'SAP', 'SAP-DOC-001', 'BRJT', '2025', '10Y',
    'PENDING', 1, true, 'admin', NOW()
);

-- 3. 插入 Original Voucher File
INSERT INTO arc_original_voucher_file (
    id, voucher_id, file_name, file_type, file_size, storage_path, 
    file_hash, hash_algorithm, file_role, sequence_no, created_by, created_time
) VALUES 
('E2E-OVF-001', 'E2E-OV-001', 'voucher_001.pdf', 'pdf', 1024, '/tmp/e2e/voucher_001.pdf', 'hash_001', 'SM3', 'PRIMARY', 1, 'admin', NOW()),
('E2E-OVF-002', 'E2E-OV-001', 'invoice_001.xml', 'xml', 512, '/tmp/e2e/invoice_001.xml', 'hash_002', 'SM3', 'ATTACHMENT', 2, 'admin', NOW());

-- 4. 插入 Collection Batch File
INSERT INTO collection_batch_file (
    batch_id, file_id, original_filename, file_size_bytes, upload_status, upload_order, created_time
)
SELECT id, 'E2E-OVF-001', 'voucher_001.pdf', 1024, 'VALIDATED', 1, NOW() FROM collection_batch WHERE batch_no = 'E2E-BATCH-001';

INSERT INTO collection_batch_file (
    batch_id, file_id, original_filename, file_size_bytes, upload_status, upload_order, created_time
)
SELECT id, 'E2E-OVF-002', 'invoice_001.xml', 512, 'VALIDATED', 2, NOW() FROM collection_batch WHERE batch_no = 'E2E-BATCH-001';

-- 5. 插入 Arc File Content (不设 batch_id)
INSERT INTO arc_file_content (
    id, archival_code, file_name, file_type, voucher_type, 
    pre_archive_status, created_time, file_size, file_hash, 
    hash_algorithm, storage_path, source_system, batch_id
) 
VALUES 
(
    'E2E-FILE-001', 'E2E-OV-001', 'voucher_001.pdf', 'pdf', 'VOUCHER', 
    'PENDING_CHECK', NOW(), 1024, 'hash_001', 
    'SM3', '/tmp/e2e/voucher_001.pdf', 'SAP', NULL
);

INSERT INTO arc_file_content (
    id, archival_code, file_name, file_type, voucher_type, 
    pre_archive_status, created_time, file_size, file_hash, 
    hash_algorithm, storage_path, source_system, batch_id
) 
VALUES 
(
    'E2E-FILE-002', 'E2E-OV-001', 'invoice_001.xml', 'xml', 'INVOICE', 
    'PENDING_CHECK', NOW(), 512, 'hash_002', 
    'SM3', '/tmp/e2e/invoice_001.xml', 'SAP', NULL
);
