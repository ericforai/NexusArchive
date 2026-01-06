-- 借阅档案明细表
CREATE TABLE acc_borrow_archive (
    id VARCHAR(36) PRIMARY KEY,
    borrow_request_id VARCHAR(36) NOT NULL,        -- 借阅申请ID
    archive_id VARCHAR(36) NOT NULL,               -- 档案ID
    archive_code VARCHAR(100) NOT NULL,           -- 档号
    archive_title VARCHAR(500) NOT NULL,          -- 题名
    return_status VARCHAR(20) DEFAULT 'BORROWED',  -- 归还状态: BORROWED, RETURNED
    return_time TIMESTAMP,                         -- 归还时间
    return_operator_id VARCHAR(36),               -- 归还操作人ID
    damaged BOOLEAN DEFAULT FALSE,                  -- 是否损坏
    damage_desc VARCHAR(500),                      -- 损坏描述
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_borrow_archive_request ON acc_borrow_archive(borrow_request_id);
CREATE INDEX idx_borrow_archive_archive ON acc_borrow_archive(archive_id);
COMMENT ON TABLE acc_borrow_archive IS '借阅档案明细表';
