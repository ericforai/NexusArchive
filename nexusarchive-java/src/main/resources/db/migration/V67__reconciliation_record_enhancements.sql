-- Phase 4.1: 增强核对记录的可追溯性与幂等约束

ALTER TABLE arc_reconciliation_record
    ADD COLUMN IF NOT EXISTS config_id BIGINT,
    ADD COLUMN IF NOT EXISTS accbook_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS recon_start_date DATE,
    ADD COLUMN IF NOT EXISTS recon_end_date DATE;

COMMENT ON COLUMN arc_reconciliation_record.config_id IS 'ERP配置ID';
COMMENT ON COLUMN arc_reconciliation_record.accbook_code IS '账套代码';
COMMENT ON COLUMN arc_reconciliation_record.recon_start_date IS '核对开始日期';
COMMENT ON COLUMN arc_reconciliation_record.recon_end_date IS '核对结束日期';

CREATE INDEX IF NOT EXISTS idx_recon_record_config ON arc_reconciliation_record(config_id);
CREATE INDEX IF NOT EXISTS idx_recon_record_range ON arc_reconciliation_record(recon_start_date, recon_end_date);

-- 幂等性唯一约束 (仅对新记录生效)
CREATE UNIQUE INDEX IF NOT EXISTS uq_recon_record_key
    ON arc_reconciliation_record(config_id, subject_code, recon_start_date, recon_end_date)
    WHERE config_id IS NOT NULL AND recon_start_date IS NOT NULL AND recon_end_date IS NOT NULL;
