-- YonSuite Sales Outbound Data Storage
-- V35: Add tables for storing synced sales outbound data from YonSuite

-- 销售出库单主表
CREATE TABLE IF NOT EXISTS ys_sales_out (
    id BIGSERIAL PRIMARY KEY,
    ys_id VARCHAR(50) UNIQUE NOT NULL,      -- YonSuite 原始ID
    code VARCHAR(50),                        -- 单据编号
    vouchdate DATE,                          -- 单据日期
    status VARCHAR(20),                      -- 状态
    cust_name VARCHAR(200),                  -- 客户名称
    warehouse_name VARCHAR(200),             -- 仓库名称
    total_quantity DECIMAL(18,4),           -- 总数量
    memo TEXT,                              -- 备注
    raw_json TEXT,                          -- 原始JSON响应
    sync_time TIMESTAMP DEFAULT NOW(),      -- 同步时间
    created_time TIMESTAMP DEFAULT NOW()    -- 创建时间
);

-- 销售出库单明细表
CREATE TABLE IF NOT EXISTS ys_sales_out_detail (
    id BIGSERIAL PRIMARY KEY,
    sales_out_id BIGINT REFERENCES ys_sales_out(id) ON DELETE CASCADE,
    ys_detail_id VARCHAR(50),               -- YonSuite明细ID
    rowno INT,                              -- 行号
    product_code VARCHAR(50),               -- 产品编码
    product_name VARCHAR(200),              -- 产品名称
    qty DECIMAL(18,4),                      -- 数量
    unit_name VARCHAR(50),                  -- 单位
    ori_money DECIMAL(18,2),                -- 原币金额
    ori_tax DECIMAL(18,2)                   -- 原币税额
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_ys_sales_out_code ON ys_sales_out(code);
CREATE INDEX IF NOT EXISTS idx_ys_sales_out_vouchdate ON ys_sales_out(vouchdate);
CREATE INDEX IF NOT EXISTS idx_ys_sales_out_detail_sales_out_id ON ys_sales_out_detail(sales_out_id);

COMMENT ON TABLE ys_sales_out IS 'YonSuite销售出库单同步数据';
COMMENT ON TABLE ys_sales_out_detail IS 'YonSuite销售出库单明细同步数据';
