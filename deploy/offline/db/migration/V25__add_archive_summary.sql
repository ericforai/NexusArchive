-- V25__add_archive_summary.sql
-- 添加档案摘要字段
-- 
-- 合规要求:
-- - DA/T 94-2022: 元数据规范要求支持档案摘要
-- - 使用 SM4 加密存储敏感信息
--
-- @author Agent B - 合规开发工程师

-- 添加摘要字段
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS summary TEXT;

-- 添加注释
COMMENT ON COLUMN acc_archive.summary IS '档案摘要/说明 - SM4加密存储';
