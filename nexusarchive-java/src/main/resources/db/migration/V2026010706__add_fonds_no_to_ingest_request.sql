-- Input: Flyway SQL 迁移脚本
-- Output: 为 sys_ingest_request_status 表添加全宗隔离支持
-- Pos: 数据库迁移

-- 为 sys_ingest_request_status 添加 fonds_no 字段
-- 确保 SIP 接收请求按全宗隔离

ALTER TABLE sys_ingest_request_status
ADD COLUMN IF NOT EXISTS fonds_no VARCHAR(50);

-- 添加索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_ingest_request_fonds_no
    ON sys_ingest_request_status(fonds_no);

-- 添加注释
COMMENT ON COLUMN sys_ingest_request_status.fonds_no IS '全宗号，用于数据隔离';
