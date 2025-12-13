-- V33: Increase column lengths for encrypted fields in acc_archive
-- Issue: SM4 encryption + Base64 encoding increases data length beyond VARCHAR(255)
-- Reference: Approval 500 error "value too long for type character varying(255)"

-- Title: Encrypted archive title (plaintext ~255 -> encrypted ~500)
ALTER TABLE acc_archive ALTER COLUMN title TYPE VARCHAR(1000);

-- Summary: Encrypted archive summary/filename
ALTER TABLE acc_archive ALTER COLUMN summary TYPE VARCHAR(2000);

-- Creator: Encrypted creator name
ALTER TABLE acc_archive ALTER COLUMN creator TYPE VARCHAR(500);

-- Org Name: Organization name (precautionary)
ALTER TABLE acc_archive ALTER COLUMN org_name TYPE VARCHAR(500);

-- Comments
COMMENT ON COLUMN acc_archive.title IS '题名 (SM4加密存储，最大1000字符)';
COMMENT ON COLUMN acc_archive.summary IS '摘要/说明 (SM4加密存储，最大2000字符)';
COMMENT ON COLUMN acc_archive.creator IS '责任者/制单人 (SM4加密存储，最大500字符)';
COMMENT ON COLUMN acc_archive.org_name IS '立档单位名称 (最大500字符)';
