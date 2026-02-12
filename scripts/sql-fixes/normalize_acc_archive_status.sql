-- 统一 acc_archive.status 的大小写，避免查询过滤遗漏
-- 目标映射：
--   ARCHIVED -> archived
--   PENDING  -> pending

BEGIN;

UPDATE acc_archive
SET status = 'archived',
    last_modified_time = NOW()
WHERE status = 'ARCHIVED';

UPDATE acc_archive
SET status = 'pending',
    last_modified_time = NOW()
WHERE status = 'PENDING';

COMMIT;

