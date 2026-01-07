-- Input: 扫描工作区和文件夹监控配置表
-- Output: scan_workspace, scan_folder_monitor 表创建脚本
-- Pos: Flyway 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
--
-- 扫描工作区表：存储临时扫描文件和 OCR 识别结果
CREATE TABLE IF NOT EXISTS scan_workspace (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,           -- 会话ID（用于移动端关联）
    user_id VARCHAR(64) NOT NULL,              -- 用户ID (匹配 sys_user.id 类型)
    file_name VARCHAR(255) NOT NULL,           -- 原始文件名
    file_path VARCHAR(500) NOT NULL,           -- 文件存储路径
    file_size BIGINT,                          -- 文件大小（字节）
    file_type VARCHAR(50),                     -- 文件类型（pdf, jpg, png等）
    upload_source VARCHAR(50) NOT NULL,        -- 上传来源（upload, monitor, mobile）

    -- OCR 相关字段
    ocr_status VARCHAR(50) NOT NULL DEFAULT 'pending',  -- pending, processing, review, completed, failed
    ocr_engine VARCHAR(50),                    -- 使用的OCR引擎（paddleocr, baidu, aliyun）
    ocr_result JSONB,                          -- OCR识别结果（结构化数据）
    overall_score INTEGER,                     -- 整体置信度分数

    -- 文档分类
    doc_type VARCHAR(50),                      -- 文档类型（invoice, contract, receipt等）

    -- 提交状态
    submit_status VARCHAR(50) DEFAULT 'draft', -- draft, submitted
    archive_id VARCHAR(64),                    -- 提交后关联的档案ID (匹配 acc_archive.id 类型)
    submitted_at TIMESTAMP,                    -- 提交时间

    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 索引
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_archive FOREIGN KEY (archive_id) REFERENCES acc_archive(id) ON DELETE SET NULL
);

CREATE INDEX idx_scan_workspace_session ON scan_workspace(session_id);
CREATE INDEX idx_scan_workspace_user ON scan_workspace(user_id);
CREATE INDEX idx_scan_workspace_archive ON scan_workspace(archive_id);
CREATE INDEX idx_scan_workspace_status ON scan_workspace(ocr_status, submit_status);
CREATE INDEX idx_scan_workspace_created ON scan_workspace(created_at DESC);

-- 文件监控配置表
CREATE TABLE IF NOT EXISTS scan_folder_monitor (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,             -- 用户ID (匹配 sys_user.id 类型)
    folder_path VARCHAR(500) NOT NULL,        -- 监控文件夹路径
    is_active BOOLEAN DEFAULT TRUE,           -- 是否启用
    file_filter VARCHAR(200) DEFAULT '*.pdf;*.jpg;*.jpeg;*.png',  -- 文件类型过滤
    auto_delete BOOLEAN DEFAULT FALSE,        -- 导入后是否删除源文件
    move_to_path VARCHAR(500),                -- 导入后移动到的路径（可选）

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_monitor_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

-- Indexes for scan_folder_monitor
CREATE INDEX idx_scan_folder_monitor_user ON scan_folder_monitor(user_id);
CREATE INDEX idx_scan_folder_monitor_active ON scan_folder_monitor(is_active);

-- Table and column comments
COMMENT ON TABLE scan_workspace IS '扫描工作区：存储临时扫描文件和OCR识别结果';
COMMENT ON COLUMN scan_workspace.id IS '主键ID';
COMMENT ON COLUMN scan_workspace.session_id IS '会话ID（用于移动端关联）';
COMMENT ON COLUMN scan_workspace.user_id IS '用户ID';
COMMENT ON COLUMN scan_workspace.file_name IS '原始文件名';
COMMENT ON COLUMN scan_workspace.file_path IS '文件存储路径';
COMMENT ON COLUMN scan_workspace.file_size IS '文件大小（字节）';
COMMENT ON COLUMN scan_workspace.file_type IS '文件类型（pdf, jpg, png等）';
COMMENT ON COLUMN scan_workspace.upload_source IS '上传来源（upload, monitor, mobile）';
COMMENT ON COLUMN scan_workspace.ocr_status IS 'OCR状态（pending, processing, review, completed, failed）';
COMMENT ON COLUMN scan_workspace.ocr_engine IS '使用的OCR引擎（paddleocr, baidu, aliyun）';
COMMENT ON COLUMN scan_workspace.ocr_result IS 'OCR识别结果（结构化数据）';
COMMENT ON COLUMN scan_workspace.overall_score IS '整体置信度分数';
COMMENT ON COLUMN scan_workspace.doc_type IS '文档类型（invoice, contract, receipt等）';
COMMENT ON COLUMN scan_workspace.submit_status IS '提交状态（draft, submitted）';
COMMENT ON COLUMN scan_workspace.archive_id IS '提交后关联的档案ID';
COMMENT ON COLUMN scan_workspace.submitted_at IS '提交时间';
COMMENT ON COLUMN scan_workspace.created_at IS '创建时间';
COMMENT ON COLUMN scan_workspace.updated_at IS '更新时间';

COMMENT ON TABLE scan_folder_monitor IS '文件监控配置表';
COMMENT ON COLUMN scan_folder_monitor.id IS '主键ID';
COMMENT ON COLUMN scan_folder_monitor.user_id IS '用户ID';
COMMENT ON COLUMN scan_folder_monitor.folder_path IS '监控文件夹路径';
COMMENT ON COLUMN scan_folder_monitor.is_active IS '是否启用';
COMMENT ON COLUMN scan_folder_monitor.file_filter IS '文件类型过滤';
COMMENT ON COLUMN scan_folder_monitor.auto_delete IS '导入后是否删除源文件';
COMMENT ON COLUMN scan_folder_monitor.move_to_path IS '导入后移动到的路径（可选）';
COMMENT ON COLUMN scan_folder_monitor.created_at IS '创建时间';
COMMENT ON COLUMN scan_folder_monitor.updated_at IS '更新时间';
