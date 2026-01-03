-- Input: User Lifecycle Tables Creation
-- Output: Schema change for user lifecycle management
-- Pos: db/migration/V77
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 1. 员工生命周期事件表
CREATE TABLE IF NOT EXISTS employee_lifecycle_event (
    id VARCHAR(32) PRIMARY KEY,
    employee_id VARCHAR(32) NOT NULL,
    employee_name VARCHAR(100),
    event_type VARCHAR(20) NOT NULL,  -- ONBOARD(入职), OFFBOARD(离职), TRANSFER(调岗)
    event_date DATE NOT NULL,
    previous_dept_id VARCHAR(32),
    new_dept_id VARCHAR(32),
    previous_role_ids TEXT,  -- JSON格式的角色ID列表
    new_role_ids TEXT,  -- JSON格式的角色ID列表
    reason TEXT,
    processed BOOLEAN DEFAULT FALSE,  -- 是否已处理
    processed_at TIMESTAMP,
    processed_by VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE employee_lifecycle_event IS '员工生命周期事件表';
COMMENT ON COLUMN employee_lifecycle_event.event_type IS '事件类型: ONBOARD(入职), OFFBOARD(离职), TRANSFER(调岗)';
COMMENT ON COLUMN employee_lifecycle_event.processed IS '是否已处理：false-待处理，true-已处理';

-- 2. 访问权限复核记录表
CREATE TABLE IF NOT EXISTS access_review (
    id VARCHAR(32) PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL,
    review_type VARCHAR(20) NOT NULL,  -- PERIODIC(定期), AD_HOC(临时), ON_DEMAND(按需)
    review_date DATE NOT NULL,
    reviewer_id VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, APPROVED, REJECTED
    current_roles TEXT,  -- JSON格式的当前角色列表
    current_permissions TEXT,  -- JSON格式的当前权限列表
    review_result TEXT,  -- 复核结果说明
    action_taken TEXT,  -- 采取的行动（如权限回收、角色调整等）
    next_review_date DATE,  -- 下次复核日期
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE access_review IS '访问权限复核记录表';
COMMENT ON COLUMN access_review.review_type IS '复核类型: PERIODIC(定期), AD_HOC(临时), ON_DEMAND(按需)';
COMMENT ON COLUMN access_review.status IS '状态: PENDING(待复核), APPROVED(已批准), REJECTED(已拒绝)';

-- 3. MFA 配置表
CREATE TABLE IF NOT EXISTS user_mfa_config (
    id VARCHAR(32) PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL UNIQUE,
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_type VARCHAR(20),  -- TOTP, SMS, EMAIL
    secret_key VARCHAR(255),  -- TOTP密钥（加密存储）
    backup_codes TEXT,  -- 备用码（JSON格式，加密存储）
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

COMMENT ON TABLE user_mfa_config IS '用户MFA配置表';
COMMENT ON COLUMN user_mfa_config.mfa_type IS 'MFA类型: TOTP(时间同步令牌), SMS(短信), EMAIL(邮件)';
COMMENT ON COLUMN user_mfa_config.secret_key IS 'TOTP密钥（加密存储）';

-- 4. 文件存储策略配置表
CREATE TABLE IF NOT EXISTS file_storage_policy (
    id VARCHAR(32) PRIMARY KEY,
    fonds_no VARCHAR(50) NOT NULL,
    policy_type VARCHAR(20) NOT NULL,  -- IMMUTABLE(不可变), RETENTION(保留策略)
    retention_days INT,  -- 保留天数（NULL表示永久保留）
    immutable_until DATE,  -- 不可变截止日期
    enabled BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE file_storage_policy IS '文件存储策略配置表';
COMMENT ON COLUMN file_storage_policy.policy_type IS '策略类型: IMMUTABLE(不可变), RETENTION(保留策略)';

-- 5. 文件哈希去重范围配置表
CREATE TABLE IF NOT EXISTS file_hash_dedup_scope (
    id VARCHAR(32) PRIMARY KEY,
    fonds_no VARCHAR(50) NOT NULL,
    scope_type VARCHAR(20) NOT NULL,  -- SAME_FONDS(同全宗), AUTHORIZED(授权范围), GLOBAL(全局)
    enabled BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE file_hash_dedup_scope IS '文件哈希去重范围配置表';
COMMENT ON COLUMN file_hash_dedup_scope.scope_type IS '去重范围: SAME_FONDS(同全宗), AUTHORIZED(授权范围), GLOBAL(全局)';

-- 6. 创建索引
CREATE INDEX IF NOT EXISTS idx_employee_lifecycle_event_employee 
    ON employee_lifecycle_event(employee_id, processed, deleted);
CREATE INDEX IF NOT EXISTS idx_employee_lifecycle_event_type 
    ON employee_lifecycle_event(event_type, event_date, deleted);
CREATE INDEX IF NOT EXISTS idx_access_review_user 
    ON access_review(user_id, status, deleted);
CREATE INDEX IF NOT EXISTS idx_access_review_date 
    ON access_review(review_date, status, deleted);
CREATE INDEX IF NOT EXISTS idx_file_storage_policy_fonds 
    ON file_storage_policy(fonds_no, enabled, deleted);
CREATE INDEX IF NOT EXISTS idx_file_hash_dedup_scope_fonds 
    ON file_hash_dedup_scope(fonds_no, enabled, deleted);


