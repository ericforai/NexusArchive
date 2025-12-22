-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- ============================================================================
-- V64: ERP 回写失败重试队列表
-- 功能: 存证溯源 - 归档编号异步写回 ERP 系统失败时入队等待重试
-- 依据: DA/T 94-2022 存证规范，确保档号与源系统同步
-- ============================================================================

-- 1. 创建 ERP 回写重试队列表
CREATE TABLE IF NOT EXISTS sys_erp_feedback_queue (
    id BIGSERIAL PRIMARY KEY,
    voucher_id VARCHAR(64) NOT NULL,           -- ERP 凭证/单据 ID
    archival_code VARCHAR(128) NOT NULL,       -- 生成的档号
    erp_type VARCHAR(32) NOT NULL,             -- ERP 类型 (YONSUITE, KINGDEE 等)
    erp_config_id BIGINT,                      -- 关联的 ERP 配置 ID
    retry_count INT DEFAULT 0,                 -- 已重试次数
    max_retries INT DEFAULT 3,                 -- 最大重试次数
    last_error TEXT,                           -- 最后一次错误信息
    status VARCHAR(16) DEFAULT 'PENDING',      -- 状态: PENDING, RETRYING, SUCCESS, FAILED
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    next_retry_time TIMESTAMP                  -- 下次重试时间 (指数退避)
);

-- 2. 添加注释
COMMENT ON TABLE sys_erp_feedback_queue IS 'ERP 回写失败重试队列 - 存证溯源';
COMMENT ON COLUMN sys_erp_feedback_queue.voucher_id IS 'ERP 凭证/单据 ID';
COMMENT ON COLUMN sys_erp_feedback_queue.archival_code IS '生成的档号';
COMMENT ON COLUMN sys_erp_feedback_queue.erp_type IS 'ERP 类型 (YONSUITE, KINGDEE 等)';
COMMENT ON COLUMN sys_erp_feedback_queue.retry_count IS '已重试次数';
COMMENT ON COLUMN sys_erp_feedback_queue.status IS '状态: PENDING-待重试, RETRYING-重试中, SUCCESS-成功, FAILED-放弃';
COMMENT ON COLUMN sys_erp_feedback_queue.next_retry_time IS '下次重试时间 (指数退避算法)';

-- 3. 创建索引
CREATE INDEX IF NOT EXISTS idx_feedback_queue_status ON sys_erp_feedback_queue(status);
CREATE INDEX IF NOT EXISTS idx_feedback_queue_next_retry ON sys_erp_feedback_queue(next_retry_time) WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_feedback_queue_erp_type ON sys_erp_feedback_queue(erp_type);
