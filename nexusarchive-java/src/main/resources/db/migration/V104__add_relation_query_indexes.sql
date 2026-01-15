-- Input: Performance Indexes for relation query optimization
-- Output: Schema change for relation query performance optimization
-- Pos: db/migration/V104
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- ============================================
-- Relation Query Performance Indexes
-- ============================================
-- 说明: 为穿透联查功能添加性能优化索引
-- 优化目标: 减少关系查询的响应时间，从 2-5 秒降低到 <500ms
-- 
-- 适用场景:
-- - 穿透联查功能的关系查询
-- - 递归查找关联凭证
-- - 批量查询关系数据
--
-- 回滚 SQL (如需回滚，请手动执行以下语句):
-- DROP INDEX IF EXISTS idx_archive_relation_source_id;
-- DROP INDEX IF EXISTS idx_archive_relation_target_id;
-- DROP INDEX IF EXISTS idx_archive_relation_source_target;
-- DROP INDEX IF EXISTS idx_archive_code;
-- DROP INDEX IF EXISTS idx_voucher_relation_voucher_id;

-- ============================================
-- acc_archive_relation 表索引
-- ============================================

-- 1. source_id 索引
-- 用途: 优化以档案为源的关系查询（如查找发票关联的凭证）
CREATE INDEX IF NOT EXISTS idx_archive_relation_source_id
    ON acc_archive_relation(source_id)
    WHERE deleted = 0;

COMMENT ON INDEX idx_archive_relation_source_id IS
    '关系表源ID索引，用于优化以档案为源的关系查询（仅未删除记录）';

-- 2. target_id 索引
-- 用途: 优化以档案为目标的关系查询（如查找凭证关联的发票）
CREATE INDEX IF NOT EXISTS idx_archive_relation_target_id
    ON acc_archive_relation(target_id)
    WHERE deleted = 0;

COMMENT ON INDEX idx_archive_relation_target_id IS
    '关系表目标ID索引，用于优化以档案为目标的关系查询（仅未删除记录）';

-- 3. source_id + target_id 复合索引
-- 用途: 优化双向关系查询（同时查询源和目标）
CREATE INDEX IF NOT EXISTS idx_archive_relation_source_target
    ON acc_archive_relation(source_id, target_id)
    WHERE deleted = 0;

COMMENT ON INDEX idx_archive_relation_source_target IS
    '关系表源ID+目标ID复合索引，用于优化双向关系查询（仅未删除记录）';

-- 4. relation_type 索引（用于按关系类型筛选）
-- 用途: 优化按关系类型查询（如 ORIGINAL_VOUCHER、BASIS）
CREATE INDEX IF NOT EXISTS idx_archive_relation_type
    ON acc_archive_relation(relation_type)
    WHERE deleted = 0;

COMMENT ON INDEX idx_archive_relation_type IS
    '关系类型索引，用于优化按关系类型筛选查询（仅未删除记录）';

-- ============================================
-- acc_archive 表补充索引
-- ============================================

-- 5. archive_code 索引（如果还没有）
-- 用途: 优化按档号查询档案（fallback 查询场景）
CREATE INDEX IF NOT EXISTS idx_archive_code
    ON acc_archive(archive_code)
    WHERE deleted = 0;

COMMENT ON INDEX idx_archive_code IS
    '档号索引，用于优化按档号查询档案（仅未删除记录）';

-- ============================================
-- arc_voucher_relation 表索引
-- ============================================

-- 6. accounting_voucher_id 索引
-- 用途: 优化按记账凭证ID查询原始凭证关联
CREATE INDEX IF NOT EXISTS idx_voucher_relation_voucher_id
    ON arc_voucher_relation(accounting_voucher_id)
    WHERE deleted = 0;

COMMENT ON INDEX idx_voucher_relation_voucher_id IS
    '记账凭证ID索引，用于优化按记账凭证查询原始凭证关联（仅未删除记录）';

-- ============================================
-- arc_original_voucher 表补充索引
-- ============================================

-- 7. source_doc_id 索引
-- 用途: 优化按 source_doc_id 查询原始凭证对应的档案
CREATE INDEX IF NOT EXISTS idx_original_voucher_source_doc_id
    ON arc_original_voucher(source_doc_id)
    WHERE source_doc_id IS NOT NULL AND deleted = 0;

COMMENT ON INDEX idx_original_voucher_source_doc_id IS
    '原始凭证源文档ID索引，用于优化按源文档ID查询原始凭证（仅非空且未删除记录）';
