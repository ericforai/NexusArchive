-- Demo Data for Structured AIP Validation
-- Archival Code: COMP001-2023-10Y-FIN-AC01-V0088

-- 1. Insert Archive Record
INSERT INTO acc_archive (
    id, fonds_no, archive_code, category_code, title, 
    fiscal_year, fiscal_period, retention_period, org_name, 
    creator, status, security_level, created_at, updated_at, deleted
) VALUES (
    '1800000000000000088', 'COMP001', 'COMP001-2023-10Y-FIN-AC01-V0088', 'AC01', '2023年11月报销凭证',
    '2023', '2023-11', '10Y', 'Nexus Corp',
    'Demo User', 'archived', 'INTERNAL', NOW(), NOW(), 0
);

-- 2. Insert File Records
-- 2.1 Main Voucher (OFD) -> Should go to /content
INSERT INTO arc_file_content (
    id, archival_code, file_name, file_type, file_size, 
    file_hash, hash_algorithm, storage_path, created_time
) VALUES (
    '1800000000000000089', 'COMP001-2023-10Y-FIN-AC01-V0088', 'voucher_v0088.ofd', 'ofd', 10240,
    'dummy_hash_1', 'SHA-256', '/tmp/archives/COMP001/2023/10Y/AC01/COMP001-2023-10Y-FIN-AC01-V0088/content/voucher_v0088.ofd', NOW()
);

-- 2.2 Attachment (Contract) -> Should go to /attachment
INSERT INTO arc_file_content (
    id, archival_code, file_name, file_type, file_size, 
    file_hash, hash_algorithm, storage_path, created_time
) VALUES (
    '1800000000000000090', 'COMP001-2023-10Y-FIN-AC01-V0088', 'contract_2023_001.pdf', 'pdf', 20480,
    'dummy_hash_2', 'SHA-256', '/tmp/archives/COMP001/2023/10Y/AC01/COMP001-2023-10Y-FIN-AC01-V0088/content/contract_2023_001.pdf', NOW()
);

-- 2.3 Attachment (Bank Slip) -> Should go to /attachment
INSERT INTO arc_file_content (
    id, archival_code, file_name, file_type, file_size, 
    file_hash, hash_algorithm, storage_path, created_time
) VALUES (
    '1800000000000000091', 'COMP001-2023-10Y-FIN-AC01-V0088', 'bank_slip_001.jpg', 'jpg', 5120,
    'dummy_hash_3', 'SHA-256', '/tmp/archives/COMP001/2023/10Y/AC01/COMP001-2023-10Y-FIN-AC01-V0088/content/bank_slip_001.jpg', NOW()
);
