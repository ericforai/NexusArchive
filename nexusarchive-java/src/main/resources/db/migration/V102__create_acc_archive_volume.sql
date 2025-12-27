-- Input: Flyway 迁移引擎
-- Output: 创建 acc_archive_volume 表
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- V102: 创建案卷表 (符合 DA/T 104-2024 规范)
CREATE TABLE IF NOT EXISTS acc_archive_volume (
    id VARCHAR(32) PRIMARY KEY,
    volume_code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(255),
    fonds_no VARCHAR(50),
    fiscal_year VARCHAR(4),
    fiscal_period VARCHAR(10),
    category_code VARCHAR(50),
    file_count INTEGER DEFAULT 0,
    retention_period VARCHAR(20),
    status VARCHAR(20) DEFAULT 'draft',
    reviewed_by VARCHAR(32),
    reviewed_at TIMESTAMP,
    archived_at TIMESTAMP,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    custodian_dept VARCHAR(50) DEFAULT 'ACCOUNTING'
);

COMMENT ON TABLE acc_archive_volume IS '案卷信息表';
COMMENT ON COLUMN acc_archive_volume.volume_code IS '案卷号';
COMMENT ON COLUMN acc_archive_volume.status IS '状态: draft-草稿, pending-待审核, archived-已归档';
COMMENT ON COLUMN acc_archive_volume.custodian_dept IS '保管部门: ACCOUNTING-会计, ARCHIVES-档案';

CREATE INDEX IF NOT EXISTS idx_volume_code ON acc_archive_volume(volume_code);
CREATE INDEX IF NOT EXISTS idx_volume_status ON acc_archive_volume(status);
CREATE INDEX IF NOT EXISTS idx_volume_period ON acc_archive_volume(fiscal_period);
