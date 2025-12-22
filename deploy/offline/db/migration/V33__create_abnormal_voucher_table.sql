-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

CREATE TABLE IF NOT EXISTS arc_abnormal_voucher (
    id VARCHAR(50) PRIMARY KEY,
    request_id VARCHAR(100),
    source_system VARCHAR(50),
    voucher_number VARCHAR(100),
    sip_data TEXT,
    fail_reason TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE arc_abnormal_voucher IS '异常凭证数据池';
COMMENT ON COLUMN arc_abnormal_voucher.id IS '主键ID';
COMMENT ON COLUMN arc_abnormal_voucher.request_id IS '请求ID';
COMMENT ON COLUMN arc_abnormal_voucher.source_system IS '来源系统';
COMMENT ON COLUMN arc_abnormal_voucher.voucher_number IS '凭证号';
COMMENT ON COLUMN arc_abnormal_voucher.sip_data IS '原始SIP数据(JSON)';
COMMENT ON COLUMN arc_abnormal_voucher.fail_reason IS '失败原因';
COMMENT ON COLUMN arc_abnormal_voucher.status IS '状态: PENDING/RETRYING/IGNORED/RESOLVED';
COMMENT ON COLUMN arc_abnormal_voucher.create_time IS '创建时间';
COMMENT ON COLUMN arc_abnormal_voucher.update_time IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_abnormal_status ON arc_abnormal_voucher(status);
CREATE INDEX IF NOT EXISTS idx_abnormal_create_time ON arc_abnormal_voucher(create_time);
