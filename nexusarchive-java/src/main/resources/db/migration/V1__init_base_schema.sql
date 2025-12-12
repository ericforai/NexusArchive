-- V1: 初始化基础表结构 (补全缺失的 Base Schema)
-- 包含: acc_archive, bas_fonds, acc_archive_relation, audit_inspection_log等

-- 1. 全宗表 (bas_fonds)
CREATE TABLE IF NOT EXISTS bas_fonds (
    id VARCHAR(32) PRIMARY KEY,
    fonds_code VARCHAR(50) NOT NULL UNIQUE,
    fonds_name VARCHAR(100) NOT NULL,
    company_name VARCHAR(100),
    description VARCHAR(500),
    created_by VARCHAR(32),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE bas_fonds IS '全宗基础信息表';

-- 2. 电子会计档案表 (acc_archive)
-- 注意：部分字段可能在后续 V10/V11 中再次添加 (如 unique_biz_id)，此处为了兼容性包含基本字段
CREATE TABLE IF NOT EXISTS acc_archive (
    id VARCHAR(32) PRIMARY KEY,
    fonds_no VARCHAR(50) NOT NULL,
    archive_code VARCHAR(100) NOT NULL UNIQUE,
    category_code VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    fiscal_year VARCHAR(4) NOT NULL,
    fiscal_period VARCHAR(10),
    retention_period VARCHAR(10) NOT NULL,
    org_name VARCHAR(100) NOT NULL,
    creator VARCHAR(50),
    status VARCHAR(20) DEFAULT 'draft',
    amount DECIMAL(18, 2),
    doc_date DATE,
    unique_biz_id VARCHAR(64),
    standard_metadata JSONB,
    custom_metadata JSONB,
    security_level VARCHAR(20) DEFAULT 'internal',
    location VARCHAR(200),
    department_id VARCHAR(32),
    created_by VARCHAR(32),
    fixity_value VARCHAR(128),
    fixity_algo VARCHAR(20),
    volume_id VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
COMMENT ON TABLE acc_archive IS '电子会计档案表';

CREATE INDEX IF NOT EXISTS idx_archive_fonds_year ON acc_archive(fonds_no, fiscal_year);
CREATE INDEX IF NOT EXISTS idx_archive_code ON acc_archive(archive_code);
CREATE INDEX IF NOT EXISTS idx_archive_category ON acc_archive(category_code);
CREATE INDEX IF NOT EXISTS idx_archive_status ON acc_archive(status);

-- 3. 档案关联关系表 (acc_archive_relation)
CREATE TABLE IF NOT EXISTS acc_archive_relation (
    id VARCHAR(32) PRIMARY KEY,
    source_id VARCHAR(32) NOT NULL,
    target_id VARCHAR(32) NOT NULL,
    relation_type VARCHAR(50) NOT NULL,
    relation_desc VARCHAR(255),
    created_by VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    FOREIGN KEY (source_id) REFERENCES acc_archive(id) ON DELETE CASCADE,
    FOREIGN KEY (target_id) REFERENCES acc_archive(id) ON DELETE CASCADE
);
COMMENT ON TABLE acc_archive_relation IS '档案关联关系表';

-- 4. 四性检测日志表 (audit_inspection_log)
CREATE TABLE IF NOT EXISTS audit_inspection_log (
    id VARCHAR(32) PRIMARY KEY,
    archive_id VARCHAR(32) NOT NULL,
    inspection_stage VARCHAR(20) NOT NULL,
    inspection_time TIMESTAMP NOT NULL,
    inspector_id VARCHAR(32),
    is_authentic BOOLEAN NOT NULL,
    is_complete BOOLEAN NOT NULL,
    is_available BOOLEAN NOT NULL,
    is_secure BOOLEAN NOT NULL,
    hash_snapshot VARCHAR(128),
    integrity_check JSONB,
    authenticity_check JSONB,
    availability_check JSONB,
    security_check JSONB,
    check_result VARCHAR(20) NOT NULL,
    detail_report JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    report_file_path VARCHAR(500),
    report_file_hash VARCHAR(100),
    FOREIGN KEY (archive_id) REFERENCES acc_archive(id) ON DELETE CASCADE
);
COMMENT ON TABLE audit_inspection_log IS '四性检测日志表';

-- 5. 档号计数器 (sys_archival_code_sequence)
CREATE TABLE IF NOT EXISTS sys_archival_code_sequence (
    fonds_code VARCHAR(50) NOT NULL,
    fiscal_year VARCHAR(4) NOT NULL,
    category_code VARCHAR(10) NOT NULL,
    current_val INT DEFAULT 0,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (fonds_code, fiscal_year, category_code)
);
COMMENT ON TABLE sys_archival_code_sequence IS '档号生成计数器';
