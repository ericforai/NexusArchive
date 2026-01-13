-- Fix Mock Data Request No Collision & Invalid Enums

DELETE FROM acc_borrow_request WHERE id LIKE 'mock-%';

-- Re-insert with MOCK prefix and Valid Enums (READING, COPY, LOAN)
-- 1. Pending Request (Recent)
INSERT INTO acc_borrow_request (
    id, request_no, applicant_id, applicant_name, dept_id, dept_name,
    purpose, borrow_type, expected_start_date, expected_end_date,
    status, archive_ids, archive_count, created_time, updated_time
) VALUES (
    'mock-req-001', 'MOCK-20260113-001', 'user-1', '测试用户', 'dept-1', '研发部',
    '查阅2024年第一季度财务报告以核对预算', 'READING', CURRENT_DATE, CURRENT_DATE + 7,
    'PENDING', '["arc-2024-r01"]', 1, NOW(), NOW()
);

-- 2. Pending Request (Multiple Archives)
INSERT INTO acc_borrow_request (
    id, request_no, applicant_id, applicant_name, dept_id, dept_name,
    purpose, borrow_type, expected_start_date, expected_end_date,
    status, archive_ids, archive_count, created_time, updated_time
) VALUES (
    'mock-req-002', 'MOCK-20260113-002', 'user-2', '李财务', 'dept-2', '财务部',
    '审计抽查：支付凭证与合同比对', 'READING', CURRENT_DATE, CURRENT_DATE + 14,
    'PENDING', '["arc-2024-001", "arc-2024-002", "arc-2024-c01"]', 3, NOW(), NOW()
);

-- 3. Approved Request
INSERT INTO acc_borrow_request (
    id, request_no, applicant_id, applicant_name, dept_id, dept_name,
    purpose, borrow_type, expected_start_date, expected_end_date,
    status, archive_ids, archive_count, created_time, updated_time,
    approver_id, approver_name, approval_time, approval_comment
) VALUES (
    'mock-req-003', 'MOCK-20260112-003', 'user-1', '测试用户', 'dept-1', '研发部',
    '阿里云服务器费用报销核对', 'READING', CURRENT_DATE - 1, CURRENT_DATE + 6,
    'APPROVED', '["arc-2024-005"]', 1, NOW() - INTERVAL '1 day', NOW() - INTERVAL '20 hours',
    'admin-1', '管理员', NOW() - INTERVAL '20 hours', '同意借阅'
);

-- 4. Borrowing (Checked Out)
INSERT INTO acc_borrow_request (
    id, request_no, applicant_id, applicant_name, dept_id, dept_name,
    purpose, borrow_type, expected_start_date, expected_end_date,
    status, archive_ids, archive_count, created_time, updated_time,
    approver_id, approver_name, approval_time, approval_comment,
    actual_start_date
) VALUES (
    'mock-req-004', 'MOCK-20260110-004', 'user-3', '王审计', 'dept-3', '审计部',
    '年度总账现场审计', 'LOAN', CURRENT_DATE - 3, CURRENT_DATE + 27,
    'BORROWING', '["arc-2023-l01", "arc-2023-l02"]', 2, NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days',
    'admin-1', '管理员', NOW() - INTERVAL '3 days', '注意原件保管',
    CURRENT_DATE - 3
);

-- 5. Returned (Completed)
INSERT INTO acc_borrow_request (
    id, request_no, applicant_id, applicant_name, dept_id, dept_name,
    purpose, borrow_type, expected_start_date, expected_end_date,
    status, archive_ids, archive_count, created_time, updated_time,
    approver_id, approver_name, approval_time, approval_comment,
    actual_start_date, actual_end_date, return_time, return_operator_id
) VALUES (
    'mock-req-005', 'MOCK-20260105-005', 'user-1', '测试用户', 'dept-1', '研发部',
    '临时查阅：办公室租赁合同', 'READING', CURRENT_DATE - 8, CURRENT_DATE - 1,
    'RETURNED', '["arc-2024-c02"]', 1, NOW() - INTERVAL '8 days', NOW(),
    'admin-1', '管理员', NOW() - INTERVAL '8 days', 'OK',
    CURRENT_DATE - 8, CURRENT_DATE - 1, NOW() - INTERVAL '1 day', 'admin-1'
);

-- 6. Rejected
INSERT INTO acc_borrow_request (
    id, request_no, applicant_id, applicant_name, dept_id, dept_name,
    purpose, borrow_type, expected_start_date, expected_end_date,
    status, archive_ids, archive_count, created_time, updated_time,
    approver_id, approver_name, approval_time, approval_comment
) VALUES (
    'mock-req-006', 'MOCK-20260101-006', 'user-4', '张实习', 'dept-1', '研发部',
    '随便看看', 'READING', CURRENT_DATE - 12, CURRENT_DATE - 10,
    'REJECTED', '["seed-ledger-gen-001"]', 1, NOW() - INTERVAL '12 days', NOW() - INTERVAL '11 days',
    'admin-1', '管理员', NOW() - INTERVAL '11 days', '借阅理由不充分'
);
