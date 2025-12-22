-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- ============================================
-- V26: OFD 格式转换日志表 (Fix for V15 conflict)
-- 记录 PDF→OFD 等格式转换结果
-- Agent D (基础设施工程师) - 2025-12-07
-- ============================================

-- Ensure table exists (V15 may have created it)
CREATE TABLE IF NOT EXISTS arc_convert_log (
    id              VARCHAR(32) PRIMARY KEY,
    archive_id      VARCHAR(32) NOT NULL,
    source_format   VARCHAR(20) NOT NULL,      -- PDF, JPG, etc.
    target_format   VARCHAR(20) NOT NULL,      -- OFD
    source_path     VARCHAR(500),
    target_path     VARCHAR(500),
    status          VARCHAR(20) NOT NULL,      -- SUCCESS, FAIL
    error_message   TEXT,
    duration_ms     BIGINT,                    -- 转换耗时（毫秒）
    source_size     BIGINT,                    -- 源文件大小
    target_size     BIGINT,                    -- 目标文件大小
    convert_time    TIMESTAMP NOT NULL,
    created_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Improve robustness: Add columns if V15 created table but missed these (V15 had differing columns)
ALTER TABLE arc_convert_log ADD COLUMN IF NOT EXISTS source_size BIGINT;
ALTER TABLE arc_convert_log ADD COLUMN IF NOT EXISTS target_size BIGINT;
ALTER TABLE arc_convert_log ADD COLUMN IF NOT EXISTS duration_ms BIGINT;
ALTER TABLE arc_convert_log ADD COLUMN IF NOT EXISTS convert_time TIMESTAMP;
-- V15 had 'source_format' as VARCHAR(10), V26 wants VARCHAR(20). Let's expand it.
ALTER TABLE arc_convert_log ALTER COLUMN source_format TYPE VARCHAR(20);
ALTER TABLE arc_convert_log ALTER COLUMN target_format TYPE VARCHAR(20);

-- 按档案 ID 查询转换记录
CREATE INDEX IF NOT EXISTS idx_convert_log_archive ON arc_convert_log(archive_id);

-- 按状态查询 (V15 didn't have this)
CREATE INDEX IF NOT EXISTS idx_convert_log_status ON arc_convert_log(status);

-- 按时间查询 (V15 had idx_convert_log_time on created_time, V26 wants convert_time)
-- We'll keep idx_convert_log_time as per V26 name but map to convert_time if we can.
-- Actually V15 named it idx_convert_log_time on created_time.
-- V26 wants idx_convert_log_time on convert_time.
-- This is a conflict. Drop the old index if it exists on the wrong column?
-- Postgres doesn't easily show which column it is.
-- Safe bet: Create idx_convert_log_convert_time
CREATE INDEX IF NOT EXISTS idx_convert_log_convert_time ON arc_convert_log(convert_time);

COMMENT ON TABLE arc_convert_log IS '格式转换日志表';
COMMENT ON COLUMN arc_convert_log.source_format IS '源格式 (PDF, JPG等)';
COMMENT ON COLUMN arc_convert_log.target_format IS '目标格式 (OFD)';
COMMENT ON COLUMN arc_convert_log.status IS '转换状态: SUCCESS, FAIL';
COMMENT ON COLUMN arc_convert_log.duration_ms IS '转换耗时（毫秒）';
