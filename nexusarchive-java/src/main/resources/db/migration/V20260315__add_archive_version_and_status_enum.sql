-- Input: existing acc_archive table
-- Output: acc_archive with version column for optimistic locking
-- Pos: src/main/resources/db/migration/

-- ================================================================
-- Migration: V20260315__add_archive_version_and_status_enum.sql
-- Purpose: Add optimistic locking version field to acc_archive
-- Author: Implementation Team
-- Date: 2026-03-15
-- Related: P1 - Archive Entity Enhancements
-- ================================================================

BEGIN;

-- Add version column with default value for existing rows
ALTER TABLE public.acc_archive
ADD COLUMN version INT NOT NULL DEFAULT 0;

COMMENT ON COLUMN public.acc_archive.version IS '乐观锁版本号 (MyBatis-Plus @Version)';

-- Create index on version for concurrent update detection
CREATE INDEX idx_acc_archive_version ON public.acc_archive(id, version);

-- Add index for status column (frequently filtered)
CREATE INDEX idx_acc_archive_status ON public.acc_archive(status);

-- Add composite index for common queries (status + fonds)
CREATE INDEX idx_acc_archive_status_fonds ON public.acc_archive(status, fonds_no);

COMMIT;
