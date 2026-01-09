-- Input: 数据库引擎
-- Output: JSONB 表达式索引增强脚本
-- Pos: Flyway 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- ============================================
-- JSONB 表达式索引增强
-- ============================================
-- 说明: 为 acc_archive 表的 JSONB 字段创建表达式索引，优化特定路径查询性能
-- OpenSpec 来源: openspec-p1-search-index-optimization.md
-- 任务: P1.3 检索索引优化增强
--
-- 当前状态: V83 已创建基础 GIN 索引
-- 本迁移: 添加高频查询路径的表达式索引

-- ============================================
-- 1. custom_metadata 表达式索引
-- ============================================

-- 1.1 reportType 索引 (AC03 报税表单类型查询)
-- 查询: custom_metadata::jsonb @> '{"reportType":"..."}'::jsonb
CREATE INDEX IF NOT EXISTS idx_acc_archive_custom_report_type
ON acc_archive ((custom_metadata->>'reportType'))
WHERE custom_metadata->>'reportType' IS NOT NULL;

-- 1.2 otherType 索引 (AC04 其他材料类型查询)
-- 查询: custom_metadata::jsonb @> '{"otherType":"..."}'::jsonb
CREATE INDEX IF NOT EXISTS idx_acc_archive_custom_other_type
ON acc_archive ((custom_metadata->>'otherType'))
WHERE custom_metadata->>'otherType' IS NOT NULL;

-- 1.3 增强 bookType 索引 (V83 已创建，这里添加注释完善)
-- 查询: custom_metadata->>'bookType' IS NOT NULL
-- 已存在: idx_archive_custom_booktype (V83)
COMMENT ON INDEX idx_archive_custom_booktype IS
'凭证册类型表达式索引，优化 AC02 类别的 bookType 精确查询';

-- ============================================
-- 2. standard_metadata 表达式索引
-- ============================================

-- 2.1 invoiceNumber 索引 (发票号匹配)
-- 查询: 用于发票号匹配策略 (InvoiceNumberMatchStrategy)
CREATE INDEX IF NOT EXISTS idx_acc_archive_standard_invoice_number
ON acc_archive ((standard_metadata->>'invoiceNumber'))
WHERE standard_metadata->>'invoiceNumber' IS NOT NULL;

-- 2.2 sellerName 索引 (销方名称搜索)
-- 查询: 常用于发票检索和关联
CREATE INDEX IF NOT EXISTS idx_acc_archive_standard_seller_name
ON acc_archive ((standard_metadata->>'sellerName'))
WHERE standard_metadata->>'sellerName' IS NOT NULL;

-- 2.3 buyerName 索引 (购方名称搜索)
CREATE INDEX IF NOT EXISTS idx_acc_archive_standard_buyer_name
ON acc_archive ((standard_metadata->>'buyerName'))
WHERE standard_metadata->>'buyerName' IS NOT NULL;

-- ============================================
-- 3. 优化 GIN 索引 (使用 jsonb_path_ops)
-- ============================================
--
-- jsonb_path_ops 优缺点:
-- - 优点: 索引体积更小 (约 1/3)，对 @> 包含操作符性能更好
-- - 缺点: 不支持 ? (键存在) 和 ?| (任一键存在) 操作符
-- - 适用场景: 主要使用 @> 包含查询的场景
--
-- 保留默认 GIN 索引用于通用查询，添加 jsonb_path_ops 用于包含查询优化

-- 3.1 custom_metadata jsonb_path_ops 索引
CREATE INDEX IF NOT EXISTS idx_acc_archive_custom_metadata_path_ops
ON acc_archive USING GIN (custom_metadata jsonb_path_ops);

-- 3.2 standard_metadata jsonb_path_ops 索引
CREATE INDEX IF NOT EXISTS idx_acc_archive_standard_metadata_path_ops
ON acc_archive USING GIN (standard_metadata jsonb_path_ops);

-- ============================================
-- 4. 复合表达式索引 (多条件 JSONB 查询)
-- ============================================

-- 4.1 category + bookType 复合索引 (AC02 常用查询)
CREATE INDEX IF NOT EXISTS idx_acc_archive_category_booktype
ON acc_archive (category_code, (custom_metadata->>'bookType'))
WHERE category_code = 'AC02' AND custom_metadata->>'bookType' IS NOT NULL;

-- 4.2 category + reportType 复合索引 (AC03 常用查询)
CREATE INDEX IF NOT EXISTS idx_acc_archive_category_reporttype
ON acc_archive (category_code, (custom_metadata->>'reportType'))
WHERE category_code = 'AC03' AND custom_metadata->>'reportType' IS NOT NULL;

-- 4.3 fonds + category + bookType 复合索引 (全宗隔离 + 分类查询)
CREATE INDEX IF NOT EXISTS idx_acc_archive_fonds_category_booktype
ON acc_archive (fonds_no, category_code, (custom_metadata->>'bookType'))
WHERE category_code = 'AC02' AND custom_metadata->>'bookType' IS NOT NULL;

-- ============================================
-- 5. 索引注释
-- ============================================

COMMENT ON INDEX idx_acc_archive_custom_report_type IS
'AC03 报税表单类型表达式索引，优化 reportType 精确查询';

COMMENT ON INDEX idx_acc_archive_custom_other_type IS
'AC04 其他材料类型表达式索引，优化 otherType 精确查询';

COMMENT ON INDEX idx_acc_archive_standard_invoice_number IS
'standard_metadata 发票号表达式索引，用于发票号匹配策略';

COMMENT ON INDEX idx_acc_archive_standard_seller_name IS
'standard_metadata 销方名称表达式索引，优化发票检索';

COMMENT ON INDEX idx_acc_archive_standard_buyer_name IS
'standard_metadata 购方名称表达式索引，优化发票检索';

COMMENT ON INDEX idx_acc_archive_custom_metadata_path_ops IS
'custom_metadata jsonb_path_ops GIN 索引，优化 @> 包含查询，索引体积更小';

COMMENT ON INDEX idx_acc_archive_standard_metadata_path_ops IS
'standard_metadata jsonb_path_ops GIN 索引，优化 @> 包含查询，索引体积更小';

COMMENT ON INDEX idx_acc_archive_category_booktype IS
'分类号 + bookType 复合表达式索引，优化 AC02 类别查询';

COMMENT ON INDEX idx_acc_archive_category_reporttype IS
'分类号 + reportType 复合表达式索引，优化 AC03 类别查询';

COMMENT ON INDEX idx_acc_archive_fonds_category_booktype IS
'全宗号 + 分类号 + bookType 复合表达式索引，优化全宗隔离下的分类查询';
