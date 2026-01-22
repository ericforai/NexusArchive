-- Input: 修复 fonds_id 类型不一致问题
-- Output: 统一 fonds_id 为 varchar(32) 类型
-- Pos: 数据库迁移

-- 问题：archive_batch 和 period_lock 的 fonds_id 是 bigint，
-- 而系统中其他表（collection_batch, bas_fonds）使用 varchar，
-- 且实际的全宗代码是字符串格式（如 'fonds-brjt', 'BR-GROUP'）

-- 修复 archive_batch.fonds_id 类型
-- 注意：如果表中已有 bigint 数据，需要先处理数据迁移
ALTER TABLE archive_batch
    ALTER COLUMN fonds_id TYPE varchar(32) USING fonds_id::varchar;

-- 修复 period_lock.fonds_id 类型
ALTER TABLE period_lock
    ALTER COLUMN fonds_id TYPE varchar(32) USING fonds_id::varchar;

-- 添加注释
COMMENT ON COLUMN archive_batch.fonds_id IS '全宗 ID（字符串格式，如 fonds-brjt）';
COMMENT ON COLUMN period_lock.fonds_id IS '全宗 ID（字符串格式，如 fonds-brjt）';
