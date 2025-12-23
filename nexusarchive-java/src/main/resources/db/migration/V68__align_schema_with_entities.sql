-- Migration V68: Align Database Schema with Entity Field Renames
-- Target: Postgres / Dameng / Kingbase (Standard SQL)

-- 1. Archive (acc_archive)
-- Rename timestamp columns if they exist as old names
DO $$
BEGIN
  IF EXISTS(SELECT * FROM information_schema.columns WHERE table_name='acc_archive' AND column_name='created_at') THEN
    ALTER TABLE acc_archive RENAME COLUMN created_at TO created_time;
  END IF;
  IF EXISTS(SELECT * FROM information_schema.columns WHERE table_name='acc_archive' AND column_name='updated_at') THEN
    ALTER TABLE acc_archive RENAME COLUMN updated_at TO last_modified_time;
  END IF;
END $$;

-- 2. Volume (acc_archive_volume)
DO $$
BEGIN
  IF EXISTS(SELECT * FROM information_schema.columns WHERE table_name='acc_archive_volume' AND column_name='created_at') THEN
    ALTER TABLE acc_archive_volume RENAME COLUMN created_at TO created_time;
  END IF;
  IF EXISTS(SELECT * FROM information_schema.columns WHERE table_name='acc_archive_volume' AND column_name='updated_at') THEN
    ALTER TABLE acc_archive_volume RENAME COLUMN updated_at TO last_modified_time;
  END IF;
END $$;

-- 3. SysAuditLog (sys_audit_log)
DO $$
BEGIN
  IF EXISTS(SELECT * FROM information_schema.columns WHERE table_name='sys_audit_log' AND column_name='created_at') THEN
    ALTER TABLE sys_audit_log RENAME COLUMN created_at TO created_time;
  END IF;
  IF EXISTS(SELECT * FROM information_schema.columns WHERE table_name='sys_audit_log' AND column_name='ip_address') THEN
    ALTER TABLE sys_audit_log RENAME COLUMN ip_address TO client_ip;
  END IF;
END $$;

-- 4. ArchiveBatch (arc_archive_batch)
-- Add batch_sequence if not exists
ALTER TABLE arc_archive_batch ADD COLUMN IF NOT EXISTS batch_sequence BIGINT;

-- 5. ReconciliationRecord (arc_reconciliation_record)
-- Add missing columns
ALTER TABLE arc_reconciliation_record ADD COLUMN IF NOT EXISTS config_id BIGINT;
ALTER TABLE arc_reconciliation_record ADD COLUMN IF NOT EXISTS accbook_code VARCHAR(64);
ALTER TABLE arc_reconciliation_record ADD COLUMN IF NOT EXISTS recon_start_date TIMESTAMP;
ALTER TABLE arc_reconciliation_record ADD COLUMN IF NOT EXISTS recon_end_date TIMESTAMP;
