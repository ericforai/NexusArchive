-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 增加归档批次表，用于实现批处理哈希链 (Batch Hash Chain)
CREATE TABLE IF NOT EXISTS arc_archive_batch (
    id SERIAL PRIMARY KEY,
    batch_no VARCHAR(64) UNIQUE NOT NULL, -- 批次号 (SIP ID)
    prev_batch_hash VARCHAR(64),          -- 上一完整批次的哈希值
    current_batch_hash VARCHAR(64),       -- 当前批次数据的聚合哈希 (Merkle Root 或 级联哈希)
    chained_hash VARCHAR(64) NOT NULL,    -- 最终挂接哈希: SM3(prev_batch_hash + current_batch_hash)
    hash_algo VARCHAR(10) DEFAULT 'SM3',  -- 使用的哈希算法
    item_count INT DEFAULT 0,             -- 本批次包含的档案数量
    operator_id VARCHAR(50),              -- 操作员 ID
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 为档案内容表增加批次关联，实现防篡改追溯
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS batch_id INT;
ALTER TABLE arc_file_content ADD COLUMN IF NOT EXISTS sequence_in_batch INT;

COMMENT ON TABLE arc_archive_batch IS '归档批次存证表 (哈希链核心)';
COMMENT ON COLUMN arc_archive_batch.chained_hash IS '本批次防篡改挂接指纹';
COMMENT ON COLUMN arc_file_content.batch_id IS '关联的归档批次 ID';
