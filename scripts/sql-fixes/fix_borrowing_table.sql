-- 手动修复 biz_borrowing 表缺少 updated_at 列的问题
-- 直接执行此 SQL 到数据库

ALTER TABLE biz_borrowing ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP;

COMMENT ON COLUMN biz_borrowing.updated_at IS '更新时间';
