-- Input: PostgreSQL(pSQL)
-- Output: 生产附件巡检 SQL 摘要
-- Pos: 生产排查辅助 SQL
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

\pset pager off
\pset null '(null)'

\echo '=== SECTION:TABLE_COUNTS ==='
SELECT 'arc_original_voucher' AS table_name, COUNT(*) AS total_rows FROM arc_original_voucher
UNION ALL
SELECT 'arc_file_content' AS table_name, COUNT(*) AS total_rows FROM arc_file_content
UNION ALL
SELECT 'arc_original_voucher_file' AS table_name, COUNT(*) AS total_rows FROM arc_original_voucher_file
ORDER BY table_name;

\echo '=== SECTION:STORAGE_PATH_EMPTY ==='
SELECT 'arc_file_content' AS table_name, COUNT(*) AS empty_storage_path_rows
FROM arc_file_content
WHERE storage_path IS NULL OR btrim(storage_path) = ''
UNION ALL
SELECT 'arc_original_voucher_file' AS table_name, COUNT(*) AS empty_storage_path_rows
FROM arc_original_voucher_file
WHERE storage_path IS NULL OR btrim(storage_path) = ''
ORDER BY table_name;

\echo '=== SECTION:TARGET_FILE_ID ==='
SELECT
  'arc_file_content' AS source_table,
  id::text AS row_id,
  COALESCE(archival_code, '') AS code,
  COALESCE(file_name, '') AS file_name,
  COALESCE(storage_path, '') AS storage_path,
  COALESCE(file_size, 0)::bigint AS file_size,
  COALESCE(to_char(created_time, 'YYYY-MM-DD HH24:MI:SS'), '') AS created_time
FROM arc_file_content
WHERE id::text = :'target_file_id'
UNION ALL
SELECT
  'arc_original_voucher_file' AS source_table,
  id::text AS row_id,
  COALESCE(voucher_no, '') AS code,
  COALESCE(file_name, '') AS file_name,
  COALESCE(storage_path, '') AS storage_path,
  COALESCE(file_size, 0)::bigint AS file_size,
  COALESCE(to_char(created_time, 'YYYY-MM-DD HH24:MI:SS'), '') AS created_time
FROM arc_original_voucher_file
WHERE id::text = :'target_file_id';

