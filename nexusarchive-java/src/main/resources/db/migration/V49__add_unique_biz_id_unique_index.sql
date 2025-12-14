-- Enforce unique business ID for archives (ignore logically deleted rows)
DROP INDEX IF EXISTS idx_archive_unique_biz_id;
CREATE UNIQUE INDEX IF NOT EXISTS ux_acc_archive_unique_biz_id_not_deleted
    ON acc_archive(unique_biz_id)
    WHERE deleted = 0;
