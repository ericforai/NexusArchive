-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- V10: Compliance Schema Update (DA/T 94-2022)

-- 1. Create acc_archive_volume table (The "Folder")
CREATE TABLE IF NOT EXISTS acc_archive_volume (
    id VARCHAR(32) PRIMARY KEY,
    volume_code VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    fonds_no VARCHAR(50),
    fiscal_year VARCHAR(4),
    fiscal_period VARCHAR(10),
    category_code VARCHAR(50),
    file_count INT DEFAULT 0,
    retention_period VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'draft',
    reviewed_by VARCHAR(32),
    reviewed_at TIMESTAMP,
    archived_at TIMESTAMP,
    custodian_dept VARCHAR(32) DEFAULT 'ACCOUNTING',
    validation_report_path VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

COMMENT ON TABLE acc_archive_volume IS '案卷表 (虚拟装订)';
COMMENT ON COLUMN acc_archive_volume.volume_code IS '案卷号';
COMMENT ON COLUMN acc_archive_volume.title IS '案卷标题 (新增)';
COMMENT ON COLUMN acc_archive_volume.fiscal_year IS '会计年度 (原 archive_year)';
COMMENT ON COLUMN acc_archive_volume.retention_period IS '保管期限';
COMMENT ON COLUMN acc_archive_volume.status IS '状态: draft, pending, archived';
COMMENT ON COLUMN acc_archive_volume.validation_report_path IS '四性检测报告路径';

-- 2. Update acc_archive table (The "Item")
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS unique_biz_id VARCHAR(64);
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS amount DECIMAL(18, 2);
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS doc_date DATE;
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS volume_id VARCHAR(32);

COMMENT ON COLUMN acc_archive.unique_biz_id IS '唯一单据号 (关联ERP/OA)';
COMMENT ON COLUMN acc_archive.amount IS '金额';
COMMENT ON COLUMN acc_archive.doc_date IS '业务日期';
COMMENT ON COLUMN acc_archive.volume_id IS '所属案卷ID';

CREATE INDEX IF NOT EXISTS idx_archive_unique_biz_id ON acc_archive(unique_biz_id);
CREATE INDEX IF NOT EXISTS idx_archive_volume_id ON acc_archive(volume_id);

-- 3. Update arc_file_content table (The "File Entity")
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS item_id VARCHAR(32);
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS original_hash VARCHAR(128);
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS current_hash VARCHAR(128);
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS timestamp_token BYTEA;
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS sign_value BYTEA;

-- Backfill original_hash from existing file_hash if empty
UPDATE arc_file_content SET original_hash = file_hash WHERE original_hash IS NULL;

COMMENT ON COLUMN arc_file_content.item_id IS '关联单据ID';
COMMENT ON COLUMN arc_file_content.original_hash IS '原始哈希值 (接收时)';
COMMENT ON COLUMN arc_file_content.current_hash IS '当前哈希值 (巡检时)';
COMMENT ON COLUMN arc_file_content.timestamp_token IS '时间戳Token';
COMMENT ON COLUMN arc_file_content.sign_value IS '电子签名值';

CREATE INDEX IF NOT EXISTS idx_file_item_id ON arc_file_content(item_id);
