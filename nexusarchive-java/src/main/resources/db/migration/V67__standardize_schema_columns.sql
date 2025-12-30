-- V67: 统一数据库列命名规范
-- 将 created_at 重命名为 created_time
-- 将 updated_at 重命名为 last_modified_time
-- 遵循 general.md 中的核心标准

DO $$
BEGIN
    -- 1. sys_user 表
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sys_user' AND column_name = 'created_at') THEN
        ALTER TABLE sys_user RENAME COLUMN created_at TO created_time;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sys_user' AND column_name = 'updated_at') THEN
        ALTER TABLE sys_user RENAME COLUMN updated_at TO last_modified_time;
    END IF;

    -- 2. sys_org 表
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sys_org' AND column_name = 'created_at') THEN
        ALTER TABLE sys_org RENAME COLUMN created_at TO created_time;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sys_org' AND column_name = 'updated_at') THEN
        ALTER TABLE sys_org RENAME COLUMN updated_at TO last_modified_time;
    END IF;

    -- 3. acc_archive 表
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'acc_archive' AND column_name = 'updated_at') THEN
        ALTER TABLE acc_archive RENAME COLUMN updated_at TO last_modified_time;
    END IF;

    -- 4. arc_file_content 表
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'arc_file_content' AND column_name = 'updated_at') THEN
        ALTER TABLE arc_file_content RENAME COLUMN updated_at TO last_modified_time;
    END IF;

    -- 5. acc_archive_attachment 表
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'acc_archive_attachment' AND column_name = 'created_at') THEN
        ALTER TABLE acc_archive_attachment RENAME COLUMN created_at TO created_time;
    END IF;

    -- 6. acc_archive_relation 表
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'acc_archive_relation' AND column_name = 'created_at') THEN
        ALTER TABLE acc_archive_relation RENAME COLUMN created_at TO created_time;
    END IF;

    -- 7. original_voucher 表
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'original_voucher' AND column_name = 'created_at') THEN
        ALTER TABLE original_voucher RENAME COLUMN created_at TO created_time;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'original_voucher' AND column_name = 'updated_at') THEN
        ALTER TABLE original_voucher RENAME COLUMN updated_at TO last_modified_time;
    END IF;

    -- 8. ingest_request_status 表
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'ingest_request_status' AND column_name = 'created_at') THEN
        ALTER TABLE ingest_request_status RENAME COLUMN created_at TO created_time;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'ingest_request_status' AND column_name = 'updated_at') THEN
        ALTER TABLE ingest_request_status RENAME COLUMN updated_at TO last_modified_time;
    END IF;

    -- 9. sys_role 表
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sys_role' AND column_name = 'updated_at') THEN
        ALTER TABLE sys_role RENAME COLUMN updated_at TO last_modified_time;
    END IF;

    -- 10. sys_permission 表
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sys_permission' AND column_name = 'updated_at') THEN
        ALTER TABLE sys_permission RENAME COLUMN updated_at TO last_modified_time;
    END IF;

    -- 11. biz_archive_approval 表
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'biz_archive_approval' AND column_name = 'updated_at') THEN
        ALTER TABLE biz_archive_approval RENAME COLUMN updated_at TO last_modified_time;
    END IF;

    -- 12. biz_borrowing 表
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'biz_borrowing' AND column_name = 'updated_at') THEN
        ALTER TABLE biz_borrowing RENAME COLUMN updated_at TO last_modified_time;
    END IF;

    -- 13. biz_destruction 表
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'biz_destruction' AND column_name = 'updated_at') THEN
        ALTER TABLE biz_destruction RENAME COLUMN updated_at TO last_modified_time;
    END IF;

    -- 14. biz_open_appraisal 表
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'biz_open_appraisal' AND column_name = 'updated_at') THEN
        ALTER TABLE biz_open_appraisal RENAME COLUMN updated_at TO last_modified_time;
    END IF;

END $$;
