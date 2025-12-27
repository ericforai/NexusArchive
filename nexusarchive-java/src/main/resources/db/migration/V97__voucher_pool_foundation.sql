-- ====================================================
-- V97: 原始凭证单据池功能增强
-- Reference: Implementation Plan (Expert Reviewed)
-- 支持：状态机、事件溯源、批次管理、乐观锁
-- ====================================================

-- ===== 1. 增强 arc_original_voucher 表 =====

-- 1.1 添加细化的池状态字段（状态机核心）
-- 与 archive_status 区分：archive_status 代表归档库状态，pool_status 代表预归档池状态
ALTER TABLE arc_original_voucher 
ADD COLUMN IF NOT EXISTS pool_status VARCHAR(20) DEFAULT 'ENTRY';

COMMENT ON COLUMN arc_original_voucher.pool_status IS '单据池状态: ENTRY(入池), PARSED(已解析), PARSE_FAILED(解析失败), MATCHED(已关联), ARCHIVED(已归档)';

-- 1.2 添加乐观锁字段（并发控制）
ALTER TABLE arc_original_voucher 
ADD COLUMN IF NOT EXISTS row_version INT DEFAULT 1;

COMMENT ON COLUMN arc_original_voucher.row_version IS '乐观锁版本号，每次更新递增';

-- 1.3 添加批次关联字段
ALTER TABLE arc_original_voucher 
ADD COLUMN IF NOT EXISTS pool_batch_id VARCHAR(64);

COMMENT ON COLUMN arc_original_voucher.pool_batch_id IS '导入批次ID，关联 arc_import_batch';

-- 1.4 添加解析结果字段
ALTER TABLE arc_original_voucher 
ADD COLUMN IF NOT EXISTS parsed_payload JSONB;

ALTER TABLE arc_original_voucher 
ADD COLUMN IF NOT EXISTS parsed_at TIMESTAMP;

COMMENT ON COLUMN arc_original_voucher.parsed_payload IS 'OCR/智能解析结果（JSON格式）';
COMMENT ON COLUMN arc_original_voucher.parsed_at IS '解析完成时间';

-- 1.5 添加关联结果字段
ALTER TABLE arc_original_voucher 
ADD COLUMN IF NOT EXISTS matched_voucher_id VARCHAR(64);

ALTER TABLE arc_original_voucher 
ADD COLUMN IF NOT EXISTS matched_at TIMESTAMP;

COMMENT ON COLUMN arc_original_voucher.matched_voucher_id IS '关联的记账凭证ID';
COMMENT ON COLUMN arc_original_voucher.matched_at IS '关联完成时间';

-- 1.6 添加软删除增强字段
ALTER TABLE arc_original_voucher 
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

ALTER TABLE arc_original_voucher 
ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(64);

ALTER TABLE arc_original_voucher 
ADD COLUMN IF NOT EXISTS delete_reason TEXT;

-- 1.7 添加 tenant_id（多租户/账套隔离）
ALTER TABLE arc_original_voucher 
ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 1;

COMMENT ON COLUMN arc_original_voucher.tenant_id IS '租户/账套ID，用于数据隔离';

-- 1.8 创建池状态索引
CREATE INDEX IF NOT EXISTS idx_ov_pool_status ON arc_original_voucher(pool_status);
CREATE INDEX IF NOT EXISTS idx_ov_batch ON arc_original_voucher(pool_batch_id);
CREATE INDEX IF NOT EXISTS idx_ov_tenant ON arc_original_voucher(tenant_id);


-- ===== 2. 创建导入批次表 =====
CREATE TABLE IF NOT EXISTS arc_import_batch (
    id                  VARCHAR(64)     PRIMARY KEY,
    batch_no            VARCHAR(64)     NOT NULL UNIQUE,
    tenant_id           BIGINT          DEFAULT 1,
    source_system       VARCHAR(50)     NOT NULL,
    voucher_type        VARCHAR(32),
    status              VARCHAR(20)     DEFAULT 'UPLOADING',
    total_files         INT             DEFAULT 0,
    success_count       INT             DEFAULT 0,
    failed_count        INT             DEFAULT 0,
    error_details       JSONB,
    rollback_reason     TEXT,
    created_by          VARCHAR(64),
    created_time        TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    completed_time      TIMESTAMP,
    CONSTRAINT chk_batch_status CHECK (status IN ('UPLOADING', 'COMPLETED', 'FAILED', 'ROLLED_BACK'))
);

COMMENT ON TABLE arc_import_batch IS '原始凭证导入批次表 - 支持批量导入追踪与回滚';
COMMENT ON COLUMN arc_import_batch.batch_no IS '批次编号，格式: IMP-{年月日}-{序号}';
COMMENT ON COLUMN arc_import_batch.status IS '批次状态: UPLOADING(上传中), COMPLETED(完成), FAILED(失败), ROLLED_BACK(已回滚)';

CREATE INDEX IF NOT EXISTS idx_batch_status ON arc_import_batch(status);
CREATE INDEX IF NOT EXISTS idx_batch_tenant ON arc_import_batch(tenant_id);


-- ===== 3. 创建事件溯源表 =====
CREATE TABLE IF NOT EXISTS arc_original_voucher_event (
    id                  BIGSERIAL       PRIMARY KEY,
    voucher_id          VARCHAR(64)     NOT NULL,
    from_status         VARCHAR(20),
    to_status           VARCHAR(20)     NOT NULL,
    action              VARCHAR(50)     NOT NULL,
    actor_type          VARCHAR(20)     DEFAULT 'USER',
    actor_id            VARCHAR(64),
    actor_name          VARCHAR(100),
    occurred_at         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    request_id          VARCHAR(100),
    client_ip           VARCHAR(50),
    reason              TEXT,
    details             JSONB,
    CONSTRAINT fk_event_voucher FOREIGN KEY (voucher_id) REFERENCES arc_original_voucher(id) ON DELETE CASCADE,
    CONSTRAINT chk_action CHECK (action IN ('UPLOAD', 'PARSE', 'PARSE_RETRY', 'MATCH', 'UNMATCH', 'ARCHIVE', 'DELETE', 'RESTORE', 'ROLLBACK', 'MOVE_TYPE'))
);

COMMENT ON TABLE arc_original_voucher_event IS '原始凭证事件溯源表 - 完整记录每一步状态变更';
COMMENT ON COLUMN arc_original_voucher_event.action IS '操作类型: UPLOAD(上传), PARSE(解析), MATCH(关联), ARCHIVE(归档), DELETE(删除)等';
COMMENT ON COLUMN arc_original_voucher_event.actor_type IS '操作者类型: USER(用户), SYSTEM(系统任务)';

CREATE INDEX IF NOT EXISTS idx_event_voucher ON arc_original_voucher_event(voucher_id);
CREATE INDEX IF NOT EXISTS idx_event_action ON arc_original_voucher_event(action);
CREATE INDEX IF NOT EXISTS idx_event_time ON arc_original_voucher_event(occurred_at);


-- ===== 4. 数据迁移：存量数据状态刷新 =====
-- 将所有已存在的原始凭证（无 pool_status）默认设置为 ARCHIVED
UPDATE arc_original_voucher 
SET pool_status = 'ARCHIVED' 
WHERE pool_status IS NULL OR pool_status = 'ENTRY';

-- 将 archive_status = 'ARCHIVED' 的记录同步 pool_status
UPDATE arc_original_voucher 
SET pool_status = 'ARCHIVED' 
WHERE archive_status = 'ARCHIVED' AND pool_status != 'ARCHIVED';


-- ===== 5. 添加外键约束 =====
-- 延迟添加外键，避免迁移失败
ALTER TABLE arc_original_voucher 
ADD CONSTRAINT fk_ov_batch FOREIGN KEY (pool_batch_id) REFERENCES arc_import_batch(id) ON DELETE SET NULL;


-- ===== 6. 创建触发器：自动递增 row_version =====
CREATE OR REPLACE FUNCTION update_row_version()
RETURNS TRIGGER AS $$
BEGIN
    NEW.row_version = OLD.row_version + 1;
    NEW.last_modified_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_ov_row_version ON arc_original_voucher;

CREATE TRIGGER trg_ov_row_version
BEFORE UPDATE ON arc_original_voucher
FOR EACH ROW
EXECUTE FUNCTION update_row_version();

COMMENT ON FUNCTION update_row_version() IS '自动递增乐观锁版本号';
