-- 借阅记录表（历史归档）
CREATE TABLE acc_borrow_log (
    id VARCHAR(36) PRIMARY KEY,
    request_no VARCHAR(50) NOT NULL,               -- 借阅单号
    applicant_id VARCHAR(36) NOT NULL,            -- 申请人ID
    applicant_name VARCHAR(100) NOT NULL,         -- 申请人姓名
    dept_name VARCHAR(200),                      -- 部门名称
    purpose VARCHAR(500),                         -- 借阅目的
    borrow_type VARCHAR(20),                      -- 借阅类型
    borrow_start_date DATE,                       -- 借阅开始日期
    borrow_end_date DATE,                         -- 借阅结束日期
    archive_count INTEGER,                         -- 档案数量
    status VARCHAR(20),                            -- 最终状态: COMPLETED, CANCELLED
    created_time TIMESTAMP NOT NULL,
    completed_time TIMESTAMP
);

CREATE INDEX idx_borrow_log_applicant ON acc_borrow_log(applicant_id);
CREATE INDEX idx_borrow_log_dates ON acc_borrow_log(borrow_start_date, borrow_end_date);
COMMENT ON TABLE acc_borrow_log IS '借阅记录历史表';
