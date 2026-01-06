-- Input: Performance Metrics Tables Creation
-- Output: Schema change for performance metrics monitoring
-- Pos: db/migration/V78
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 1. 系统性能指标表
CREATE TABLE IF NOT EXISTS system_performance_metrics (
    id VARCHAR(32) PRIMARY KEY,
    metric_type VARCHAR(50) NOT NULL,  -- FONDS_CAPACITY, CONCURRENT_SEARCH, FILE_SIZE, PREVIEW_TIME, LOG_RETENTION
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(18, 2),
    metric_unit VARCHAR(20),  -- GB, MB, ms, count, days
    fonds_no VARCHAR(50),  -- 全宗号（如果是全宗相关指标）
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE system_performance_metrics IS '系统性能指标表';
COMMENT ON COLUMN system_performance_metrics.metric_type IS '指标类型: FONDS_CAPACITY(单全宗容量), CONCURRENT_SEARCH(并发检索), FILE_SIZE(最大文件大小), PREVIEW_TIME(预览首屏时间), LOG_RETENTION(日志留存周期)';

-- 2. 检索性能统计表
CREATE TABLE IF NOT EXISTS search_performance_stats (
    id VARCHAR(32) PRIMARY KEY,
    fonds_no VARCHAR(50),
    search_type VARCHAR(50) NOT NULL,  -- KEYWORD, ADVANCED, AMOUNT_RANGE, SUMMARY
    search_duration_ms INT,  -- 检索耗时（毫秒）
    result_count INT,  -- 结果数量
    user_id VARCHAR(32),
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE search_performance_stats IS '检索性能统计表';

-- 3. 文件存储容量统计表
CREATE TABLE IF NOT EXISTS storage_capacity_stats (
    id VARCHAR(32) PRIMARY KEY,
    fonds_no VARCHAR(50) NOT NULL,
    total_size_gb DECIMAL(18, 2) NOT NULL,  -- 总容量（GB）
    used_size_gb DECIMAL(18, 2) NOT NULL,  -- 已使用容量（GB）
    file_count BIGINT NOT NULL,  -- 文件数量
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE storage_capacity_stats IS '文件存储容量统计表';

-- 4. 创建索引
CREATE INDEX IF NOT EXISTS idx_performance_metrics_type 
    ON system_performance_metrics(metric_type, recorded_at);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_fonds 
    ON system_performance_metrics(fonds_no, recorded_at);
CREATE INDEX IF NOT EXISTS idx_search_performance_fonds 
    ON search_performance_stats(fonds_no, recorded_at);
CREATE INDEX IF NOT EXISTS idx_search_performance_type 
    ON search_performance_stats(search_type, recorded_at);
CREATE INDEX IF NOT EXISTS idx_storage_capacity_fonds 
    ON storage_capacity_stats(fonds_no, recorded_at);





