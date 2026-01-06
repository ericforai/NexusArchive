-- 借阅申请表
CREATE TABLE acc_borrow_request (
    id VARCHAR(36) PRIMARY KEY,
    request_no VARCHAR(50) UNIQUE NOT NULL,        -- 借阅单号 (BL-YYYYMMDD-序号)
    applicant_id VARCHAR(36) NOT NULL,             -- 申请人ID
    applicant_name VARCHAR(100) NOT NULL,          -- 申请人姓名
    dept_id VARCHAR(36),                          -- 申请部门ID
    dept_name VARCHAR(200),                       -- 申请部门名称
    purpose VARCHAR(500) NOT NULL,                 -- 借阅目的
    borrow_type VARCHAR(20) NOT NULL,              -- 借阅类型: READING(阅览), COPY(复制), LOAN(外借)
    expected_start_date DATE NOT NULL,             -- 预期开始日期
    expected_end_date DATE NOT NULL,               -- 预期结束日期
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 状态: PENDING, APPROVED, REJECTED, BORROWING, RETURNED, OVERDUE
    archive_ids TEXT NOT NULL,                     -- 借阅档案ID列表 (JSON数组)
    archive_count INTEGER NOT NULL,                -- 借阅档案数量
    approver_id VARCHAR(36),                       -- 审批人ID
    approver_name VARCHAR(100),                     -- 审批人姓名
    approval_time TIMESTAMP,                        -- 审批时间
    approval_comment VARCHAR(500),                  -- 审批意见
    actual_start_date DATE,                         -- 实际开始日期
    actual_end_date DATE,                           -- 实际结束日期
    return_time TIMESTAMP,                           -- 归还时间
    return_operator_id VARCHAR(36),                 -- 归还操作人ID
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted NUMERIC(1) DEFAULT 0 NOT NULL
);

CREATE INDEX idx_borrow_request_applicant ON acc_borrow_request(applicant_id);
CREATE INDEX idx_borrow_request_status ON acc_borrow_request(status);
CREATE INDEX idx_borrow_request_dates ON acc_borrow_request(expected_start_date, expected_end_date);
COMMENT ON TABLE acc_borrow_request IS '档案借阅申请表';
