-- Input: Destruction Log Table Creation
-- Output: Schema change for destruction log
-- Pos: db/migration/V74
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 1. 创建销毁清册表（永久只读）
CREATE TABLE IF NOT EXISTS destruction_log (
    id VARCHAR(32) NOT NULL,
    fonds_no VARCHAR(50) NOT NULL,
    archive_year INT NOT NULL,
    archive_object_id VARCHAR(32) NOT NULL,
    retention_policy_id VARCHAR(32) NOT NULL,
    approval_ticket_id VARCHAR(64) NOT NULL,
    destroyed_by VARCHAR(32) NOT NULL,
    destroyed_at TIMESTAMP NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    snapshot TEXT NOT NULL,  -- JSON 格式的完整元数据快照
    prev_hash VARCHAR(128),
    curr_hash VARCHAR(128),
    sig TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, fonds_no, archive_year),
    -- 唯一性约束：防止同一档案被重复记录销毁
    UNIQUE (archive_object_id, fonds_no, archive_year)
);

COMMENT ON TABLE destruction_log IS '销毁清册表（永久只读，禁止修改/删除）';
COMMENT ON COLUMN destruction_log.id IS '主键ID';
COMMENT ON COLUMN destruction_log.fonds_no IS '全宗号';
COMMENT ON COLUMN destruction_log.archive_year IS '归档年度';
COMMENT ON COLUMN destruction_log.archive_object_id IS '档案ID';
COMMENT ON COLUMN destruction_log.retention_policy_id IS '保管期限ID';
COMMENT ON COLUMN destruction_log.approval_ticket_id IS '审批票据ID（销毁申请ID）';
COMMENT ON COLUMN destruction_log.destroyed_by IS '执行人ID';
COMMENT ON COLUMN destruction_log.destroyed_at IS '销毁时间';
COMMENT ON COLUMN destruction_log.trace_id IS '追踪ID（用于审计）';
COMMENT ON COLUMN destruction_log.snapshot IS '档案元数据快照（JSON格式，包含完整信息）';
COMMENT ON COLUMN destruction_log.prev_hash IS '前一条记录的哈希值（哈希链）';
COMMENT ON COLUMN destruction_log.curr_hash IS '当前记录的哈希值（哈希链）';
COMMENT ON COLUMN destruction_log.sig IS '数字签名（可选）';

-- 2. 创建索引
CREATE INDEX IF NOT EXISTS idx_destruction_log_fonds_year 
    ON destruction_log(fonds_no, archive_year, destroyed_at);
CREATE INDEX IF NOT EXISTS idx_destruction_log_destroyed_at 
    ON destruction_log(destroyed_at);
CREATE INDEX IF NOT EXISTS idx_destruction_log_trace_id 
    ON destruction_log(trace_id);

-- 3. 数据库触发器：禁止 UPDATE/DELETE 操作（不可篡改性保障）
-- PostgreSQL 版本
CREATE OR REPLACE FUNCTION prevent_destruction_log_modification()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'destruction_log table is read-only. UPDATE operation is not allowed.';
    ELSIF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'destruction_log table is read-only. DELETE operation is not allowed.';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER destruction_log_readonly_trigger
    BEFORE UPDATE OR DELETE ON destruction_log
    FOR EACH ROW
    EXECUTE FUNCTION prevent_destruction_log_modification();

COMMENT ON FUNCTION prevent_destruction_log_modification() IS '防止销毁清册表被修改或删除的触发器';

-- 4. 按年度分区（可选，大数据量时启用）
-- CREATE TABLE destruction_log_2025 PARTITION OF destruction_log FOR VALUES FROM (2025) TO (2026);

