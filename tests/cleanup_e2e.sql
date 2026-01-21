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

    integrity_check,

    audit_inspection_log

CASCADE;



-- 重置所有相关表的序列

SELECT setval(pg_get_serial_sequence('collection_batch', 'id'), coalesce(max(id), 1), max(id) IS NOT null) FROM collection_batch;

SELECT setval(pg_get_serial_sequence('collection_batch_file', 'id'), coalesce(max(id), 1), max(id) IS NOT null) FROM collection_batch_file;
