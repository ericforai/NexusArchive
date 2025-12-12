-- V7: 添加-- 档案审批表 (Archive Approval Table)
-- 用于记录档案的审批流程
CREATE TABLE IF NOT EXISTS biz_archive_approval (
    id VARCHAR(64) PRIMARY KEY,
    archive_id VARCHAR(64) NOT NULL,
    archive_code VARCHAR(100),
    archive_title VARCHAR(500),
    applicant_id VARCHAR(64) NOT NULL,
    applicant_name VARCHAR(100),
    application_reason TEXT,
    approver_id VARCHAR(64),
    approver_name VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approval_comment TEXT,
    approval_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0
);

-- 创建索引 (使用 IF NOT EXISTS 确保幂等性)
CREATE INDEX IF NOT EXISTS idx_archive_approval_status ON biz_archive_approval(status);
CREATE INDEX IF NOT EXISTS idx_archive_approval_archive_id ON biz_archive_approval(archive_id);
CREATE INDEX IF NOT EXISTS idx_archive_approval_applicant ON biz_archive_approval(applicant_id);
CREATE INDEX IF NOT EXISTS idx_archive_approval_created_at ON biz_archive_approval(created_at);
