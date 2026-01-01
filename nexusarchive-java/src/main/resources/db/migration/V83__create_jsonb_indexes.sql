-- Input: 数据库引擎
-- Output: 检索索引创建脚本
-- Pos: Flyway 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- ============================================
-- 检索索引优化
-- ============================================
-- 说明: 为 acc_archive 表创建优化索引，提升检索性能
-- OpenSpec 来源: openspec-p1-search-index-optimization.md
-- 任务: P1.3 检索索引优化

-- 1. JSONB GIN 索引（用于 JSONB 字段查询）
CREATE INDEX IF NOT EXISTS idx_archive_custom_metadata_gin
ON acc_archive USING GIN (custom_metadata);

CREATE INDEX IF NOT EXISTS idx_archive_standard_metadata_gin
ON acc_archive USING GIN (standard_metadata);

-- 2. 常用字段索引（如果不存在，避免重复创建）
-- 注意: 这些索引可能在 V70 中已创建，使用 IF NOT EXISTS 确保幂等性
CREATE INDEX IF NOT EXISTS idx_archive_fonds_no
ON acc_archive (fonds_no);

CREATE INDEX IF NOT EXISTS idx_archive_fiscal_year
ON acc_archive (fiscal_year);

CREATE INDEX IF NOT EXISTS idx_archive_category_code
ON acc_archive (category_code);

CREATE INDEX IF NOT EXISTS idx_archive_status
ON acc_archive (status);

-- 3. 复合索引（多条件查询优化）
CREATE INDEX IF NOT EXISTS idx_archive_fonds_year_category
ON acc_archive (fonds_no, fiscal_year, category_code);

CREATE INDEX IF NOT EXISTS idx_archive_fonds_status
ON acc_archive (fonds_no, status)
WHERE status IN ('archived', 'pending');

CREATE INDEX IF NOT EXISTS idx_archive_year_status
ON acc_archive (fiscal_year, status);

-- 4. 部分索引（优化特定查询场景）
-- 只索引已归档的档案（最常用查询）
CREATE INDEX IF NOT EXISTS idx_archive_archived_fonds_year
ON acc_archive (fonds_no, fiscal_year)
WHERE status = 'archived';

-- 5. 表达式索引（常用 JSONB 路径查询）
-- 如果经常查询 custom_metadata 中的 bookType 字段
CREATE INDEX IF NOT EXISTS idx_archive_custom_booktype
ON acc_archive ((custom_metadata->>'bookType'))
WHERE custom_metadata->>'bookType' IS NOT NULL;

-- 索引注释
COMMENT ON INDEX idx_archive_custom_metadata_gin IS 'custom_metadata JSONB GIN 索引，用于 JSONB 包含和键值查询';
COMMENT ON INDEX idx_archive_standard_metadata_gin IS 'standard_metadata JSONB GIN 索引，用于 JSONB 包含和键值查询';
COMMENT ON INDEX idx_archive_fonds_year_category IS '全宗号+年度+分类号复合索引，用于多条件组合查询';
COMMENT ON INDEX idx_archive_archived_fonds_year IS '已归档档案的部分索引，优化常用查询';
