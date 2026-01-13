-- ============================================================================
-- 修复借阅相关表结构不一致问题
-- 问题描述：
--   1. acc_borrow_request 和 acc_borrow_archive 表在某些环境中不存在
--   2. acc_borrow_log 表结构与 BorrowLog.java Entity 不匹配
-- 解决方案：使用 IF NOT EXISTS 确保表存在且结构正确
-- ============================================================================

-- ============================================================================
-- 1. 修复 acc_borrow_request 表
-- ============================================================================
CREATE TABLE IF NOT EXISTS acc_borrow_request (
    id VARCHAR(36) PRIMARY KEY,
    request_no VARCHAR(50) UNIQUE NOT NULL,        -- 借阅单号 (BL-YYYYMMDD-序号)
    applicant_id VARCHAR(36) NOT NULL,             -- 申请人ID
    applicant_name VARCHAR(100) NOT NULL,          -- 申请人姓名
    dept_id VARCHAR(36),                           -- 申请部门ID
    dept_name VARCHAR(200),                        -- 申请部门名称
    purpose VARCHAR(500) NOT NULL,                 -- 借阅目的
    borrow_type VARCHAR(20) NOT NULL,              -- 借阅类型: READING/COPY/LOAN
    expected_start_date DATE NOT NULL,             -- 预期开始日期
    expected_end_date DATE NOT NULL,               -- 预期结束日期
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 状态
    archive_ids TEXT NOT NULL,                     -- 借阅档案ID列表 (JSON)
    archive_count INTEGER NOT NULL,                -- 借阅档案数量
    approver_id VARCHAR(36),                       -- 审批人ID
    approver_name VARCHAR(100),                    -- 审批人姓名
    approval_time TIMESTAMP,                       -- 审批时间
    approval_comment VARCHAR(500),                 -- 审批意见
    actual_start_date DATE,                        -- 实际开始日期
    actual_end_date DATE,                          -- 实际结束日期
    return_time TIMESTAMP,                         -- 归还时间
    return_operator_id VARCHAR(36),                -- 归还操作人ID
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_borrow_request_applicant ON acc_borrow_request(applicant_id);
CREATE INDEX IF NOT EXISTS idx_borrow_request_status ON acc_borrow_request(status);
CREATE INDEX IF NOT EXISTS idx_borrow_request_dates ON acc_borrow_request(expected_start_date, expected_end_date);

-- ============================================================================
-- 2. 修复 acc_borrow_archive 表
-- ============================================================================
CREATE TABLE IF NOT EXISTS acc_borrow_archive (
    id VARCHAR(36) PRIMARY KEY,
    borrow_request_id VARCHAR(36) NOT NULL,        -- 借阅申请ID
    archive_id VARCHAR(36) NOT NULL,               -- 档案ID
    archive_code VARCHAR(100) NOT NULL,            -- 档号
    archive_title VARCHAR(500) NOT NULL,           -- 题名
    return_status VARCHAR(20) DEFAULT 'BORROWED',  -- 归还状态: BORROWED/RETURNED
    return_time TIMESTAMP,                         -- 归还时间
    return_operator_id VARCHAR(36),                -- 归还操作人ID
    damaged BOOLEAN DEFAULT FALSE,                 -- 是否损坏
    damage_desc VARCHAR(500),                      -- 损坏描述
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_borrow_archive_request ON acc_borrow_archive(borrow_request_id);
CREATE INDEX IF NOT EXISTS idx_borrow_archive_archive ON acc_borrow_archive(archive_id);

-- ============================================================================
-- 3. 修复 acc_borrow_log 表
-- 注意：该表之前可能以错误结构创建，需要检测并重建
-- ============================================================================

-- 检查表是否存在并具有正确的列结构
DO $$
BEGIN
    -- 如果表存在但缺少 request_no 列（旧结构），则重建
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_name = 'acc_borrow_log' AND table_schema = 'public'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'acc_borrow_log' AND column_name = 'request_no'
    ) THEN
        -- 删除旧表（旧结构无用数据）
        DROP TABLE acc_borrow_log;
    END IF;
END $$;

-- 创建正确结构的表
CREATE TABLE IF NOT EXISTS acc_borrow_log (
    id VARCHAR(36) PRIMARY KEY,
    request_no VARCHAR(50),                        -- 借阅单号
    applicant_id VARCHAR(36),                      -- 申请人ID
    applicant_name VARCHAR(100),                   -- 申请人姓名
    dept_name VARCHAR(200),                        -- 部门名称
    purpose VARCHAR(500),                          -- 借阅目的
    borrow_type VARCHAR(20),                       -- 借阅类型
    borrow_start_date DATE,                        -- 借阅开始日期
    borrow_end_date DATE,                          -- 借阅结束日期
    archive_count INTEGER,                         -- 档案数量
    status VARCHAR(20),                            -- 最终状态: COMPLETED/CANCELLED
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_borrow_log_applicant ON acc_borrow_log(applicant_id);
CREATE INDEX IF NOT EXISTS idx_borrow_log_dates ON acc_borrow_log(borrow_start_date, borrow_end_date);

-- 添加表注释
COMMENT ON TABLE acc_borrow_request IS '档案借阅申请表';
COMMENT ON TABLE acc_borrow_archive IS '借阅档案明细表';
COMMENT ON TABLE acc_borrow_log IS '借阅记录历史表';
