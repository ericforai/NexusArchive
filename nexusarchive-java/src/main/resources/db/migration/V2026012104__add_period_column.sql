-- Add period column to arc_file_content
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS period VARCHAR(20);
COMMENT ON COLUMN arc_file_content.period IS '会计期间';
