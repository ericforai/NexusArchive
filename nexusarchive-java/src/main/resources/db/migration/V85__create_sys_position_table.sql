-- 职位表 (sys_position)
-- 用于管理系统中的职位信息

CREATE TABLE IF NOT EXISTS sys_position (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    department_id VARCHAR(32),
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'active',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

-- 索引
CREATE UNIQUE INDEX IF NOT EXISTS uk_position_code ON sys_position(code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_position_department ON sys_position(department_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_position_status ON sys_position(status) WHERE deleted = 0;

-- 注释
COMMENT ON TABLE sys_position IS '系统职位表';
COMMENT ON COLUMN sys_position.name IS '职位名称';
COMMENT ON COLUMN sys_position.code IS '职位编码';
COMMENT ON COLUMN sys_position.department_id IS '所属部门ID';
COMMENT ON COLUMN sys_position.description IS '职位描述';
COMMENT ON COLUMN sys_position.status IS '状态: active/disabled';
COMMENT ON COLUMN sys_position.created_time IS '创建时间';
COMMENT ON COLUMN sys_position.updated_time IS '更新时间';
COMMENT ON COLUMN sys_position.deleted IS '逻辑删除标记';
