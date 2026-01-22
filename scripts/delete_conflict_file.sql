
-- 彻底物理删除冲突文件 (Thorough Check & Delete)
-- Target: Batch 2025010115, File '2024年年度报告.pdf'

BEGIN;

DO $$
DECLARE
    deleted_content_count INTEGER;
    deleted_batch_file_count INTEGER;
BEGIN
    -- 1. 删除档案内容表 (arc_file_content)
    DELETE FROM arc_file_content 
    WHERE (file_name = '2024年年度报告.pdf' OR file_name = '2024年年度报告') 
      AND batch_id = 2025010115
    RETURNING count(*) INTO deleted_content_count;

    RAISE NOTICE 'Deleted % rows from arc_file_content', deleted_content_count;

    -- 2. 删除批次文件表 (collection_batch_file)
    DELETE FROM collection_batch_file 
    WHERE original_filename = '2024年年度报告.pdf' 
      AND batch_id = 2025010115
    RETURNING count(*) INTO deleted_batch_file_count;

    RAISE NOTICE 'Deleted % rows from collection_batch_file', deleted_batch_file_count;

    -- 3. 强制提交 (Execute content)
    IF deleted_batch_file_count = 0 THEN
        RAISE NOTICE 'WARNING: No batch file records found to delete! Check batch_id/filename.';
    END IF;
END $$;

COMMIT;
