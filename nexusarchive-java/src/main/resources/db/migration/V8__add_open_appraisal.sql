-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- V8: 添加-- 开放鉴定表 (Open Appraisal Table)
-- 用于记录档案的开放性鉴定过程
CREATE TABLE IF NOT EXISTS biz_open_appraisal (
    id VARCHAR(64) PRIMARY KEY,
    archive_id VARCHAR(64) NOT NULL,
    archive_code VARCHAR(100),
    archive_title VARCHAR(500),
    retention_period VARCHAR(20),
    current_security_level VARCHAR(20),
    appraiser_id VARCHAR(64),
    appraiser_name VARCHAR(100),
    appraisal_date DATE,
    appraisal_result VARCHAR(20),
    open_level VARCHAR(20),
    reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0
);

-- 创建索引 (使用 IF NOT EXISTS 确保幂等性)
CREATE INDEX IF NOT EXISTS idx_open_appraisal_status ON biz_open_appraisal(status);
CREATE INDEX IF NOT EXISTS idx_open_appraisal_archive_id ON biz_open_appraisal(archive_id);
CREATE INDEX IF NOT EXISTS idx_open_appraisal_result ON biz_open_appraisal(appraisal_result);
CREATE INDEX IF NOT EXISTS idx_open_appraisal_created_at ON biz_open_appraisal(created_at);

COMMENT ON TABLE biz_open_appraisal IS '开放鉴定表';
