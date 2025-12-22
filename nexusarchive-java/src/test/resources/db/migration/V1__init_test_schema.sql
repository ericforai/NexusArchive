-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- V1__init_test_schema.sql
-- Base schema for Testing H2 Database

CREATE TABLE IF NOT EXISTS acc_archive (
    id VARCHAR(64) PRIMARY KEY,
    fonds_no VARCHAR(64),
    archive_code VARCHAR(128),
    category_code VARCHAR(32),
    title VARCHAR(512),
    fiscal_year VARCHAR(10),
    fiscal_period VARCHAR(10),
    retention_period VARCHAR(32),
    org_name VARCHAR(128),
    creator VARCHAR(128),
    summary VARCHAR(1024),
    status VARCHAR(32),
    standard_metadata TEXT,
    custom_metadata TEXT,
    security_level VARCHAR(32),
    location VARCHAR(255),
    department_id VARCHAR(64),
    created_by VARCHAR(64),
    fixity_value VARCHAR(128), -- Needed for V4 drop
    fixity_algo VARCHAR(32),
    unique_biz_id VARCHAR(128),
    amount DECIMAL(18,2),
    doc_date DATE,
    volume_id VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paper_ref_link VARCHAR(255),
    destruction_hold BOOLEAN,
    hold_reason VARCHAR(512),
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGSERIAL PRIMARY KEY,
    operation_type VARCHAR(64),
    operator_id VARCHAR(64),
    operator_name VARCHAR(128),
    client_ip VARCHAR(64),
    operation_desc TEXT,
    operation_time TIMESTAMP,
    module_name VARCHAR(64),
    status INT,
    error_msg TEXT,
    details TEXT
);
