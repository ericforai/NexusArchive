CREATE TABLE IF NOT EXISTS arc_signature_verification (
    id VARCHAR(64) PRIMARY KEY,
    archive_id VARCHAR(64) NOT NULL,
    file_id VARCHAR(64),
    file_name VARCHAR(255),
    document_type VARCHAR(16) NOT NULL,
    trigger_source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    provider_code VARCHAR(64),
    provider_version VARCHAR(64),
    verification_status VARCHAR(32) NOT NULL,
    signature_count INTEGER NOT NULL DEFAULT 0,
    valid_signature_count INTEGER NOT NULL DEFAULT 0,
    invalid_signature_count INTEGER NOT NULL DEFAULT 0,
    error_code VARCHAR(64),
    error_message VARCHAR(2000),
    verified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    result_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sig_ver_archive
        FOREIGN KEY (archive_id) REFERENCES acc_archive(id) ON DELETE CASCADE,
    CONSTRAINT chk_sig_ver_signature_count_non_negative
        CHECK (signature_count >= 0),
    CONSTRAINT chk_sig_ver_valid_signature_count_non_negative
        CHECK (valid_signature_count >= 0),
    CONSTRAINT chk_sig_ver_invalid_signature_count_non_negative
        CHECK (invalid_signature_count >= 0),
    CONSTRAINT chk_sig_ver_signature_count_consistent
        CHECK (signature_count >= valid_signature_count + invalid_signature_count)
);

CREATE INDEX IF NOT EXISTS idx_sig_ver_archive_time
    ON arc_signature_verification (archive_id, verified_at DESC);

CREATE INDEX IF NOT EXISTS idx_sig_ver_file_time
    ON arc_signature_verification (file_id, verified_at DESC);

CREATE INDEX IF NOT EXISTS idx_sig_ver_status
    ON arc_signature_verification (verification_status);

COMMENT ON TABLE arc_signature_verification IS 'Archive signature verification records';
COMMENT ON COLUMN arc_signature_verification.archive_id IS 'Associated archive ID';
COMMENT ON COLUMN arc_signature_verification.file_id IS 'Associated file ID';
COMMENT ON COLUMN arc_signature_verification.file_name IS 'Source document file name';
COMMENT ON COLUMN arc_signature_verification.document_type IS 'Document type: PDF/OFD/UNKNOWN';
COMMENT ON COLUMN arc_signature_verification.trigger_source IS 'Verification trigger source';
COMMENT ON COLUMN arc_signature_verification.provider_code IS 'Verification provider code';
COMMENT ON COLUMN arc_signature_verification.provider_version IS 'Verification provider version';
COMMENT ON COLUMN arc_signature_verification.verification_status IS 'Verification status: PASSED/FAILED/NO_SIGNATURE/UNSUPPORTED/ERROR';
COMMENT ON COLUMN arc_signature_verification.signature_count IS 'Total signature count found in the document';
COMMENT ON COLUMN arc_signature_verification.valid_signature_count IS 'Number of valid signatures';
COMMENT ON COLUMN arc_signature_verification.invalid_signature_count IS 'Number of invalid signatures';
COMMENT ON COLUMN arc_signature_verification.error_code IS 'Verification error code';
COMMENT ON COLUMN arc_signature_verification.error_message IS 'Verification error message';
COMMENT ON COLUMN arc_signature_verification.verified_at IS 'Verification execution time';
COMMENT ON COLUMN arc_signature_verification.result_payload IS 'Full verification result payload as JSONB';
COMMENT ON COLUMN arc_signature_verification.created_time IS 'Record creation time';
