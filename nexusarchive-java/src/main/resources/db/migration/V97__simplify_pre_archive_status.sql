-- Input: 10-state pre_archive_status column
-- Output: 5-state pre_archive_status column
-- Pos: src/main/resources/db/migration/
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- ================================================================
-- Migration: V97__simplify_pre_archive_status.sql
-- Purpose: 简化预归档状态，从 10 个状态合并为 5 个核心状态
-- Author: Claude Code
-- Date: 2026-01-11
-- ================================================================
-- 注意：此迁移会永久改变 pre_archive_status 的值，执行前请备份数据
-- 回滚策略：需要从数据库备份恢复

BEGIN;

-- 步骤 1: 添加新的状态列（临时）
ALTER TABLE public.arc_file_content
ADD COLUMN pre_archive_status_new VARCHAR(20);

COMMENT ON COLUMN public.arc_file_content.pre_archive_status_new IS '简化后的预归档状态：PENDING_CHECK/NEEDS_ACTION/READY_TO_MATCH/READY_TO_ARCHIVE/COMPLETED';

-- 步骤 2: 将旧状态映射到新状态
UPDATE public.arc_file_content
SET pre_archive_status_new = CASE pre_archive_status
    -- PENDING_CHECK: 合并 DRAFT + PENDING_CHECK
    WHEN 'DRAFT' THEN 'PENDING_CHECK'
    WHEN 'PENDING_CHECK' THEN 'PENDING_CHECK'

    -- NEEDS_ACTION: 合并 CHECK_FAILED + PENDING_METADATA
    WHEN 'CHECK_FAILED' THEN 'NEEDS_ACTION'
    WHEN 'PENDING_METADATA' THEN 'NEEDS_ACTION'

    -- READY_TO_MATCH: 合并 MATCH_PENDING + MATCHED
    WHEN 'MATCH_PENDING' THEN 'READY_TO_MATCH'
    WHEN 'MATCHED' THEN 'READY_TO_MATCH'

    -- READY_TO_ARCHIVE: 原名 PENDING_ARCHIVE
    WHEN 'PENDING_ARCHIVE' THEN 'READY_TO_ARCHIVE'

    -- COMPLETED: 合并 PENDING_APPROVAL + ARCHIVING + ARCHIVED
    WHEN 'PENDING_APPROVAL' THEN 'COMPLETED'
    WHEN 'ARCHIVING' THEN 'COMPLETED'
    WHEN 'ARCHIVED' THEN 'COMPLETED'

    -- 兜底：未知状态归到待检测
    ELSE 'PENDING_CHECK'
END
WHERE pre_archive_status IS NOT NULL;

-- 步骤 3: 删除旧列，重命名新列
ALTER TABLE public.arc_file_content DROP COLUMN pre_archive_status;
ALTER TABLE public.arc_file_content RENAME COLUMN pre_archive_status_new TO pre_archive_status;

-- 步骤 4: 添加 NOT NULL 约束和默认值
ALTER TABLE public.arc_file_content
ALTER COLUMN pre_archive_status SET NOT NULL;

ALTER TABLE public.arc_file_content
ALTER COLUMN pre_archive_status SET DEFAULT 'PENDING_CHECK';

-- 步骤 5: 创建索引
CREATE INDEX idx_arc_file_content_pre_archive_status
ON public.arc_file_content(pre_archive_status);

COMMENT ON INDEX public.idx_arc_file_content_pre_archive_status IS '预归档状态索引 - 用于仪表板统计和筛选';

-- 数据完整性验证：确保所有行都被正确映射
DO $$
DECLARE
    null_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO null_count
    FROM public.arc_file_content
    WHERE pre_archive_status IS NULL;

    IF null_count > 0 THEN
        RAISE EXCEPTION '迁移后存在 NULL 状态值，数据完整性验证失败';
    END IF;
END $$;

COMMIT;
