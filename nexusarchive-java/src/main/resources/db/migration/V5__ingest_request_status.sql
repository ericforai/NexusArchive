-- V5: 添加接收请求状态追踪表
-- 用于支持事件驱动架构中的异步处理状态查询

CREATE TABLE IF NOT EXISTS sys_ingest_request_status (
    request_id VARCHAR(64) PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    message TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_ingest_request_status IS 'SIP接收请求状态追踪表';
COMMENT ON COLUMN sys_ingest_request_status.request_id IS '请求ID';
COMMENT ON COLUMN sys_ingest_request_status.status IS '状态: RECEIVED, CHECKING, CHECK_PASSED, PROCESSING, COMPLETED, FAILED';
COMMENT ON COLUMN sys_ingest_request_status.message IS '详细消息或错误信息';
COMMENT ON COLUMN sys_ingest_request_status.created_time IS '创建时间';
COMMENT ON COLUMN sys_ingest_request_status.updated_time IS '更新时间';

-- 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_status ON sys_ingest_request_status(status);
CREATE INDEX IF NOT EXISTS idx_created_time ON sys_ingest_request_status(created_time);
