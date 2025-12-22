-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- V6: 添加业务模块表 (借阅、销毁、库房)

-- 1. 借阅申请表
CREATE TABLE IF NOT EXISTS biz_borrowing (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    user_name VARCHAR(64),
    archive_id VARCHAR(64) NOT NULL,
    archive_title VARCHAR(255),
    reason VARCHAR(512),
    borrow_date DATE,
    expected_return_date DATE,
    actual_return_date DATE,
    status VARCHAR(32) DEFAULT 'PENDING',
    approval_comment VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE biz_borrowing IS '借阅申请表';
COMMENT ON COLUMN biz_borrowing.id IS '主键ID';
COMMENT ON COLUMN biz_borrowing.user_id IS '申请人ID';
COMMENT ON COLUMN biz_borrowing.user_name IS '申请人姓名';
COMMENT ON COLUMN biz_borrowing.archive_id IS '借阅档案ID';
COMMENT ON COLUMN biz_borrowing.archive_title IS '档案题名';
COMMENT ON COLUMN biz_borrowing.reason IS '借阅原因';
COMMENT ON COLUMN biz_borrowing.borrow_date IS '借阅日期';
COMMENT ON COLUMN biz_borrowing.expected_return_date IS '预计归还日期';
COMMENT ON COLUMN biz_borrowing.actual_return_date IS '实际归还日期';
COMMENT ON COLUMN biz_borrowing.status IS '状态: PENDING, APPROVED, REJECTED, RETURNED, CANCELLED';
COMMENT ON COLUMN biz_borrowing.approval_comment IS '审批意见';
COMMENT ON COLUMN biz_borrowing.created_at IS '创建时间';
COMMENT ON COLUMN biz_borrowing.updated_at IS '更新时间';
COMMENT ON COLUMN biz_borrowing.deleted IS '逻辑删除标识';

CREATE INDEX IF NOT EXISTS idx_borrowing_user ON biz_borrowing(user_id);
CREATE INDEX IF NOT EXISTS idx_borrowing_status ON biz_borrowing(status);

-- 2. 销毁申请表
CREATE TABLE IF NOT EXISTS biz_destruction (
    id VARCHAR(64) PRIMARY KEY,
    applicant_id VARCHAR(64) NOT NULL,
    applicant_name VARCHAR(64),
    reason VARCHAR(512),
    archive_count INT DEFAULT 0,
    archive_ids TEXT,
    status VARCHAR(32) DEFAULT 'PENDING',
    approver_id VARCHAR(64),
    approver_name VARCHAR(64),
    approval_comment VARCHAR(512),
    approval_time TIMESTAMP,
    execution_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE biz_destruction IS '销毁申请表';
COMMENT ON COLUMN biz_destruction.id IS '主键ID';
COMMENT ON COLUMN biz_destruction.applicant_id IS '申请人ID';
COMMENT ON COLUMN biz_destruction.applicant_name IS '申请人姓名';
COMMENT ON COLUMN biz_destruction.reason IS '销毁原因';
COMMENT ON COLUMN biz_destruction.archive_count IS '待销毁档案数量';
COMMENT ON COLUMN biz_destruction.archive_ids IS '待销毁档案ID列表(JSON)';
COMMENT ON COLUMN biz_destruction.status IS '状态: PENDING, APPROVED, REJECTED, EXECUTED';
COMMENT ON COLUMN biz_destruction.approver_id IS '审批人ID';
COMMENT ON COLUMN biz_destruction.approver_name IS '审批人姓名';
COMMENT ON COLUMN biz_destruction.approval_comment IS '审批意见';
COMMENT ON COLUMN biz_destruction.approval_time IS '审批时间';
COMMENT ON COLUMN biz_destruction.execution_time IS '执行时间';
COMMENT ON COLUMN biz_destruction.created_at IS '创建时间';
COMMENT ON COLUMN biz_destruction.updated_at IS '更新时间';
COMMENT ON COLUMN biz_destruction.deleted IS '逻辑删除标识';

CREATE INDEX IF NOT EXISTS idx_destruction_status ON biz_destruction(status);

-- 3. 库房/位置表
CREATE TABLE IF NOT EXISTS bas_location (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64),
    type VARCHAR(32) NOT NULL,
    parent_id VARCHAR(64) DEFAULT '0',
    path VARCHAR(255),
    capacity INT DEFAULT 0,
    used_count INT DEFAULT 0,
    status VARCHAR(32) DEFAULT 'NORMAL',
    rfid_tag VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE bas_location IS '库房位置表';
COMMENT ON COLUMN bas_location.id IS '主键ID';
COMMENT ON COLUMN bas_location.name IS '位置名称';
COMMENT ON COLUMN bas_location.code IS '位置编码';
COMMENT ON COLUMN bas_location.type IS '类型: WAREHOUSE, AREA, SHELF, BOX';
COMMENT ON COLUMN bas_location.parent_id IS '父级ID';
COMMENT ON COLUMN bas_location.path IS '完整路径';
COMMENT ON COLUMN bas_location.capacity IS '容量';
COMMENT ON COLUMN bas_location.used_count IS '已用数量';
COMMENT ON COLUMN bas_location.status IS '状态: NORMAL, FULL, MAINTENANCE';
COMMENT ON COLUMN bas_location.rfid_tag IS 'RFID标签号';
COMMENT ON COLUMN bas_location.created_at IS '创建时间';
COMMENT ON COLUMN bas_location.updated_at IS '更新时间';
COMMENT ON COLUMN bas_location.deleted IS '逻辑删除标识';

CREATE INDEX IF NOT EXISTS idx_location_parent ON bas_location(parent_id);
CREATE INDEX IF NOT EXISTS idx_location_type ON bas_location(type);
