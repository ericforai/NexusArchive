-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
