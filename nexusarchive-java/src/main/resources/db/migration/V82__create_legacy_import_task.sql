-- Input: 数据库引擎
-- Output: legacy_import_task 表创建脚本
-- Pos: Flyway 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- ----------------------------
-- 历史数据导入任务表 (Legacy Import Task)
-- ----------------------------
-- 说明: 记录历史数据导入任务的详细信息，用于审计和追溯
-- OpenSpec 来源: openspec-legacy-data-import.md

CREATE TABLE IF NOT EXISTS legacy_import_task (
    id VARCHAR(32) PRIMARY KEY,
    operator_id VARCHAR(32) NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(100) COMMENT '操作人姓名',
    fonds_no VARCHAR(50) NOT NULL COMMENT '全宗号',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_hash VARCHAR(64) COMMENT '文件哈希值',
    total_rows INT NOT NULL COMMENT '总行数',
    success_rows INT NOT NULL DEFAULT 0 COMMENT '成功行数',
    failed_rows INT NOT NULL DEFAULT 0 COMMENT '失败行数',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING(待处理), PROCESSING(处理中), SUCCESS(成功), FAILED(失败), PARTIAL_SUCCESS(部分成功)',
    error_report_path VARCHAR(500) COMMENT '错误报告文件路径',
    created_fonds_nos TEXT COMMENT '自动创建的全宗号列表（JSON 数组）',
    created_entity_ids TEXT COMMENT '自动创建的实体ID列表（JSON 数组）',
    started_at TIMESTAMP COMMENT '开始时间',
    completed_at TIMESTAMP COMMENT '完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_import_task_operator ON legacy_import_task(operator_id, created_at);
CREATE INDEX IF NOT EXISTS idx_import_task_fonds ON legacy_import_task(fonds_no, created_at);
CREATE INDEX IF NOT EXISTS idx_import_task_status ON legacy_import_task(status, created_at);

-- 表注释
COMMENT ON TABLE legacy_import_task IS '历史数据导入任务表';
COMMENT ON COLUMN legacy_import_task.status IS '状态: PENDING(待处理), PROCESSING(处理中), SUCCESS(成功), FAILED(失败), PARTIAL_SUCCESS(部分成功)';
COMMENT ON COLUMN legacy_import_task.created_fonds_nos IS '自动创建的全宗号列表（JSON 数组格式）';
COMMENT ON COLUMN legacy_import_task.created_entity_ids IS '自动创建的实体ID列表（JSON 数组格式）';

