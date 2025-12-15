-- ============================================================
-- V31: 添加 Entity-Schema 验证中发现的缺失列
-- 涉及表: sys_setting, acc_archive_volume
-- 参考: DA/T 94-2022, DA/T 104-2024
-- ============================================================

-- ============================================
-- 1. sys_setting 表补充列
-- ============================================

-- 分组/类别 (如 system/storage/retention)
-- Ensure table exists first
CREATE TABLE IF NOT EXISTS sys_setting (
    id VARCHAR(64) PRIMARY KEY,
    config_key VARCHAR(128),
    config_value TEXT,
    description VARCHAR(512),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE sys_setting ADD COLUMN IF NOT EXISTS category VARCHAR(64);
COMMENT ON COLUMN sys_setting.category IS '配置分组/类别';

-- 创建时间
ALTER TABLE sys_setting ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN sys_setting.created_at IS '创建时间';

-- 逻辑删除标记
ALTER TABLE sys_setting ADD COLUMN IF NOT EXISTS deleted INTEGER DEFAULT 0;
COMMENT ON COLUMN sys_setting.deleted IS '逻辑删除标记: 0=正常, 1=已删除';

-- ============================================
-- 2. acc_archive_volume 表补充列
-- ============================================

-- 案卷标题
ALTER TABLE acc_archive_volume ADD COLUMN IF NOT EXISTS title VARCHAR(512);
COMMENT ON COLUMN acc_archive_volume.title IS '案卷标题 (格式: 责任者+年度+月度+业务子系统+业务单据名称)';

-- 全宗号
ALTER TABLE acc_archive_volume ADD COLUMN IF NOT EXISTS fonds_no VARCHAR(64);
COMMENT ON COLUMN acc_archive_volume.fonds_no IS '全宗号';

-- 会计年度
ALTER TABLE acc_archive_volume ADD COLUMN IF NOT EXISTS fiscal_year VARCHAR(10);
COMMENT ON COLUMN acc_archive_volume.fiscal_year IS '会计年度';

-- 会计期间
ALTER TABLE acc_archive_volume ADD COLUMN IF NOT EXISTS fiscal_period VARCHAR(10);
COMMENT ON COLUMN acc_archive_volume.fiscal_period IS '会计期间 (YYYY-MM)';

-- 分类代号
ALTER TABLE acc_archive_volume ADD COLUMN IF NOT EXISTS category_code VARCHAR(20);
COMMENT ON COLUMN acc_archive_volume.category_code IS '分类代号 (AC01=凭证, AC02=账簿, AC03=报告)';

-- 卷内文件数
ALTER TABLE acc_archive_volume ADD COLUMN IF NOT EXISTS file_count INTEGER DEFAULT 0;
COMMENT ON COLUMN acc_archive_volume.file_count IS '卷内文件数';

-- 审核人ID
ALTER TABLE acc_archive_volume ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(64);
COMMENT ON COLUMN acc_archive_volume.reviewed_by IS '审核人ID';

-- 审核时间
ALTER TABLE acc_archive_volume ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
COMMENT ON COLUMN acc_archive_volume.reviewed_at IS '审核时间';

-- 归档时间
ALTER TABLE acc_archive_volume ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP;
COMMENT ON COLUMN acc_archive_volume.archived_at IS '归档时间';

-- ============================================
-- 索引优化 (方便按条件查询)
-- ============================================

-- sys_setting 按 category 查询
CREATE INDEX IF NOT EXISTS idx_sys_setting_category ON sys_setting(category);

-- acc_archive_volume 多条件索引
CREATE INDEX IF NOT EXISTS idx_volume_fonds_year ON acc_archive_volume(fonds_no, fiscal_year);
CREATE INDEX IF NOT EXISTS idx_volume_category ON acc_archive_volume(category_code);
CREATE INDEX IF NOT EXISTS idx_volume_fiscal_period ON acc_archive_volume(fiscal_period);
