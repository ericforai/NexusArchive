-- Input: Fonds History Table Creation
-- Output: Schema change for fonds history tracking
-- Pos: db/migration/V76
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 1. 创建全宗沿革表（历史追溯）
CREATE TABLE IF NOT EXISTS fonds_history (
    id VARCHAR(32) PRIMARY KEY,
    fonds_no VARCHAR(50) NOT NULL,
    event_type VARCHAR(20) NOT NULL,  -- MERGE(合并), SPLIT(分立), MIGRATE(迁移), RENAME(重命名)
    from_fonds_no VARCHAR(50),  -- 源全宗号（用于合并/迁移）
    to_fonds_no VARCHAR(50),  -- 目标全宗号（用于迁移）
    effective_date DATE NOT NULL,  -- 生效日期
    reason TEXT,  -- 变更原因
    approval_ticket_id VARCHAR(64),  -- 审批票据ID
    snapshot_json TEXT,  -- 变更时的快照信息（JSON格式）
    created_by VARCHAR(32),  -- 创建人ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE fonds_history IS '全宗沿革表（历史追溯）';
COMMENT ON COLUMN fonds_history.id IS '主键ID';
COMMENT ON COLUMN fonds_history.fonds_no IS '全宗号（当前全宗）';
COMMENT ON COLUMN fonds_history.event_type IS '事件类型: MERGE(合并), SPLIT(分立), MIGRATE(迁移), RENAME(重命名)';
COMMENT ON COLUMN fonds_history.from_fonds_no IS '源全宗号（用于合并/迁移场景）';
COMMENT ON COLUMN fonds_history.to_fonds_no IS '目标全宗号（用于迁移场景）';
COMMENT ON COLUMN fonds_history.effective_date IS '生效日期';
COMMENT ON COLUMN fonds_history.reason IS '变更原因';
COMMENT ON COLUMN fonds_history.approval_ticket_id IS '审批票据ID（关联审批流程）';
COMMENT ON COLUMN fonds_history.snapshot_json IS '变更时的快照信息（JSON格式）：包含全宗信息、档案数量等';
COMMENT ON COLUMN fonds_history.created_by IS '创建人ID';
COMMENT ON COLUMN fonds_history.deleted IS '逻辑删除标记：0-未删除，1-已删除';

-- 2. 创建索引
CREATE INDEX IF NOT EXISTS idx_fonds_history_fonds_no 
    ON fonds_history(fonds_no, deleted);
CREATE INDEX IF NOT EXISTS idx_fonds_history_event_type 
    ON fonds_history(event_type, effective_date, deleted);
CREATE INDEX IF NOT EXISTS idx_fonds_history_from_fonds 
    ON fonds_history(from_fonds_no, deleted);
CREATE INDEX IF NOT EXISTS idx_fonds_history_to_fonds 
    ON fonds_history(to_fonds_no, deleted);





