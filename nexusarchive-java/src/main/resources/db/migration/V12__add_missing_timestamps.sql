-- V12: Ensure timestamp columns exist for dashboard queries
-- Fixes errors like "column \"created_at\" does not exist" on acc_archive and sys_ingest_request_status
-- PostgreSQL syntax; for other DBs please adapt accordingly.

-- 1) acc_archive: add created_at/updated_at when missing, backfill from created_time/updated_time
ALTER TABLE acc_archive
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'acc_archive' AND column_name = 'created_time'
    ) THEN
        EXECUTE 'UPDATE acc_archive SET created_at = created_time WHERE created_at IS NULL';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'acc_archive' AND column_name = 'updated_time'
    ) THEN
        EXECUTE 'UPDATE acc_archive SET updated_at = updated_time WHERE updated_at IS NULL';
    END IF;
END $$;

-- 2) sys_ingest_request_status: add created_at/updated_at, backfill from existing *_time columns
ALTER TABLE sys_ingest_request_status
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'sys_ingest_request_status' AND column_name = 'created_time'
    ) THEN
        EXECUTE 'UPDATE sys_ingest_request_status SET created_at = created_time WHERE created_at IS NULL';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'sys_ingest_request_status' AND column_name = 'updated_time'
    ) THEN
        EXECUTE 'UPDATE sys_ingest_request_status SET updated_at = updated_time WHERE updated_at IS NULL';
    END IF;
END $$;
