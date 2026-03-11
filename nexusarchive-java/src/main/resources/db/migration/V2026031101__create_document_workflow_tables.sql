CREATE TABLE IF NOT EXISTS document_sections (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_document_sections_project
    ON document_sections (project_id, sort_order, created_at);

CREATE TABLE IF NOT EXISTS document_assignments (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    section_id VARCHAR(64) NOT NULL,
    assignee_id VARCHAR(64) NOT NULL,
    assignee_name VARCHAR(128),
    assigned_by VARCHAR(64),
    note VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_assignments_section
        FOREIGN KEY (section_id) REFERENCES document_sections(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_document_assignments_section
    ON document_assignments (project_id, section_id, created_at DESC);

CREATE TABLE IF NOT EXISTS document_locks (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    section_id VARCHAR(64) NOT NULL,
    locked_by VARCHAR(64) NOT NULL,
    locked_by_name VARCHAR(128),
    reason VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_locks_section
        FOREIGN KEY (section_id) REFERENCES document_sections(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_document_locks_section
    ON document_locks (project_id, section_id, created_at DESC);

CREATE TABLE IF NOT EXISTS document_reminders (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    section_id VARCHAR(64) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    remind_at TIMESTAMP NOT NULL,
    recipient_id VARCHAR(64) NOT NULL,
    recipient_name VARCHAR(128),
    created_by VARCHAR(64),
    delivered BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_reminders_section
        FOREIGN KEY (section_id) REFERENCES document_sections(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_document_reminders_section
    ON document_reminders (project_id, section_id, created_at DESC);

CREATE TABLE IF NOT EXISTS document_versions (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    version_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    created_by VARCHAR(64),
    rolled_back_by VARCHAR(64),
    rolled_back_at TIMESTAMP,
    snapshot_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_document_versions_project
    ON document_versions (project_id, created_at DESC);
