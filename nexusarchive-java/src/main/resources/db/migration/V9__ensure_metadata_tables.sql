-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- Ensure tables exist (Fix for missing V3 tables)

CREATE TABLE IF NOT EXISTS arc_file_content (
    id VARCHAR(64) PRIMARY KEY,
    archival_code VARCHAR(100) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_size BIGINT NOT NULL,
    file_hash VARCHAR(128),
    hash_algorithm VARCHAR(20),
    storage_path VARCHAR(500) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS arc_file_metadata_index (
    id VARCHAR(64) PRIMARY KEY,
    file_id VARCHAR(64) NOT NULL,
    invoice_code VARCHAR(50),
    invoice_number VARCHAR(50),
    total_amount DECIMAL(18,2),
    seller_name VARCHAR(200),
    issue_date DATE,
    parsed_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    parser_type VARCHAR(50)
);
