-- V13: Compliance Enhancement (DA/T 94 & Archive Management Rules)

-- 1. Enhance acc_archive (Item)
-- Add link to physical paper archive
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS paper_ref_link VARCHAR(128);
COMMENT ON COLUMN acc_archive.paper_ref_link IS '纸质档案关联号 (物理存放位置)';

-- Add destruction hold mechanism
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS destruction_hold BOOLEAN DEFAULT FALSE;
COMMENT ON COLUMN acc_archive.destruction_hold IS '销毁留置 (冻结状态)';

ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS hold_reason VARCHAR(255);
COMMENT ON COLUMN acc_archive.hold_reason IS '留置/冻结原因 (如: 未结清债权)';

-- 2. Enhance acc_archive_volume (Folder)
-- Add custody tracking
ALTER TABLE acc_archive_volume ADD COLUMN IF NOT EXISTS custodian_dept VARCHAR(32) DEFAULT 'ACCOUNTING';
COMMENT ON COLUMN acc_archive_volume.custodian_dept IS '当前保管部门: ACCOUNTING(会计), ARCHIVES(档案)';

CREATE INDEX IF NOT EXISTS idx_archive_destruction_hold ON acc_archive(destruction_hold);
