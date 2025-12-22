-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 增加 acc_archive 表关键字段长度以支持完整档号和UUID
-- 档号格式: COMP001-2025-10Y-DEPT001-AC04-000001 (36+字符)
ALTER TABLE acc_archive ALTER COLUMN archive_code TYPE VARCHAR(64);

-- ID 格式: UUID (36字符)
ALTER TABLE acc_archive ALTER COLUMN id TYPE VARCHAR(64);

-- 同时也需要更新关联表 biz_archive_approval 的 archive_id 长度
ALTER TABLE biz_archive_approval ALTER COLUMN archive_id TYPE VARCHAR(64);
