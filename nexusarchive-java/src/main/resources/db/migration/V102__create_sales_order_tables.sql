-- Input: SQL DDL
-- Output: V102__create_sales_order_tables.sql
-- Pos: Flyway 数据库迁移脚本

-- 销售订单主表
CREATE TABLE IF NOT EXISTS sd_sales_order (
    id BIGSERIAL PRIMARY KEY,

    -- YonSuite 字段
    order_id VARCHAR(64) NOT NULL,
    order_code VARCHAR(64) NOT NULL,
    agent_id VARCHAR(64),
    agent_name VARCHAR(255),
    vouchdate DATE,
    order_date DATE,
    next_status_name VARCHAR(32),

    -- 金额字段
    total_money DECIMAL(18,2),
    promotion_money DECIMAL(18,2),
    rebate_money DECIMAL(18,2),
    pay_money DECIMAL(18,2),
    real_money DECIMAL(18,2),
    order_pay_money DECIMAL(18,2),
    order_real_money DECIMAL(18,2),

    -- 外键关联
    sales_out_id VARCHAR(64),
    voucher_id VARCHAR(64),

    -- 业务单号（YonSuite编码）
    yon_order_code VARCHAR(64),
    yon_sales_out_code VARCHAR(64),
    yon_voucher_code VARCHAR(64),

    -- 多字段匹配用
    agent_id_erp VARCHAR(64),
    sales_org_id VARCHAR(64),

    -- 系统字段
    fonds_code VARCHAR(32),
    source_system VARCHAR(32) DEFAULT 'YonSuite',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    pubts VARCHAR(32),

    CONSTRAINT uk_order_id UNIQUE (order_id)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_sd_sales_order_fonds_code ON sd_sales_order(fonds_code);
CREATE INDEX IF NOT EXISTS idx_sd_sales_order_agent_id ON sd_sales_order(agent_id);
CREATE INDEX IF NOT EXISTS idx_sd_sales_order_vouchdate ON sd_sales_order(vouchdate);
CREATE INDEX IF NOT EXISTS idx_sd_sales_order_sales_out_id ON sd_sales_order(sales_out_id);
CREATE INDEX IF NOT EXISTS idx_sd_sales_order_voucher_id ON sd_sales_order(voucher_id);

-- 订单明细表
CREATE TABLE IF NOT EXISTS sd_sales_order_detail (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    line_no INT,

    -- 产品信息
    product_id VARCHAR(64),
    product_code VARCHAR(64),
    product_name VARCHAR(255),
    sku_id VARCHAR(64),
    sku_code VARCHAR(64),
    sku_name VARCHAR(255),

    -- 数量金额
    qty DECIMAL(18,4),
    ori_unit_price DECIMAL(18,4),
    ori_money DECIMAL(18,2),
    ori_tax DECIMAL(18,2),
    nat_unit_price DECIMAL(18,4),
    nat_money DECIMAL(18,2),
    nat_tax DECIMAL(18,2),

    -- 税务
    tax_rate VARCHAR(32),
    tax_id VARCHAR(64),

    -- 交付信息
    stock_id VARCHAR(64),
    stock_name VARCHAR(255),
    hope_receive_date DATE,

    -- 系统字段
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sales_order_detail_order FOREIGN KEY (order_id)
        REFERENCES sd_sales_order(id) ON DELETE CASCADE
);

-- 明细表索引
CREATE INDEX IF NOT EXISTS idx_sd_sales_order_detail_order_id ON sd_sales_order_detail(order_id);

-- 表注释
COMMENT ON TABLE sd_sales_order IS '销售订单主表';
COMMENT ON TABLE sd_sales_order_detail IS '销售订单明细表';
