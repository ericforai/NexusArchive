-- ====================================================
-- V69: 原始凭证模块数据表
-- Reference: DA/T 94-2022, GB/T 39362-2020
-- 设计文档: original_voucher_design.md
-- ====================================================

-- 1. 原始凭证类型字典表
CREATE TABLE IF NOT EXISTS sys_original_voucher_type (
    id                  VARCHAR(64)     PRIMARY KEY,
    category_code       VARCHAR(32)     NOT NULL,
    category_name       VARCHAR(50)     NOT NULL,
    type_code           VARCHAR(32)     NOT NULL UNIQUE,
    type_name           VARCHAR(100)    NOT NULL,
    default_retention   VARCHAR(20)     DEFAULT '30Y',
    sort_order          INT             DEFAULT 0,
    enabled             BOOLEAN         DEFAULT TRUE,
    created_time        TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    last_modified_time  TIMESTAMP
);

COMMENT ON TABLE sys_original_voucher_type IS '原始凭证类型字典表 - 支持运维扩展';

-- 初始化类型数据
INSERT INTO sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order) VALUES
('ovt-001', 'INVOICE', '发票类', 'INV_PAPER', '纸质发票', '30Y', 1),
('ovt-002', 'INVOICE', '发票类', 'INV_VAT_E', '增值税电子发票', '30Y', 2),
('ovt-003', 'INVOICE', '发票类', 'INV_DIGITAL', '数电发票', '30Y', 3),
('ovt-004', 'INVOICE', '发票类', 'INV_RAIL', '数电票（铁路）', '30Y', 4),
('ovt-005', 'INVOICE', '发票类', 'INV_AIR', '数电票（航空）', '30Y', 5),
('ovt-006', 'INVOICE', '发票类', 'INV_GOV', '数电票（财政）', '30Y', 6),
('ovt-007', 'BANK', '银行类', 'BANK_RECEIPT', '银行回单', '30Y', 10),
('ovt-008', 'BANK', '银行类', 'BANK_STATEMENT', '银行对账单', '30Y', 11),
('ovt-009', 'DOCUMENT', '单据类', 'DOC_PAYMENT', '付款单', '30Y', 20),
('ovt-010', 'DOCUMENT', '单据类', 'DOC_RECEIPT', '收款单', '30Y', 21),
('ovt-011', 'DOCUMENT', '单据类', 'DOC_RECEIPT_VOUCHER', '收款单据（收据）', '30Y', 22),
('ovt-012', 'DOCUMENT', '单据类', 'DOC_PAYROLL', '工资单', '30Y', 23),
('ovt-013', 'CONTRACT', '合同类', 'CONTRACT', '合同', 'PERMANENT', 30),
('ovt-014', 'CONTRACT', '合同类', 'AGREEMENT', '协议', '30Y', 31),
('ovt-015', 'OTHER', '其他类', 'OTHER', '其他', '30Y', 99)
ON CONFLICT (type_code) DO NOTHING;

-- 2. 原始凭证主表
CREATE TABLE IF NOT EXISTS arc_original_voucher (
    id                  VARCHAR(64)     PRIMARY KEY,
    voucher_no          VARCHAR(100)    NOT NULL,
    voucher_category    VARCHAR(32)     NOT NULL,
    voucher_type        VARCHAR(32)     NOT NULL,
    business_date       DATE            NOT NULL,
    amount              DECIMAL(18,2),
    currency            VARCHAR(10)     DEFAULT 'CNY',
    counterparty        VARCHAR(200),
    summary             TEXT,
    creator             VARCHAR(100),
    auditor             VARCHAR(100),
    bookkeeper          VARCHAR(100),
    approver            VARCHAR(100),
    source_system       VARCHAR(50),
    source_doc_id       VARCHAR(200),
    fonds_code          VARCHAR(50)     NOT NULL,
    fiscal_year         VARCHAR(4)      NOT NULL,
    retention_period    VARCHAR(20)     NOT NULL,
    archive_status      VARCHAR(20)     DEFAULT 'DRAFT',
    archived_time       TIMESTAMP,
    version             INT             DEFAULT 1,
    parent_version_id   VARCHAR(64),
    version_reason      TEXT,
    is_latest           BOOLEAN         DEFAULT TRUE,
    created_by          VARCHAR(64),
    created_time        TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    last_modified_by    VARCHAR(64),
    last_modified_time  TIMESTAMP,
    deleted             INT             DEFAULT 0,
    CONSTRAINT uk_ov_voucher_no UNIQUE (fonds_code, fiscal_year, voucher_no)
);

CREATE INDEX IF NOT EXISTS idx_ov_type ON arc_original_voucher(voucher_category, voucher_type);
CREATE INDEX IF NOT EXISTS idx_ov_status ON arc_original_voucher(archive_status);
CREATE INDEX IF NOT EXISTS idx_ov_date ON arc_original_voucher(business_date);
CREATE INDEX IF NOT EXISTS idx_ov_counterparty ON arc_original_voucher(counterparty);
CREATE INDEX IF NOT EXISTS idx_ov_source ON arc_original_voucher(source_system, source_doc_id);

COMMENT ON TABLE arc_original_voucher IS '原始凭证主表 - 独立于记账凭证，符合DA/T 94-2022';
COMMENT ON COLUMN arc_original_voucher.voucher_no IS '原始凭证编号，格式: OV-{年度}-{类型}-{序号}';
COMMENT ON COLUMN arc_original_voucher.version IS '版本号，每次修改递增';
COMMENT ON COLUMN arc_original_voucher.parent_version_id IS '指向前一版本的ID，形成版本链';

-- 3. 原始凭证文件表
CREATE TABLE IF NOT EXISTS arc_original_voucher_file (
    id                  VARCHAR(64)     PRIMARY KEY,
    voucher_id          VARCHAR(64)     NOT NULL,
    file_name           VARCHAR(255)    NOT NULL,
    file_type           VARCHAR(20)     NOT NULL,
    file_size           BIGINT          NOT NULL,
    storage_path        VARCHAR(500)    NOT NULL,
    file_hash           VARCHAR(128)    NOT NULL,
    hash_algorithm      VARCHAR(20)     DEFAULT 'SM3',
    original_hash       VARCHAR(128),
    sign_value          BYTEA,
    sign_cert           TEXT,
    sign_time           TIMESTAMP,
    timestamp_token     BYTEA,
    file_role           VARCHAR(20)     DEFAULT 'PRIMARY',
    sequence_no         INT             DEFAULT 1,
    created_by          VARCHAR(64),
    created_time        TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    deleted             INT             DEFAULT 0,
    CONSTRAINT fk_ovf_voucher FOREIGN KEY (voucher_id) REFERENCES arc_original_voucher(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ovf_voucher ON arc_original_voucher_file(voucher_id);
CREATE INDEX IF NOT EXISTS idx_ovf_hash ON arc_original_voucher_file(file_hash);

COMMENT ON TABLE arc_original_voucher_file IS '原始凭证文件表 - 支持一凭证多文件';

-- 4. 原始凭证与记账凭证关联表
CREATE TABLE IF NOT EXISTS arc_voucher_relation (
    id                      VARCHAR(64)     PRIMARY KEY,
    original_voucher_id     VARCHAR(64)     NOT NULL,
    accounting_voucher_id   VARCHAR(64)     NOT NULL,
    relation_type           VARCHAR(30)     DEFAULT 'ORIGINAL_TO_ACCOUNTING',
    relation_desc           VARCHAR(200),
    created_by              VARCHAR(64),
    created_time            TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    deleted                 INT             DEFAULT 0,
    CONSTRAINT uk_voucher_rel UNIQUE (original_voucher_id, accounting_voucher_id),
    CONSTRAINT fk_vr_original FOREIGN KEY (original_voucher_id) REFERENCES arc_original_voucher(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vr_original ON arc_voucher_relation(original_voucher_id);
CREATE INDEX IF NOT EXISTS idx_vr_accounting ON arc_voucher_relation(accounting_voucher_id);

COMMENT ON TABLE arc_voucher_relation IS '原始凭证与记账凭证多对多关联表';

-- 5. 原始凭证编号序列表
CREATE TABLE IF NOT EXISTS arc_original_voucher_sequence (
    id                  VARCHAR(64)     PRIMARY KEY,
    fonds_code          VARCHAR(50)     NOT NULL,
    fiscal_year         VARCHAR(4)      NOT NULL,
    voucher_category    VARCHAR(32)     NOT NULL,
    current_seq         BIGINT          DEFAULT 0,
    last_updated        TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ov_seq UNIQUE (fonds_code, fiscal_year, voucher_category)
);

COMMENT ON TABLE arc_original_voucher_sequence IS '原始凭证编号序列表';
