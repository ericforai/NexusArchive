-- Input: SQL DDL
-- Output: V2026012401__create_sales_order_tables.sql
-- Pos: Flyway 数据库迁移脚本

-- 销售订单主表
CREATE TABLE IF NOT EXISTS sd_sales_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    -- YonSuite 字段
    order_id VARCHAR(64) NOT NULL COMMENT 'YonSuite 订单ID',
    order_code VARCHAR(64) NOT NULL COMMENT '单据编号',
    agent_id VARCHAR(64) COMMENT '客户ID',
    agent_name VARCHAR(255) COMMENT '客户名称',
    vouchdate DATE COMMENT '单据日期',
    order_date DATE COMMENT '订单日期',
    next_status_name VARCHAR(32) COMMENT '订单状态',

    -- 金额字段
    total_money DECIMAL(18,2) COMMENT '总金额',
    promotion_money DECIMAL(18,2) COMMENT '总优惠金额',
    rebate_money DECIMAL(18,2) COMMENT '折扣返利金额',
    pay_money DECIMAL(18,2) COMMENT '含税金额',
    real_money DECIMAL(18,2) COMMENT '应收金额',

    -- 外键关联
    sales_out_id VARCHAR(64) COMMENT '关联销售出库单ID',
    voucher_id VARCHAR(64) COMMENT '关联记账凭证ID',

    -- 业务单号（YonSuite编码）
    yon_order_code VARCHAR(64) COMMENT 'YonSuite 订单编码',
    yon_sales_out_code VARCHAR(64) COMMENT 'YonSuite 出库单编码',
    yon_voucher_code VARCHAR(64) COMMENT 'YonSuite 凭证编码',

    -- 多字段匹配用
    agent_id_erp VARCHAR(64) COMMENT '客户ERP编码',
    sales_org_id VARCHAR(64) COMMENT '销售组织ID',

    -- 系统字段
    fonds_code VARCHAR(32) COMMENT '全宗代码',
    source_system VARCHAR(32) DEFAULT 'YonSuite' COMMENT '来源系统',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    pubts VARCHAR(32) COMMENT 'YonSuite 时间戳',

    CONSTRAINT uk_order_id UNIQUE (order_id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_sd_sales_order_fonds_code ON sd_sales_order(fonds_code);
CREATE INDEX IF NOT EXISTS idx_sd_sales_order_agent_id ON sd_sales_order(agent_id);
CREATE INDEX IF NOT EXISTS idx_sd_sales_order_vouchdate ON sd_sales_order(vouchdate);
CREATE INDEX IF NOT EXISTS idx_sd_sales_order_sales_out_id ON sd_sales_order(sales_out_id);
CREATE INDEX IF NOT EXISTS idx_sd_sales_order_voucher_id ON sd_sales_order(voucher_id);

-- 订单明细表
CREATE TABLE IF NOT EXISTS sd_sales_order_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '关联主表ID',
    line_no INT COMMENT '行号',

    -- 产品信息
    product_id VARCHAR(64) COMMENT '产品ID',
    product_code VARCHAR(64) COMMENT '产品编码',
    product_name VARCHAR(255) COMMENT '产品名称',
    sku_id VARCHAR(64) COMMENT 'SKU ID',
    sku_code VARCHAR(64) COMMENT 'SKU编码',
    sku_name VARCHAR(255) COMMENT 'SKU名称',

    -- 数量金额
    qty DECIMAL(18,4) COMMENT '数量',
    ori_unit_price DECIMAL(18,4) COMMENT '原币单价',
    ori_money DECIMAL(18,2) COMMENT '原币金额',
    ori_tax DECIMAL(18,2) COMMENT '原币税额',
    nat_unit_price DECIMAL(18,4) COMMENT '本币单价',
    nat_money DECIMAL(18,2) COMMENT '本币金额',
    nat_tax DECIMAL(18,2) COMMENT '本币税额',

    -- 税务
    tax_rate VARCHAR(32) COMMENT '税率',
    tax_id VARCHAR(64) COMMENT '税目ID',

    -- 交付信息
    stock_id VARCHAR(64) COMMENT '仓库ID',
    stock_name VARCHAR(255) COMMENT '仓库名称',
    hope_receive_date DATE COMMENT '预计收货日期',

    -- 系统字段
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    CONSTRAINT fk_sd_sales_order_detail_order FOREIGN KEY (order_id) REFERENCES sd_sales_order(id) ON DELETE CASCADE
);

-- 创建明细表索引
CREATE INDEX IF NOT EXISTS idx_sd_sales_order_detail_order_id ON sd_sales_order_detail(order_id);

-- 添加表注释
COMMENT ON TABLE sd_sales_order IS '销售订单主表';
COMMENT ON TABLE sd_sales_order_detail IS '销售订单明细表';
