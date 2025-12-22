-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

CREATE TABLE IF NOT EXISTS arc_convert_log (
    id VARCHAR(32) PRIMARY KEY,
    archive_id VARCHAR(32) NOT NULL,
    source_format VARCHAR(10) NOT NULL,
    target_format VARCHAR(10) NOT NULL,
    source_path VARCHAR(500),
    target_path VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    file_size_bytes BIGINT,
    convert_duration_ms INTEGER,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_convert_log_archive ON arc_convert_log(archive_id);
CREATE INDEX IF NOT EXISTS idx_convert_log_time ON arc_convert_log(created_time);
