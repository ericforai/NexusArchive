-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 集成中心 Phase 4: 三位一体核对引擎数据表
-- 财务账、归档凭证、原始证据一致性核对记录

CREATE TABLE IF NOT EXISTS arc_reconciliation_record (
    id VARCHAR(64) PRIMARY KEY,
    fonds_code VARCHAR(50) NOT NULL,                    -- 全宗号
    fiscal_year VARCHAR(4) NOT NULL,                    -- 会计年度
    fiscal_period VARCHAR(2) NOT NULL,                  -- 会计期间
    subject_code VARCHAR(50),                           -- 科目代码
    subject_name VARCHAR(100),                          -- 科目名称
    
    -- ERP 侧汇总数据 (账)
    erp_debit_total DECIMAL(18, 2) DEFAULT 0,
    erp_credit_total DECIMAL(18, 2) DEFAULT 0,
    erp_voucher_count INT DEFAULT 0,
    
    -- 档案系统侧汇总数据 (凭)
    arc_debit_total DECIMAL(18, 2) DEFAULT 0,
    arc_credit_total DECIMAL(18, 2) DEFAULT 0,
    arc_voucher_count INT DEFAULT 0,
    
    -- 原始证据核对 (证)
    attachment_count INT DEFAULT 0,                     -- 附件总数
    attachment_missing_count INT DEFAULT 0,             -- 缺失附件的凭证数
    
    -- 核对结果
    recon_status VARCHAR(20) NOT NULL,                  -- SUCCESS, DISCREPANCY, ERROR
    recon_message TEXT,                                 -- 差异说明或错误信息
    recon_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    operator_id VARCHAR(64),
    
    -- 关联审计信息
    snapshot_data JSONB,                                -- 核对时的快照数据
    source_system VARCHAR(100)                          -- 来源 ERP 系统名称
);

COMMENT ON TABLE arc_reconciliation_record IS '财务账、凭证与附件一致性核对记录表';
COMMENT ON COLUMN arc_reconciliation_record.erp_debit_total IS 'ERP侧借方合计';
COMMENT ON COLUMN arc_reconciliation_record.arc_debit_total IS '档案系统侧借方合计';
COMMENT ON COLUMN arc_reconciliation_record.recon_status IS '核对状态: SUCCESS(通过), DISCREPANCY(有差异), ERROR(异常)';
