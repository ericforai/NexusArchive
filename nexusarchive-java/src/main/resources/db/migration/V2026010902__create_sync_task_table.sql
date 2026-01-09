-- Input: Flyway SQL、PostgreSQL DDL
-- Output: sys_sync_task 表
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 创建异步同步任务状态表
-- 用于持久化 ERP 同步任务的执行状态，解决内存存储重启丢失的问题
CREATE TABLE IF NOT EXISTS sys_sync_task (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(100) NOT NULL UNIQUE,
    scenario_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    total_count INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    fail_count INTEGER DEFAULT 0,
    error_message TEXT,
    progress DECIMAL(5,4) DEFAULT 0.0,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    operator_id VARCHAR(50),
    client_ip VARCHAR(50),
    sync_params TEXT,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_sync_task_task_id ON sys_sync_task(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_task_scenario_id ON sys_sync_task(scenario_id);
CREATE INDEX IF NOT EXISTS idx_sync_task_status ON sys_sync_task(status);
CREATE INDEX IF NOT EXISTS idx_sync_task_created_time ON sys_sync_task(created_time DESC);

-- 添加注释
COMMENT ON TABLE sys_sync_task IS '异步同步任务状态表，用于持久化 ERP 同步任务的执行状态';
COMMENT ON COLUMN sys_sync_task.task_id IS '任务唯一标识，格式: sync-{scenarioId}-{timestamp}';
COMMENT ON COLUMN sys_sync_task.scenario_id IS '关联 sys_erp_scenario.id';
COMMENT ON COLUMN sys_sync_task.status IS '任务状态: SUBMITTED=已提交, RUNNING=运行中, SUCCESS=成功, FAIL=失败';
COMMENT ON COLUMN sys_sync_task.total_count IS '总记录数';
COMMENT ON COLUMN sys_sync_task.success_count IS '成功数';
COMMENT ON COLUMN sys_sync_task.fail_count IS '失败数';
COMMENT ON COLUMN sys_sync_task.error_message IS '错误信息';
COMMENT ON COLUMN sys_sync_task.progress IS '进度 (0.0 - 1.0)';
COMMENT ON COLUMN sys_sync_task.start_time IS '开始时间';
COMMENT ON COLUMN sys_sync_task.end_time IS '结束时间';
COMMENT ON COLUMN sys_sync_task.operator_id IS '操作人 ID';
COMMENT ON COLUMN sys_sync_task.client_ip IS '操作客户端 IP';
COMMENT ON COLUMN sys_sync_task.sync_params IS '同步参数 (JSON)';
