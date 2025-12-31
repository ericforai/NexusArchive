-- Input: Destruction Workflow Enhancement
-- Output: Schema change for destruction workflow
-- Pos: db/migration/V71
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 1. 添加保管期限起算日期字段
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS retention_start_date DATE;

COMMENT ON COLUMN acc_archive.retention_start_date IS '保管期限起算日期（用于计算到期时间，默认为归档日期或会计年度结束日期）';

-- 2. 添加销毁状态字段
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS destruction_status VARCHAR(20) DEFAULT 'NORMAL';

COMMENT ON COLUMN acc_archive.destruction_status IS '销毁状态: NORMAL(正常), EXPIRED(到期), APPRAISING(鉴定中), DESTRUCTION_APPROVED(审批通过), DESTROYED(已销毁), FROZEN(冻结), HOLD(保全)';

-- 3. 为到期识别添加索引
CREATE INDEX IF NOT EXISTS idx_archive_retention_expiration 
    ON acc_archive(retention_period, retention_start_date, destruction_status)
    WHERE destruction_status IN ('NORMAL', 'EXPIRED');

-- 4. 初始化 retention_start_date（对于已有数据，使用 archived_at 或 doc_date）
UPDATE acc_archive 
SET retention_start_date = COALESCE(
    DATE(archived_at),
    doc_date,
    (fiscal_year || '-12-31')::DATE
)
WHERE retention_start_date IS NULL;

