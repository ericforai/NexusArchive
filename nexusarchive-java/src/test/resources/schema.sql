-- Schema for Testing (H2 Mode PostgreSQL)

DROP TABLE IF EXISTS sys_audit_log;
DROP TABLE IF EXISTS acc_archive;
DROP TABLE IF EXISTS sys_erp_config;
DROP TABLE IF EXISTS sys_org;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_user_role;

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
    unique_biz_id VARCHAR(128),
    amount DECIMAL(18,2),
    doc_date DATE,
    volume_id VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paper_ref_link VARCHAR(255),
    destruction_hold BOOLEAN,
    hold_reason VARCHAR(512),
    deleted INT DEFAULT 0,
    fixity_value VARCHAR(128),
    fixity_algo VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS sys_audit_log (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64),
    username VARCHAR(128),
    role_type VARCHAR(32),
    action VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    resource_type VARCHAR(64),
    resource_id VARCHAR(64),
    operation_result VARCHAR(32),
    risk_level VARCHAR(32),
    details TEXT,
    data_before TEXT,
    data_after TEXT,
    session_id VARCHAR(64),
    ip_address VARCHAR(50) NOT NULL DEFAULT '0.0.0.0',
    mac_address VARCHAR(64) NOT NULL DEFAULT 'UNKNOWN',
    object_digest VARCHAR(128),
    user_agent VARCHAR(255),
    prev_log_hash VARCHAR(128),
    log_hash VARCHAR(128),
    device_fingerprint VARCHAR(128),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_erp_config (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128),
    erp_type VARCHAR(64),
    config_json TEXT,
    is_active INT DEFAULT 1,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_org (
  id            VARCHAR(64) PRIMARY KEY,
  name          VARCHAR(255) NOT NULL,
  code          VARCHAR(128),
  parent_id     VARCHAR(64),
  type          VARCHAR(32) DEFAULT 'DEPARTMENT',
  order_num     INTEGER DEFAULT 0,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted       INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role (
  id             VARCHAR(64) PRIMARY KEY,
  name           VARCHAR(255) NOT NULL,
  code           VARCHAR(128) NOT NULL UNIQUE,
  role_category  VARCHAR(64),
  is_exclusive   BOOLEAN DEFAULT FALSE,
  description    TEXT,
  permissions    TEXT,
  data_scope     VARCHAR(32) DEFAULT 'self',
  type           VARCHAR(32) DEFAULT 'custom',
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted        INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user (
  id               VARCHAR(64) PRIMARY KEY,
  username         VARCHAR(128) NOT NULL UNIQUE,
  password_hash    VARCHAR(255) NOT NULL,
  full_name        VARCHAR(255),
  org_code         VARCHAR(128),
  email            VARCHAR(255),
  phone            VARCHAR(64),
  avatar           VARCHAR(512),
  department_id    VARCHAR(64),
  status           VARCHAR(32) DEFAULT 'active',
  last_login_at    TIMESTAMP,
  employee_id      VARCHAR(64),
  job_title        VARCHAR(128),
  join_date        VARCHAR(32),
  created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted          INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id   VARCHAR(64) NOT NULL,
  role_id   VARCHAR(64) NOT NULL,
  PRIMARY KEY (user_id, role_id)
);
