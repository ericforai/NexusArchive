-- Increase column lengths for SM4 encryption (Base64 expansion)
-- Reference: docs/knowledge/2025-12-10-approval-ui-fix.md

-- Modify acc_archive (Archive Entity)
ALTER TABLE acc_archive ALTER COLUMN title TYPE VARCHAR(1000);
ALTER TABLE acc_archive ALTER COLUMN summary TYPE VARCHAR(2000);
ALTER TABLE acc_archive ALTER COLUMN creator TYPE VARCHAR(500);

-- Modify arc_archive_approval (ArchiveApproval Entity) if necessary
-- Note: approval table typically copies title, so it should be checked too.
-- Based on error "value too long for type character varying(255)", likely referring to archive title.
-- Let's check archive_approval definition in V7
ALTER TABLE biz_archive_approval ALTER COLUMN archive_title TYPE VARCHAR(1000);
