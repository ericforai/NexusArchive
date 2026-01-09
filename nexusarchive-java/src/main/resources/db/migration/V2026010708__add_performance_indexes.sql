-- Input: Performance Indexes for acc_archive and arc_file_content tables
-- Output: Schema change for query performance optimization
-- Pos: db/migration/V2026010708
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- ============================================
-- Performance Indexes
-- ============================================
-- 说明: 为 acc_archive 和 arc_file_content 表添加性能优化索引
-- 使用 CONCURRENTLY 避免锁表，适合生产环境在线执行
-- 使用部分索引 (WHERE 条件) 优化存储空间
--
-- 适用场景:
-- - 部门维度查询优化
-- - 全宗+状态+年度组合查询优化
-- - 会计期间+年度组合查询优化
-- - 批次处理状态查询优化
-- - 全宗预归档状态查询优化
--
-- 回滚 SQL (如需回滚，请手动执行以下语句):
-- DROP INDEX IF EXISTS idx_acc_archive_department_id;
-- DROP INDEX IF EXISTS idx_acc_archive_fonds_status_year;
-- DROP INDEX IF EXISTS idx_acc_archive_fiscal_period_year;
-- DROP INDEX IF EXISTS idx_arc_file_content_batch_status;
-- DROP INDEX IF EXISTS idx_arc_file_content_fonds_status;

-- ============================================
-- acc_archive 表补充索引
-- ============================================

-- 1. 部门ID索引 (部分索引，仅索引非空值)
-- 用途: 按部门筛选档案，优化部门权限控制查询
CREATE INDEX IF NOT EXISTS idx_acc_archive_department_id
    ON acc_archive(department_id)
    WHERE department_id IS NOT NULL;

COMMENT ON INDEX idx_acc_archive_department_id IS
    '部门ID部分索引，用于按部门筛选档案查询优化';

-- 2. 全宗+状态+年度复合索引 (部分索引，仅索引未删除记录)
-- 用途: 全宗维度的状态查询，支持按年度统计和筛选
CREATE INDEX IF NOT EXISTS idx_acc_archive_fonds_status_year
    ON acc_archive(fonds_no, status, fiscal_year)
    WHERE deleted = 0;

COMMENT ON INDEX idx_acc_archive_fonds_status_year IS
    '全宗+状态+年度复合索引，用于全宗维度状态查询和统计优化（仅未删除记录）';

-- 3. 会计期间+年度复合索引 (部分索引，仅索引未删除记录)
-- 用途: 按会计期间和年度查询，优化期间筛选性能
CREATE INDEX IF NOT EXISTS idx_acc_archive_fiscal_period_year
    ON acc_archive(fiscal_period, fiscal_year)
    WHERE deleted = 0;

COMMENT ON INDEX idx_acc_archive_fiscal_period_year IS
    '会计期间+年度复合索引，用于期间维度查询优化（仅未删除记录）';

-- ============================================
-- arc_file_content 表补充索引
-- ============================================

-- 4. 批次ID+预归档状态复合索引 (部分索引，仅索引批次关联记录)
-- 用途: 批次处理时的文件状态查询，优化批量归档流程
CREATE INDEX IF NOT EXISTS idx_arc_file_content_batch_status
    ON arc_file_content(batch_id, pre_archive_status)
    WHERE batch_id IS NOT NULL;

COMMENT ON INDEX idx_arc_file_content_batch_status IS
    '批次ID+预归档状态复合索引，用于批次处理状态查询优化';

-- 5. 全宗代码+预归档状态复合索引 (部分索引，仅索引有全宗代码的记录)
-- 用途: 全宗维度的文件预归档状态查询，优化全宗隔离场景
CREATE INDEX IF NOT EXISTS idx_arc_file_content_fonds_status
    ON arc_file_content(fonds_code, pre_archive_status)
    WHERE fonds_code IS NOT NULL;

COMMENT ON INDEX idx_arc_file_content_fonds_status IS
    '全宗代码+预归档状态复合索引，用于全宗维度预归档状态查询优化';
