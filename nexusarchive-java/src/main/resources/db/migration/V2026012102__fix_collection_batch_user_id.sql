-- V2026012102: 修正 collection_batch 表的 created_by 列类型
-- 2026-01-21: 由于系统用户 ID 统一为 VARCHAR(64)，修正此处的 BIGINT 类型以避免解析错误

DO $$
BEGIN
    -- 1. 检查 created_by 是否为 BIGINT，若是则修改为 VARCHAR
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'collection_batch' 
        AND column_name = 'created_by' 
        AND data_type = 'bigint'
    ) THEN
        -- 修改列类型 (假设数据可以隐式转换为 VARCHAR)
        ALTER TABLE collection_batch ALTER COLUMN created_by TYPE VARCHAR(64);
        COMMENT ON COLUMN collection_batch.created_by IS '创建人ID (对应 sys_user.id)';
    END IF;

END $$;
