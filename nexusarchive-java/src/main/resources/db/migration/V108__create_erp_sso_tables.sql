-- ERP 发起联查 SSO 基础表

CREATE TABLE IF NOT EXISTS erp_sso_client (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(64) NOT NULL UNIQUE,
    client_secret VARCHAR(256) NOT NULL,
    client_name VARCHAR(128),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS erp_user_mapping (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(64) NOT NULL,
    erp_user_job_no VARCHAR(64) NOT NULL,
    nexus_user_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_erp_user_mapping UNIQUE (client_id, erp_user_job_no)
);

CREATE INDEX IF NOT EXISTS idx_erp_user_mapping_nexus_user_id
    ON erp_user_mapping (nexus_user_id);

CREATE TABLE IF NOT EXISTS erp_sso_launch_ticket (
    id VARCHAR(64) PRIMARY KEY,
    client_id VARCHAR(64) NOT NULL,
    erp_user_job_no VARCHAR(64) NOT NULL,
    nexus_user_id VARCHAR(64) NOT NULL,
    accbook_code VARCHAR(64) NOT NULL,
    fonds_code VARCHAR(64) NOT NULL,
    voucher_no VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used SMALLINT NOT NULL DEFAULT 0,
    used_at TIMESTAMP,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_erp_sso_launch_ticket_expire
    ON erp_sso_launch_ticket (expires_at);
