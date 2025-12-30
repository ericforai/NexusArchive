-- V68: 修复 Schema Validator 检测到的 17 个缺失列
-- 确保 Entity 字段与数据库表结构完全一致

-- 1. biz_archive_approval.created_time
ALTER TABLE biz_archive_approval ADD COLUMN IF NOT EXISTS created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN biz_archive_approval.created_time IS '创建时间';

-- 2. acc_archive_attachment.created_at
ALTER TABLE acc_archive_attachment ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN acc_archive_attachment.created_at IS '创建时间';

-- 3. acc_archive_relation.created_at
ALTER TABLE acc_archive_relation ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN acc_archive_relation.created_at IS '创建时间';

-- 4. audit_inspection_log.created_time
ALTER TABLE audit_inspection_log ADD COLUMN IF NOT EXISTS created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN audit_inspection_log.created_time IS '创建时间';

-- 5. biz_destruction.created_time
ALTER TABLE biz_destruction ADD COLUMN IF NOT EXISTS created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN biz_destruction.created_time IS '创建时间';

-- 6. bas_location.created_time
ALTER TABLE bas_location ADD COLUMN IF NOT EXISTS created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN bas_location.created_time IS '创建时间';

-- 7. bas_location.last_modified_time
ALTER TABLE bas_location ADD COLUMN IF NOT EXISTS last_modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN bas_location.last_modified_time IS '更新时间';

-- 8. biz_open_appraisal.created_time
ALTER TABLE biz_open_appraisal ADD COLUMN IF NOT EXISTS created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN biz_open_appraisal.created_time IS '创建时间';

-- 9. sys_org.updated_time
ALTER TABLE sys_org ADD COLUMN IF NOT EXISTS updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN sys_org.updated_time IS '更新时间';

-- 10. sys_permission.created_time
ALTER TABLE sys_permission ADD COLUMN IF NOT EXISTS created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN sys_permission.created_time IS '创建时间';

-- 11. sys_permission.updated_time
ALTER TABLE sys_permission ADD COLUMN IF NOT EXISTS updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN sys_permission.updated_time IS '更新时间';

-- 12. sys_role.created_time
ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN sys_role.created_time IS '创建时间';

-- 13. sys_audit_log.ip_address
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS ip_address VARCHAR(64);
COMMENT ON COLUMN sys_audit_log.ip_address IS 'IP地址';

-- 14. sys_audit_log.created_at
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN sys_audit_log.created_at IS '创建时间';

-- 15. sys_setting.created_time
ALTER TABLE sys_setting ADD COLUMN IF NOT EXISTS created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN sys_setting.created_time IS '创建时间';

-- 16. sys_setting.updated_time
ALTER TABLE sys_setting ADD COLUMN IF NOT EXISTS updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN sys_setting.updated_time IS '更新时间';

-- 17. acc_archive_volume.updated_time
ALTER TABLE acc_archive_volume ADD COLUMN IF NOT EXISTS updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN acc_archive_volume.updated_time IS '更新时间';
