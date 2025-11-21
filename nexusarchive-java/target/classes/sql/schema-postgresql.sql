-- ============================================================================
-- NexusArchive 电子会计档案管理系统 - PostgreSQL Schema
-- ============================================================================
-- 版本: 2.0.0
-- 数据库: PostgreSQL 14+
-- 合规标准: DA/T 94-2022, GB/T 39784-2021
-- ============================================================================

-- 创建数据库
CREATE DATABASE nexusarchive
    WITH 
    ENCODING = 'UTF8'
    LC_COLLATE = 'zh_CN.UTF-8'
    LC_CTYPE = 'zh_CN.UTF-8'
    TEMPLATE = template0;

\c nexusarchive;

-- ============================================================================
-- 1. 组织架构
-- ============================================================================

-- 部门表
CREATE TABLE sys_department (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE,
    parent_id VARCHAR(32),
    manager_id VARCHAR(32),
    description VARCHAR(500),
    "order" INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'active',
    type VARCHAR(20) DEFAULT 'department',
    path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

COMMENT ON TABLE sys_department IS '部门表';
COMMENT ON COLUMN sys_department.name IS '部门名称';
COMMENT ON COLUMN sys_department.parent_id IS '上级部门ID';
COMMENT ON COLUMN sys_department.manager_id IS '部门负责人ID';
COMMENT ON COLUMN sys_department.path IS '部门路径 /root/id1/id2';

CREATE INDEX idx_dept_parent ON sys_department(parent_id);
CREATE INDEX idx_dept_code ON sys_department(code);

-- ============================================================================
-- 2. 三员管理与权限体系
-- ============================================================================

-- 角色表 (三员管理)
CREATE TABLE sys_role (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    role_category VARCHAR(20) NOT NULL,
    is_exclusive BOOLEAN DEFAULT FALSE,
    description VARCHAR(200),
    permissions TEXT,
    data_scope VARCHAR(20) DEFAULT 'self',
    type VARCHAR(20) DEFAULT 'custom',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

COMMENT ON TABLE sys_role IS '系统角色表(三员管理)';
COMMENT ON COLUMN sys_role.role_category IS '角色类别: system_admin/security_admin/audit_admin/business_user';
COMMENT ON COLUMN sys_role.is_exclusive IS '是否互斥(三员角色)';
COMMENT ON COLUMN sys_role.permissions IS 'JSON权限列表';
COMMENT ON COLUMN sys_role.data_scope IS '数据权限范围: all/dept/dept_and_child/self';

CREATE INDEX idx_role_code ON sys_role(code);
CREATE INDEX idx_role_category ON sys_role(role_category);

-- 用户表
CREATE TABLE sys_user (
    id VARCHAR(32) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(50) NOT NULL,
    org_code VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar VARCHAR(500),
    department_id VARCHAR(32),
    status VARCHAR(20) DEFAULT 'active',
    last_login_at TIMESTAMP,
    employee_id VARCHAR(50),
    job_title VARCHAR(50),
    join_date VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    FOREIGN KEY (department_id) REFERENCES sys_department(id)
);

COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.full_name IS 'M84 机构人员名称 (DA/T 94)';
COMMENT ON COLUMN sys_user.org_code IS 'M85 组织机构代码 (DA/T 94)';
COMMENT ON COLUMN sys_user.status IS '状态: active/disabled/locked';

CREATE INDEX idx_user_username ON sys_user(username);
CREATE INDEX idx_user_department ON sys_user(department_id);
CREATE INDEX idx_user_employee ON sys_user(employee_id);

-- 用户-角色关联表
CREATE TABLE sys_user_role (
    user_id VARCHAR(32) NOT NULL,
    role_id VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
);

COMMENT ON TABLE sys_user_role IS '用户角色关联表';

-- 用户-部门关联表 (多部门支持)
CREATE TABLE sys_user_department (
    user_id VARCHAR(32) NOT NULL,
    department_id VARCHAR(32) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (user_id, department_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES sys_department(id) ON DELETE CASCADE
);

COMMENT ON TABLE sys_user_department IS '用户部门关联表';

-- ============================================================================
-- 3. 电子会计档案核心表
-- ============================================================================

-- 档案表
CREATE TABLE acc_archive (
    id VARCHAR(32) PRIMARY KEY,
    fonds_no VARCHAR(50) NOT NULL,
    archive_code VARCHAR(100) NOT NULL UNIQUE,
    category_code VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    fiscal_year VARCHAR(4) NOT NULL,
    fiscal_period VARCHAR(10),
    retention_period VARCHAR(10) NOT NULL,
    org_name VARCHAR(100) NOT NULL,
    creator VARCHAR(50),
    status VARCHAR(20) DEFAULT 'draft',
    standard_metadata JSONB,
    custom_metadata JSONB,
    security_level VARCHAR(20) DEFAULT 'internal',
    location VARCHAR(200),
    department_id VARCHAR(32),
    created_by VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    FOREIGN KEY (department_id) REFERENCES sys_department(id),
    FOREIGN KEY (created_by) REFERENCES sys_user(id)
);

COMMENT ON TABLE acc_archive IS '电子会计档案表';
COMMENT ON COLUMN acc_archive.fonds_no IS 'M9 全宗号';
COMMENT ON COLUMN acc_archive.archive_code IS 'M13 档号';
COMMENT ON COLUMN acc_archive.category_code IS 'M14 类别号';
COMMENT ON COLUMN acc_archive.title IS 'M22 题名';
COMMENT ON COLUMN acc_archive.fiscal_year IS 'M11 年度';
COMMENT ON COLUMN acc_archive.fiscal_period IS 'M41 会计月份/期间';
COMMENT ON COLUMN acc_archive.retention_period IS 'M12 保管期限';
COMMENT ON COLUMN acc_archive.org_name IS 'M6 立档单位名称';
COMMENT ON COLUMN acc_archive.creator IS 'M32 责任者/制单人';
COMMENT ON COLUMN acc_archive.standard_metadata IS 'DA/T 94标准元数据(JSON)';
COMMENT ON COLUMN acc_archive.custom_metadata IS '客户自定义元数据(JSON)';

CREATE INDEX idx_archive_fonds_year ON acc_archive(fonds_no, fiscal_year);
CREATE INDEX idx_archive_code ON acc_archive(archive_code);
CREATE INDEX idx_archive_category ON acc_archive(category_code);
CREATE INDEX idx_archive_status ON acc_archive(status);

-- 档案关联关系表
CREATE TABLE acc_archive_relation (
    id VARCHAR(32) PRIMARY KEY,
    source_id VARCHAR(32) NOT NULL,
    target_id VARCHAR(32) NOT NULL,
    relation_type VARCHAR(50) NOT NULL,
    relation_desc VARCHAR(255),
    created_by VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    FOREIGN KEY (source_id) REFERENCES acc_archive(id) ON DELETE CASCADE,
    FOREIGN KEY (target_id) REFERENCES acc_archive(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES sys_user(id)
);

COMMENT ON TABLE acc_archive_relation IS '档案关联关系表';
COMMENT ON COLUMN acc_archive_relation.relation_type IS 'M93 关系类型: voucher_source/red_dash/attachment/reference/replacement';
COMMENT ON COLUMN acc_archive_relation.relation_desc IS 'M95 关系描述';

CREATE INDEX idx_relation_source ON acc_archive_relation(source_id);
CREATE INDEX idx_relation_target ON acc_archive_relation(target_id);
CREATE INDEX idx_relation_type ON acc_archive_relation(relation_type);

-- ============================================================================
-- 4. 四性检测日志表
-- ============================================================================

CREATE TABLE audit_inspection_log (
    id VARCHAR(32) PRIMARY KEY,
    archive_id VARCHAR(32) NOT NULL,
    inspection_stage VARCHAR(20) NOT NULL,
    inspection_time TIMESTAMP NOT NULL,
    inspector_id VARCHAR(32),
    is_authentic BOOLEAN NOT NULL,
    is_complete BOOLEAN NOT NULL,
    is_available BOOLEAN NOT NULL,
    is_secure BOOLEAN NOT NULL,
    hash_snapshot VARCHAR(128),
    integrity_check JSONB,
    authenticity_check JSONB,
    availability_check JSONB,
    security_check JSONB,
    check_result VARCHAR(20) NOT NULL,
    detail_report JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (archive_id) REFERENCES acc_archive(id) ON DELETE CASCADE,
    FOREIGN KEY (inspector_id) REFERENCES sys_user(id)
);

COMMENT ON TABLE audit_inspection_log IS '四性检测日志表(合规证据)';
COMMENT ON COLUMN audit_inspection_log.inspection_stage IS '检测环节: receive/transfer/patrol/migration';
COMMENT ON COLUMN audit_inspection_log.is_authentic IS '真实性(验签)';
COMMENT ON COLUMN audit_inspection_log.is_complete IS '完整性(哈希)';
COMMENT ON COLUMN audit_inspection_log.is_available IS '可用性(格式)';
COMMENT ON COLUMN audit_inspection_log.is_secure IS '安全性(病毒)';

CREATE INDEX idx_inspection_archive ON audit_inspection_log(archive_id);
CREATE INDEX idx_inspection_stage ON audit_inspection_log(inspection_stage);
CREATE INDEX idx_inspection_time ON audit_inspection_log(inspection_time);

-- ============================================================================
-- 5. 安全审计日志表
-- ============================================================================

CREATE TABLE sys_audit_log (
    id VARCHAR(32) PRIMARY KEY,
    user_id VARCHAR(32),
    username VARCHAR(50),
    role_type VARCHAR(20),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50),
    resource_id VARCHAR(32),
    operation_result VARCHAR(20),
    risk_level VARCHAR(20),
    details TEXT,
    data_before TEXT,
    data_after TEXT,
    session_id VARCHAR(100),
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_audit_log IS '安全审计日志表';
COMMENT ON COLUMN sys_audit_log.role_type IS '操作人角色类型';
COMMENT ON COLUMN sys_audit_log.action IS '操作类型: login/create/update/delete/view/export/config';
COMMENT ON COLUMN sys_audit_log.operation_result IS '操作结果: success/fail/denied';
COMMENT ON COLUMN sys_audit_log.risk_level IS '风险等级: low/medium/high/critical';

CREATE INDEX idx_audit_time ON sys_audit_log(created_at);
CREATE INDEX idx_audit_user ON sys_audit_log(username);
CREATE INDEX idx_audit_role ON sys_audit_log(role_type);
CREATE INDEX idx_audit_risk ON sys_audit_log(risk_level);

-- ============================================================================
-- 6. 系统配置表
-- ============================================================================

CREATE TABLE sys_setting (
    key VARCHAR(100) PRIMARY KEY,
    value TEXT NOT NULL,
    description VARCHAR(500),
    "group" VARCHAR(50) DEFAULT 'general',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_setting IS '系统设置表';

-- ============================================================================
-- 7. 初始化数据
-- ============================================================================

-- 插入默认角色
INSERT INTO sys_role (id, name, code, role_category, is_exclusive, description, permissions, data_scope, type) VALUES
('role_system_admin', '系统管理员', 'SYSTEM_ADMIN', 'system_admin', TRUE, '负责系统运维和配置管理', '["manage_org","manage_users","manage_roles","manage_settings"]', 'all', 'system'),
('role_security_admin', '安全保密员', 'SECURITY_ADMIN', 'security_admin', TRUE, '负责权限管理和密钥管理', '["manage_roles","manage_users"]', 'all', 'system'),
('role_audit_admin', '安全审计员', 'AUDIT_ADMIN', 'audit_admin', TRUE, '负责查看和审计系统日志', '["audit_logs","view_dashboard"]', 'all', 'system'),
('role_archivist', '档案员', 'ARCHIVIST', 'business_user', FALSE, '负责档案管理和归档', '["view_archives","manage_archives","borrow_archives"]', 'dept', 'system');

-- 插入默认管理员用户 (密码: admin123)
INSERT INTO sys_user (id, username, password_hash, full_name, status) VALUES
('user_admin', 'admin', '$argon2id$v=19$m=65536,t=3,p=4$salt$hash', '系统管理员', 'active');

-- 分配系统管理员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES
('user_admin', 'role_system_admin');

COMMIT;
