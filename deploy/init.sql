-- Input: PRD Schema Definition
-- Output: Database Initialization Script
-- Pos: NexusArchive deploy/init.sql
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 1. 法人实体表
CREATE TABLE IF NOT EXISTS sys_entity (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    tax_code VARCHAR(50) UNIQUE,
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

-- 2. 全宗表
CREATE TABLE IF NOT EXISTS sys_fonds (
    id VARCHAR(32) PRIMARY KEY,
    fonds_no VARCHAR(50) NOT NULL UNIQUE,
    fonds_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(32),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    valid_from DATE,
    valid_to DATE
);

-- 3. 保管期限表
CREATE TABLE IF NOT EXISTS retention_policy (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    years INT NOT NULL,
    is_permanent BOOLEAN DEFAULT FALSE
);

-- 4. 档案主表
CREATE TABLE IF NOT EXISTS acc_archive (
    id VARCHAR(32) NOT NULL,
    fonds_no VARCHAR(50) NOT NULL,
    fiscal_year VARCHAR(20) NOT NULL,
    category_code VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    doc_date DATE,
    amount DECIMAL(18, 2),
    counterparty VARCHAR(100),
    voucher_no VARCHAR(50),
    invoice_no VARCHAR(50),
    status VARCHAR(20) DEFAULT 'NORMAL',
    security_level VARCHAR(20) DEFAULT 'INTERNAL',
    metadata_ext TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- 5. 档案文件内容表
CREATE TABLE IF NOT EXISTS arc_file_content (
    id VARCHAR(32) PRIMARY KEY,
    item_id VARCHAR(32) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(20),
    file_size BIGINT,
    file_hash VARCHAR(128),
    hash_algorithm VARCHAR(20),
    storage_path VARCHAR(512) NOT NULL,
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. 审计日志
CREATE TABLE IF NOT EXISTS arc_preservation_audit (
    id BIGSERIAL PRIMARY KEY,
    archive_id VARCHAR(32) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    operator VARCHAR(50),
    check_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    overall_status VARCHAR(20),
    check_result_json TEXT
);

-- Initial Data
INSERT INTO retention_policy (id, name, years, is_permanent) VALUES
('RP001', '10 Years', 10, FALSE),
('RP002', '30 Years', 30, FALSE),
('RP003', 'Permanent', 100, TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_entity (id, name, tax_code) VALUES
('ENT001', 'Nexus Corp', '91110000XXXXXX')
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_fonds (id, fonds_no, fonds_name, entity_id) VALUES
('F001', 'FONDS-001', 'Nexus Main Fonds', 'ENT001')
ON CONFLICT (id) DO NOTHING;
