-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 为audit_inspection_log表添加符合性检查字段
ALTER TABLE audit_inspection_log ADD COLUMN IF NOT EXISTS is_compliant BOOLEAN;
ALTER TABLE audit_inspection_log ADD COLUMN IF NOT EXISTS compliance_violations TEXT;
ALTER TABLE audit_inspection_log ADD COLUMN IF NOT EXISTS compliance_warnings TEXT;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_audit_inspection_compliance ON audit_inspection_log(is_compliant, inspection_time);
CREATE INDEX IF NOT EXISTS idx_audit_inspection_archive_compliance ON audit_inspection_log(archive_id, is_compliant);