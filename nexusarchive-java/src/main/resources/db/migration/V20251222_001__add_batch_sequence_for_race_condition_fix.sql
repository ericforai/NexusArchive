-- [FIXED P0-4] 创建批次序列号，防止哈希链并发竞态条件
-- 依据：使用数据库序列保证批次顺序的唯一性和原子性

-- 1. 创建序列
CREATE SEQUENCE IF NOT EXISTS arc_batch_seq START 1;

-- 2. 添加序列号列
ALTER TABLE arc_archive_batch ADD COLUMN IF NOT EXISTS batch_sequence BIGINT;

-- 3. 为现有数据回填序列号（按 ID 顺序）
DO $$
DECLARE
    rec RECORD;
    seq_num BIGINT := 1;
BEGIN
    FOR rec IN SELECT id FROM arc_archive_batch ORDER BY id ASC
    LOOP
        UPDATE arc_archive_batch SET batch_sequence = seq_num WHERE id = rec.id;
        seq_num := seq_num + 1;
    END LOOP;
    
    -- 重置序列到当前最大值 + 1
    PERFORM setval('arc_batch_seq', seq_num);
END $$;

-- 4. 设置序列号为非空且唯一
ALTER TABLE arc_archive_batch ALTER COLUMN batch_sequence SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_batch_sequence ON arc_archive_batch(batch_sequence);

-- 5. 添加注释
COMMENT ON COLUMN arc_archive_batch.batch_sequence IS '批次序列号，用于防止哈希链并发竞态条件';
COMMENT ON SEQUENCE arc_batch_seq IS '批次序列生成器，保证哈希链顺序唯一性';
