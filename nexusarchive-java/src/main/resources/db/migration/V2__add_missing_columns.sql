-- 添加 arc_file_content 表缺失的列
-- 2025-12-29: 修复 Entity 与数据库 Schema 不一致问题
-- 根本原因：Entity 字段添加后未同步创建 Flyway 迁移脚本

-- 添加 summary 列（幂等）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'arc_file_content' AND column_name = 'summary'
    ) THEN
        ALTER TABLE arc_file_content ADD COLUMN summary TEXT;
        COMMENT ON COLUMN arc_file_content.summary IS '摘要/备注';
    END IF;
END $$;

-- 添加 voucher_word 列（幂等）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'arc_file_content' AND column_name = 'voucher_word'
    ) THEN
        ALTER TABLE arc_file_content ADD COLUMN voucher_word VARCHAR(50);
        COMMENT ON COLUMN arc_file_content.voucher_word IS '凭证字（如：记、收、付）';
    END IF;
END $$;

-- 添加 doc_date 列（幂等）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'arc_file_content' AND column_name = 'doc_date'
    ) THEN
        ALTER TABLE arc_file_content ADD COLUMN doc_date DATE;
        COMMENT ON COLUMN arc_file_content.doc_date IS '单据日期';
    END IF;
END $$;

-- 添加 highlight_meta 列（幂等）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'arc_file_content' AND column_name = 'highlight_meta'
    ) THEN
        ALTER TABLE arc_file_content ADD COLUMN highlight_meta JSONB;
        COMMENT ON COLUMN arc_file_content.highlight_meta IS '文件高亮元数据(坐标信息)';
    END IF;
END $$;
