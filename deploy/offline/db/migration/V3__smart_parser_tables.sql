-- SmartParserService 数据库表
-- 创建时间: 2025-11-22

-- 1. 电子文件内容表
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

COMMENT ON TABLE arc_file_content IS '电子文件存储记录表';
COMMENT ON COLUMN arc_file_content.id IS '文件ID';
COMMENT ON COLUMN arc_file_content.archival_code IS '档号';
COMMENT ON COLUMN arc_file_content.file_name IS '文件名';
COMMENT ON COLUMN arc_file_content.file_type IS '文件类型 (PDF/OFD/XML)';
COMMENT ON COLUMN arc_file_content.file_size IS '文件大小(字节)';
COMMENT ON COLUMN arc_file_content.file_hash IS '文件哈希值';
COMMENT ON COLUMN arc_file_content.hash_algorithm IS '哈希算法 (SM3/SHA256)';
COMMENT ON COLUMN arc_file_content.storage_path IS '存储路径';
COMMENT ON COLUMN arc_file_content.created_time IS '创建时间';

CREATE INDEX IF NOT EXISTS idx_archival_code ON arc_file_content(archival_code);
CREATE INDEX IF NOT EXISTS idx_file_type ON arc_file_content(file_type);
CREATE INDEX IF NOT EXISTS idx_created_time ON arc_file_content(created_time);

-- 2. 文件元数据索引表
CREATE TABLE IF NOT EXISTS arc_file_metadata_index (
    id VARCHAR(64) PRIMARY KEY,
    file_id VARCHAR(64) NOT NULL,
    invoice_code VARCHAR(50),
    invoice_number VARCHAR(50),
    total_amount DECIMAL(18,2),
    seller_name VARCHAR(200),
    issue_date DATE,
    parsed_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    parser_type VARCHAR(50),
    FOREIGN KEY (file_id) REFERENCES arc_file_content(id) ON DELETE CASCADE
);

COMMENT ON TABLE arc_file_metadata_index IS '智能解析元数据索引表';
COMMENT ON COLUMN arc_file_metadata_index.id IS '索引ID';
COMMENT ON COLUMN arc_file_metadata_index.file_id IS '文件ID';
COMMENT ON COLUMN arc_file_metadata_index.invoice_code IS '发票代码';
COMMENT ON COLUMN arc_file_metadata_index.invoice_number IS '发票号码';
COMMENT ON COLUMN arc_file_metadata_index.total_amount IS '价税合计';
COMMENT ON COLUMN arc_file_metadata_index.seller_name IS '销售方名称';
COMMENT ON COLUMN arc_file_metadata_index.issue_date IS '开票日期';
COMMENT ON COLUMN arc_file_metadata_index.parsed_time IS '解析时间';
COMMENT ON COLUMN arc_file_metadata_index.parser_type IS '解析器类型';

CREATE INDEX IF NOT EXISTS idx_file_id ON arc_file_metadata_index(file_id);
CREATE INDEX IF NOT EXISTS idx_invoice_number ON arc_file_metadata_index(invoice_number);
CREATE INDEX IF NOT EXISTS idx_seller_name ON arc_file_metadata_index(seller_name);
CREATE INDEX IF NOT EXISTS idx_issue_date ON arc_file_metadata_index(issue_date);
