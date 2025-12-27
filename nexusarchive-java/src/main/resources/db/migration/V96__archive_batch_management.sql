-- =====================================================
-- V96: 归档批次管理
-- 功能: 实现从预归档库到正式档案库的批次归档能力
-- 作者: Claude Code
-- 日期: 2025-12-26
-- =====================================================

-- 1. 归档批次表
CREATE TABLE IF NOT EXISTS archive_batch (
    id BIGSERIAL PRIMARY KEY,
    batch_no VARCHAR(32) NOT NULL,
    fonds_id BIGINT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    scope_type VARCHAR(20) DEFAULT 'PERIOD',  -- PERIOD: 按期间, CUSTOM: 自定义范围
    status VARCHAR(20) DEFAULT 'PENDING',      -- PENDING/VALIDATING/APPROVED/ARCHIVED/REJECTED/FAILED

    -- 统计信息
    voucher_count INT DEFAULT 0,
    doc_count INT DEFAULT 0,
    file_count INT DEFAULT 0,
    total_size_bytes BIGINT DEFAULT 0,

    -- 校验报告
    validation_report JSONB,    -- 归档前校验报告
    integrity_report JSONB,     -- 四性检测报告
    error_message TEXT,         -- 错误信息

    -- 审批信息
    submitted_by BIGINT,
    submitted_at TIMESTAMP,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    approval_comment TEXT,

    -- 归档执行
    archived_at TIMESTAMP,
    archived_by BIGINT,

    -- 时间戳
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT uk_archive_batch_no UNIQUE (batch_no)
);

-- 2. 归档批次条目表（记录批次包含的凭证/单据）
CREATE TABLE IF NOT EXISTS archive_batch_item (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    item_type VARCHAR(32) NOT NULL,    -- VOUCHER: 记账凭证, SOURCE_DOC: 原始单据
    ref_id VARCHAR(64) NOT NULL,       -- 引用 ID (arc_file_content.id 或 original_voucher.id)
    ref_no VARCHAR(64),                -- 凭证号/单据号
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING/VALIDATED/ARCHIVED/FAILED
    validation_result JSONB,           -- 校验结果
    hash_sm3 VARCHAR(64),              -- 归档时计算的哈希
    created_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT fk_batch_item_batch FOREIGN KEY (batch_id)
        REFERENCES archive_batch(id) ON DELETE CASCADE
);

-- 3. 期间锁定表
CREATE TABLE IF NOT EXISTS period_lock (
    id BIGSERIAL PRIMARY KEY,
    fonds_id BIGINT NOT NULL,
    period VARCHAR(7) NOT NULL,        -- 2024-01
    lock_type VARCHAR(20) NOT NULL,    -- ERP_CLOSED: ERP结账, ARCHIVED: 已归档, AUDIT_LOCKED: 审计锁定
    locked_at TIMESTAMP NOT NULL,
    locked_by BIGINT,
    unlock_at TIMESTAMP,               -- 解锁时间（如果允许解锁）
    unlock_by BIGINT,
    reason TEXT,

    CONSTRAINT uk_period_lock UNIQUE (fonds_id, period, lock_type)
);

-- 4. 四性检测结果表
CREATE TABLE IF NOT EXISTS integrity_check (
    id BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(32) NOT NULL,  -- BATCH: 批次, ARCHIVE: 档案, FILE: 文件, VOUCHER: 凭证
    target_id BIGINT NOT NULL,
    check_type VARCHAR(32) NOT NULL,   -- AUTHENTICITY: 真实性, INTEGRITY: 完整性, USABILITY: 可用性, SECURITY: 安全性
    result VARCHAR(20) NOT NULL,       -- PASS: 通过, FAIL: 失败, WARNING: 警告
    hash_expected VARCHAR(64),
    hash_actual VARCHAR(64),
    signature_valid BOOLEAN,
    details JSONB,                     -- 详细检测信息
    checked_at TIMESTAMP DEFAULT NOW(),
    checked_by BIGINT
);

-- 5. 归档后更正记录表
CREATE TABLE IF NOT EXISTS archive_amendment (
    id BIGSERIAL PRIMARY KEY,
    archive_id BIGINT NOT NULL,        -- 关联的档案 ID
    batch_id BIGINT,                   -- 关联的批次 ID
    amendment_type VARCHAR(20) NOT NULL,  -- CORRECTION: 更正, SUPPLEMENT: 补充, ANNOTATION: 备注
    reason TEXT NOT NULL,
    original_content JSONB,
    amended_content JSONB,
    attachment_ids BIGINT[],
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING/APPROVED/REJECTED
    approved_by BIGINT,
    approved_at TIMESTAMP,
    approval_comment TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 6. 为现有 acc_archive 表添加批次关联字段
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS batch_id BIGINT;
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP;

-- 7. 索引
CREATE INDEX IF NOT EXISTS idx_archive_batch_fonds ON archive_batch(fonds_id);
CREATE INDEX IF NOT EXISTS idx_archive_batch_period ON archive_batch(period_start, period_end);
CREATE INDEX IF NOT EXISTS idx_archive_batch_status ON archive_batch(status);
CREATE INDEX IF NOT EXISTS idx_archive_batch_created ON archive_batch(created_at);

CREATE INDEX IF NOT EXISTS idx_batch_item_batch ON archive_batch_item(batch_id);
CREATE INDEX IF NOT EXISTS idx_batch_item_type_ref ON archive_batch_item(item_type, ref_id);

CREATE INDEX IF NOT EXISTS idx_period_lock_fonds ON period_lock(fonds_id);
CREATE INDEX IF NOT EXISTS idx_period_lock_period ON period_lock(period);

CREATE INDEX IF NOT EXISTS idx_integrity_check_target ON integrity_check(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_integrity_check_result ON integrity_check(result);

CREATE INDEX IF NOT EXISTS idx_archive_amendment_archive ON archive_amendment(archive_id);
CREATE INDEX IF NOT EXISTS idx_archive_amendment_batch ON archive_amendment(batch_id);

CREATE INDEX IF NOT EXISTS idx_acc_archive_batch_id ON acc_archive(batch_id);

-- 8. 批次号生成序列
CREATE SEQUENCE IF NOT EXISTS archive_batch_seq START WITH 1;

-- 9. 注释
COMMENT ON TABLE archive_batch IS '归档批次表 - 管理从预归档库到正式档案库的批量归档';
COMMENT ON TABLE archive_batch_item IS '归档批次条目表 - 记录批次包含的凭证和单据';
COMMENT ON TABLE period_lock IS '期间锁定表 - 控制会计期间的修改权限';
COMMENT ON TABLE integrity_check IS '四性检测结果表 - 真实性、完整性、可用性、安全性检测';
COMMENT ON TABLE archive_amendment IS '归档更正记录表 - 归档后的更正/补充/备注';

COMMENT ON COLUMN archive_batch.scope_type IS '范围类型: PERIOD-按期间, CUSTOM-自定义';
COMMENT ON COLUMN archive_batch.status IS '状态: PENDING-待提交, VALIDATING-校验中, APPROVED-已审批, ARCHIVED-已归档, REJECTED-已驳回, FAILED-失败';
COMMENT ON COLUMN period_lock.lock_type IS '锁定类型: ERP_CLOSED-ERP结账, ARCHIVED-已归档, AUDIT_LOCKED-审计锁定';
COMMENT ON COLUMN integrity_check.check_type IS '检测类型: AUTHENTICITY-真实性, INTEGRITY-完整性, USABILITY-可用性, SECURITY-安全性';
