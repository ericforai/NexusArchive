-- Input: Auth Ticket Table Creation
-- Output: Schema change for cross-fonds authorization ticket
-- Pos: db/migration/V75
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 1. 创建跨全宗访问授权票据表
CREATE TABLE IF NOT EXISTS auth_ticket (
    id VARCHAR(32) PRIMARY KEY,
    applicant_id VARCHAR(32) NOT NULL,
    applicant_name VARCHAR(100),
    source_fonds VARCHAR(50) NOT NULL,
    target_fonds VARCHAR(50) NOT NULL,
    scope TEXT NOT NULL,  -- JSON格式
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approval_snapshot TEXT,  -- JSON格式的审批链
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE auth_ticket IS '跨全宗访问授权票据表';
COMMENT ON COLUMN auth_ticket.id IS '主键ID';
COMMENT ON COLUMN auth_ticket.applicant_id IS '申请人ID';
COMMENT ON COLUMN auth_ticket.applicant_name IS '申请人姓名';
COMMENT ON COLUMN auth_ticket.source_fonds IS '源全宗号（申请人所属全宗）';
COMMENT ON COLUMN auth_ticket.target_fonds IS '目标全宗号';
COMMENT ON COLUMN auth_ticket.scope IS '访问范围（JSON格式）：{ "archiveYears": [2020, 2021], "docTypes": ["凭证"], "keywords": [], "accessType": "READ_ONLY" }';
COMMENT ON COLUMN auth_ticket.expires_at IS '有效期（必须 >= 当前时间 + 1天，<= 当前时间 + 90天）';
COMMENT ON COLUMN auth_ticket.status IS '状态: PENDING(待审批), FIRST_APPROVED(第一审批通过), APPROVED(已批准), REJECTED(已拒绝), REVOKED(已撤销), EXPIRED(已过期)';
COMMENT ON COLUMN auth_ticket.approval_snapshot IS '审批链快照（JSON格式）：{ "firstApprover": {...}, "secondApprover": {...} }';
COMMENT ON COLUMN auth_ticket.reason IS '申请原因';
COMMENT ON COLUMN auth_ticket.deleted IS '逻辑删除标记：0-未删除，1-已删除';

-- 2. 创建索引
CREATE INDEX IF NOT EXISTS idx_auth_ticket_applicant 
    ON auth_ticket(applicant_id, status, deleted);
CREATE INDEX IF NOT EXISTS idx_auth_ticket_target 
    ON auth_ticket(target_fonds, status, expires_at, deleted);
CREATE INDEX IF NOT EXISTS idx_auth_ticket_expires 
    ON auth_ticket(expires_at, status, deleted);
CREATE INDEX IF NOT EXISTS idx_auth_ticket_status 
    ON auth_ticket(status, deleted);

-- 3. 添加约束检查（PostgreSQL）
-- 有效期必须 >= 当前时间 + 1天，<= 当前时间 + 90天
-- 注意：这个约束在应用层验证，数据库层仅做基本检查
ALTER TABLE auth_ticket ADD CONSTRAINT chk_auth_ticket_expires 
    CHECK (expires_at > created_at + INTERVAL '1 day' AND expires_at <= created_at + INTERVAL '90 days');





