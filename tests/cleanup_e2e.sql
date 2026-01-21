-- E2E 测试环境清理脚本
-- 仅清理业务数据，保留配置数据（如 User, Role, SystemSetting 等）

TRUNCATE TABLE 
    collection_batch,
    collection_batch_file,
    arc_original_voucher,
    arc_original_voucher_file,
    arc_file_content,
    acc_archive,
    arc_archive_batch,
    archive_batch_item,
    acc_archive_attachment,
    -- pool_item, -- 如果找不到表，先注释掉
    -- compliance_result, -- 如果找不到表，先注释掉
    integrity_check,
    audit_inspection_log
CASCADE;