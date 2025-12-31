-- Input: Fonds scope, audit log, borrowing model alignment
-- Output: Schema change for fonds scope and audit fields
-- Pos: db/migration/V79
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 1. 用户-全宗授权范围
CREATE TABLE IF NOT EXISTS sys_user_fonds_scope (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    fonds_no VARCHAR(50) NOT NULL,
    scope_type VARCHAR(32) DEFAULT 'DIRECT',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

COMMENT ON TABLE sys_user_fonds_scope IS '用户-全宗授权范围';
COMMENT ON COLUMN sys_user_fonds_scope.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_fonds_scope.fonds_no IS '全宗号';
COMMENT ON COLUMN sys_user_fonds_scope.scope_type IS '授权来源类型';
COMMENT ON COLUMN sys_user_fonds_scope.created_time IS '创建时间';
COMMENT ON COLUMN sys_user_fonds_scope.last_modified_time IS '更新时间';
COMMENT ON COLUMN sys_user_fonds_scope.deleted IS '逻辑删除标识';

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_fonds_scope
    ON sys_user_fonds_scope(user_id, fonds_no);

CREATE INDEX IF NOT EXISTS idx_user_fonds_scope_user
    ON sys_user_fonds_scope(user_id, deleted);

INSERT INTO sys_user_fonds_scope (id, user_id, fonds_no, scope_type, created_time, last_modified_time, deleted)
SELECT CONCAT(u.id, '-', u.org_code), u.id, u.org_code, 'DIRECT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM sys_user u
WHERE u.org_code IS NOT NULL
  AND u.org_code <> ''
  AND u.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_fonds_scope s
      WHERE s.user_id = u.id AND s.fonds_no = u.org_code
  );

-- 2. 审计日志补齐字段
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64);
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS data_snapshot TEXT;
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS source_fonds VARCHAR(50);
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS target_fonds VARCHAR(50);
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS auth_ticket_id VARCHAR(64);

COMMENT ON COLUMN sys_audit_log.trace_id IS 'TraceID(全链路追踪)';
COMMENT ON COLUMN sys_audit_log.data_snapshot IS '脱敏后的快照';
COMMENT ON COLUMN sys_audit_log.source_fonds IS '跨全宗访问源全宗';
COMMENT ON COLUMN sys_audit_log.target_fonds IS '跨全宗访问目标全宗';
COMMENT ON COLUMN sys_audit_log.auth_ticket_id IS '跨全宗授权票据ID';

-- 3. 借阅记录补齐字段
ALTER TABLE biz_borrowing ADD COLUMN IF NOT EXISTS fonds_no VARCHAR(50);
ALTER TABLE biz_borrowing ADD COLUMN IF NOT EXISTS archive_year INT;
ALTER TABLE biz_borrowing ADD COLUMN IF NOT EXISTS type VARCHAR(20) DEFAULT 'electronic';
ALTER TABLE biz_borrowing ADD COLUMN IF NOT EXISTS return_deadline DATE;
ALTER TABLE biz_borrowing ADD COLUMN IF NOT EXISTS actual_return_time DATE;

COMMENT ON COLUMN biz_borrowing.fonds_no IS '全宗号';
COMMENT ON COLUMN biz_borrowing.archive_year IS '归档年度';
COMMENT ON COLUMN biz_borrowing.type IS '借阅类型: electronic/physical';
COMMENT ON COLUMN biz_borrowing.return_deadline IS '归还截止日期';
COMMENT ON COLUMN biz_borrowing.actual_return_time IS '实际归还时间';

UPDATE biz_borrowing
SET return_deadline = expected_return_date
WHERE return_deadline IS NULL
  AND expected_return_date IS NOT NULL;

UPDATE biz_borrowing
SET actual_return_time = actual_return_date
WHERE actual_return_time IS NULL
  AND actual_return_date IS NOT NULL;

UPDATE biz_borrowing
SET fonds_no = (
    SELECT a.fonds_no
    FROM acc_archive a
    WHERE a.id = biz_borrowing.archive_id
)
WHERE fonds_no IS NULL;

UPDATE biz_borrowing
SET archive_year = (
    SELECT CAST(a.fiscal_year AS INTEGER)
    FROM acc_archive a
    WHERE a.id = biz_borrowing.archive_id
)
WHERE archive_year IS NULL;

CREATE INDEX IF NOT EXISTS idx_borrowing_fonds_year_status
    ON biz_borrowing(fonds_no, archive_year, status);
