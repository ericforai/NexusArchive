-- H2 Schema for Testing
CREATE TABLE IF NOT EXISTS acc_archive (
    id VARCHAR(64) PRIMARY KEY,
    fonds_no VARCHAR(50),
    fiscal_year VARCHAR(4),
    title VARCHAR(1000),
    amount DECIMAL(18,2),
    doc_date DATE,
    counterparty VARCHAR(255),
    voucher_no VARCHAR(100),
    invoice_no VARCHAR(100),
    category_code VARCHAR(50),
    status VARCHAR(20),
    created_time TIMESTAMP,
    last_modified_time TIMESTAMP,
    -- Other fields as needed by Entity, nullable
    security_level VARCHAR(20),
    org_name VARCHAR(500),
    retention_period VARCHAR(10)
);
