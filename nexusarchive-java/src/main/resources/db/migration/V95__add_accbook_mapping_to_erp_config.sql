-- Input: None
-- Output: Add accbook_mapping column to sys_erp_config
-- Pos: src/main/resources/db/migration/
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- ================================================================
-- Migration: V95__add_accbook_mapping_to_erp_config.sql
-- Purpose: 添加账套-全宗映射字段，实现后端强制路由和合规性控制
-- Author: Claude Code
-- Date: 2025-01-07
-- ================================================================

-- 添加账套-全宗映射字段
ALTER TABLE public.sys_erp_config
ADD COLUMN accbook_mapping TEXT;

COMMENT ON COLUMN public.sys_erp_config.accbook_mapping IS '账套-全宗映射JSON: {"BR01": "FONDS_A", "BR02": "FONDS_B"} - 合规性要求一个全宗只能映射一个账套';

-- 为 accbook_mapping 添加 GIN 索引用于 JSON 查询（PostgreSQL）
-- Note: 跳过索引创建，因为 accbook_mapping 是 TEXT 类型，GIN 索引需要 jsonb 类型
-- 如果需要索引，可以将列类型改为 jsonb 或使用表达式索引
-- CREATE INDEX idx_erp_config_accbook_mapping_gin ON public.sys_erp_config
-- USING GIN (CAST(accbook_mapping AS jsonb));

-- 更新现有数据：从 configJson 中提取 accbookCode 并建立映射
-- 注意：这只是迁移逻辑，实际映射需要系统管理员后续配置
UPDATE public.sys_erp_config
SET accbook_mapping = CASE
    WHEN CAST(config_json AS jsonb)->>'accbookCode' IS NOT NULL THEN
        '{"' || (CAST(config_json AS jsonb)->>'accbookCode') || '": "' ||
        COALESCE(org_id, (CAST(config_json AS jsonb)->>'accbookCode')) || '"}'
    ELSE NULL
END
WHERE accbook_mapping IS NULL
AND CAST(config_json AS jsonb)->>'accbookCode' IS NOT NULL;

-- 标记 org_id 为已废弃
COMMENT ON COLUMN public.sys_erp_config.org_id IS '关联组织ID (已废弃，请使用 accbook_mapping)';
