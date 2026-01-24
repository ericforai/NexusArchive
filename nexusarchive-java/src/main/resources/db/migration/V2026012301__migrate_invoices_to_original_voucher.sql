-- ============================================================
-- V2026012301: 发票数据迁移 - 从 acc_archive 迁移到 arc_original_voucher
-- ============================================================
-- Input: 发票种子数据 (INV-*)
-- Output: 正确存储在 arc_original_voucher 表的原始凭证
-- Pos: 数据库迁移脚本

-- 问题背景：
-- 发票 (INV-*) 属于「原始凭证」，应存储在 arc_original_voucher 表
-- 而非 acc_archive 表的 AC01（会计凭证/记账凭证）类别

-- Step 1: 插入发票数据到 arc_original_voucher 表
INSERT INTO arc_original_voucher (
    id,
    voucher_no,
    archival_category,
    voucher_type,
    business_date,
    amount,
    currency,
    counterparty,
    summary,
    creator,
    source_system,
    source_doc_id,
    fonds_code,
    fiscal_year,
    retention_period,
    archive_status,
    archived_time,
    version,
    is_latest,
    created_by,
    created_time,
    deleted,
    pool_status
)
SELECT
    id,                                     -- 保持原ID
    archive_code AS voucher_no,             -- 档号 -> 凭证号
    'VOUCHER' AS archival_category,         -- 原始凭证门类
    'INV_PAPER' AS voucher_type,            -- 发票类型
    doc_date AS business_date,              -- 业务日期
    amount,                                 -- 金额
    'CNY' AS currency,                      -- 币种
    org_name AS counterparty,               -- 对方单位
    title AS summary,                       -- 题名 -> 摘要
    creator,                                -- 制单人
    'SEED_DATA' AS source_system,           -- 来源系统
    unique_biz_id AS source_doc_id,         -- 唯一业务ID
    fonds_no AS fonds_code,                 -- 全宗号
    fiscal_year,                            -- 会计年度
    retention_period,                       -- 保管期限
    'ARCHIVED' AS archive_status,           -- 归档状态
    CURRENT_TIMESTAMP AS archived_time,     -- 归档时间
    1 AS version,                           -- 版本号
    true AS is_latest,                      -- 是否最新版本
    created_by,                             -- 创建人
    created_time,                           -- 创建时间
    0 AS deleted,                           -- 未删除
    'ARCHIVED' AS pool_status              -- 池状态（需匹配前端 poolStatus 过滤值）
FROM acc_archive
WHERE archive_code LIKE 'INV-%'
ON CONFLICT (fonds_code, fiscal_year, voucher_no) DO NOTHING;

-- Step 2: 更新 arc_file_content 中发票文件的关联
-- 将 item_id (原 acc_archive.id) 更新为指向新的 original_voucher
-- 并更新 voucher_type 为正确的类型
UPDATE arc_file_content
SET voucher_type = 'INV_PAPER',
    pre_archive_status = 'ARCHIVED'
WHERE archival_code LIKE 'INV-%';

-- Step 3: 从 acc_archive 删除发票数据
DELETE FROM acc_archive
WHERE archive_code LIKE 'INV-%';

-- 添加注释说明
COMMENT ON TABLE arc_original_voucher IS '原始凭证表：存储发票、收据、银行回单等原始单据';
