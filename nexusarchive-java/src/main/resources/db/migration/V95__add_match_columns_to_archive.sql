-- Input: acc_archive table
-- Output: Added match_score and match_method columns
-- Pos: Database Migration
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- Add matching quality columns to acc_archive
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS match_score INTEGER DEFAULT 0;
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS match_method VARCHAR(100);

COMMENT ON COLUMN acc_archive.match_score IS '智能匹配得分 (0-100)';
COMMENT ON COLUMN acc_archive.match_method IS '关联方式 (如：金额+日期匹配)';
