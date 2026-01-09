-- Add fonds_code to scan_workspace table
-- Compliance: DA/T 94-2022 Multi-fonds isolation
ALTER TABLE scan_workspace ADD COLUMN IF NOT EXISTS fonds_code VARCHAR(32);

-- Update comments
COMMENT ON COLUMN scan_workspace.fonds_code IS '所属全宗代码';

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_scan_workspace_fonds ON scan_workspace(fonds_code);

-- Best-effort: No reliable way to backfill existing files as they were userId-only,
-- but we could theoretically use the current user's default fonds if needed.
-- For now, new files will require this.
