-- Input: PostgreSQL(pSQL)
-- Output: 附件修复 SQL 模板（由 prod_attachment_repair.sh 自动生成 apply_updates.sql）
-- Pos: 生产修复模板
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

BEGIN;

-- 示例：修复 arc_file_content
-- UPDATE arc_file_content
-- SET storage_path = 'uploads/recovered/<row_id>_<file_name>',
--     file_size = <new_size>,
--     last_modified_time = NOW()
-- WHERE id = '<row_id>';

-- 示例：修复 arc_original_voucher_file
-- UPDATE arc_original_voucher_file
-- SET storage_path = 'uploads/recovered/<row_id>_<file_name>',
--     file_size = <new_size>,
--     last_modified_time = NOW()
-- WHERE id = '<row_id>';

COMMIT;

