-- V51: 档案附件关联表
-- 支持全景视图中凭证与附件的多对多关联

CREATE TABLE IF NOT EXISTS acc_archive_attachment (
    id VARCHAR(64) PRIMARY KEY,
    archive_id VARCHAR(64) NOT NULL,
    file_id VARCHAR(64) NOT NULL,
    attachment_type VARCHAR(32) NOT NULL, -- 'invoice', 'contract', 'bank_slip', 'other'
    relation_desc VARCHAR(255),
    created_by VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_archive_file UNIQUE (archive_id, file_id)
);

COMMENT ON TABLE acc_archive_attachment IS '档案附件关联表';
COMMENT ON COLUMN acc_archive_attachment.archive_id IS '档案ID (acc_archive.id)';
COMMENT ON COLUMN acc_archive_attachment.file_id IS '文件ID (arc_file_content.id)';
COMMENT ON COLUMN acc_archive_attachment.attachment_type IS '附件类型: invoice/contract/bank_slip/other';
COMMENT ON COLUMN acc_archive_attachment.relation_desc IS '关联描述';
COMMENT ON COLUMN acc_archive_attachment.created_by IS '创建人ID';
COMMENT ON COLUMN acc_archive_attachment.created_at IS '创建时间';

CREATE INDEX IF NOT EXISTS idx_archive_attachment_archive ON acc_archive_attachment(archive_id);
CREATE INDEX IF NOT EXISTS idx_archive_attachment_file ON acc_archive_attachment(file_id);
