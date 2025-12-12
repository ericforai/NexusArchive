-- Add org_name to biz_archive_approval
ALTER TABLE biz_archive_approval ADD COLUMN IF NOT EXISTS org_name VARCHAR(255);
COMMENT ON COLUMN biz_archive_approval.org_name IS '立档单位';
