-- 财务报告演示数据
-- 用于测试财务报告详情抽屉组件
-- 生成时间: 2026-01-19

-- 确保使用正确的全宗和数据库上下文
SET search_path TO public;

-- 插入财务报告数据
INSERT INTO acc_archive (
    id,
    fonds_no,
    unique_biz_id,
    archive_code,
    category_code,
    title,
    amount,
    fiscal_year,
    fiscal_period,
    doc_date,
    custom_metadata,
    created_time,
    creator,
    summary,
    org_name,
    retention_period,
    security_level,
    status,
    archived_at,
    deleted
) VALUES
-- 月度财务报告
(
    'FR-M-2025-001', 'DEMO', 'DEMO-FR-MONTHLY-001', 'DEMO-FR-2025-001', 'AC03',
    '2025年1月月度财务报告', 2500000.00, '2025', '01', '2025-01-31',
    '{"reportType": "MONTHLY", "period": "2025-01", "reportNo": "FR-2025-01-001", "unit": "财务部", "revenue": 2500000.00, "expenses": 1800000.00, "profit": 700000.00}'::jsonb,
    '2025-01-31 18:00:00', '财务部',
    '本月实现营业收入250万元，同比下降3%，环比增长8%。主要受季节性因素影响，传统业务收入略有下降，但新业务板块增长显著。',
    '示例公司', '永久', 'internal', 'archived', '2025-01-31 18:00:00', 0
),
(
    'FR-M-2025-002', 'DEMO', 'DEMO-FR-MONTHLY-002', 'DEMO-FR-2025-002', 'AC03',
    '2025年2月月度财务报告', 2800000.00, '2025', '02', '2025-02-28',
    '{"reportType": "MONTHLY", "period": "2025-02", "reportNo": "FR-2025-02-001", "unit": "财务部", "revenue": 2800000.00, "expenses": 2000000.00, "profit": 800000.00}'::jsonb,
    '2025-02-28 18:00:00', '财务部',
    '本月实现营业收入280万元，同比增长12%，环比增长12%。春节假期影响下，业务仍保持稳定增长态势。',
    '示例公司', '永久', 'internal', 'archived', '2025-02-28 18:00:00', 0
),
(
    'FR-M-2025-003', 'DEMO', 'DEMO-FR-MONTHLY-003', 'DEMO-FR-2025-003', 'AC03',
    '2025年3月月度财务报告', 3200000.00, '2025', '03', '2025-03-31',
    '{"reportType": "MONTHLY", "period": "2025-03", "reportNo": "FR-2025-03-001", "unit": "财务部", "revenue": 3200000.00, "expenses": 2300000.00, "profit": 900000.00}'::jsonb,
    '2025-03-31 18:00:00', '财务部',
    '一季度收官，本月实现营业收入320万元，创季度新高。主要得益于新产品线的顺利推出和市场拓展效果显现。',
    '示例公司', '永久', 'internal', 'archived', '2025-03-31 18:00:00', 0
),
(
    'FR-M-2025-004', 'DEMO', 'DEMO-FR-MONTHLY-004', 'DEMO-FR-2025-004', 'AC03',
    '2025年4月月度财务报告', 2900000.00, '2025', '04', '2025-04-30',
    '{"reportType": "MONTHLY", "period": "2025-04", "reportNo": "FR-2025-04-001", "unit": "财务部", "revenue": 2900000.00, "expenses": 2100000.00, "profit": 800000.00}'::jsonb,
    '2025-04-30 18:00:00', '财务部',
    '本月实现营业收入290万元，同比增长8%，环比下降9%。受季节性因素影响，业务增长有所放缓。',
    '示例公司', '永久', 'internal', 'archived', '2025-04-30 18:00:00', 0
),
(
    'FR-M-2025-005', 'DEMO', 'DEMO-FR-MONTHLY-005', 'DEMO-FR-2025-005', 'AC03',
    '2025年5月月度财务报告', 3100000.00, '2025', '05', '2025-05-31',
    '{"reportType": "MONTHLY", "period": "2025-05", "reportNo": "FR-2025-05-001", "unit": "财务部", "revenue": 3100000.00, "expenses": 2200000.00, "profit": 900000.00}'::jsonb,
    '2025-05-31 18:00:00', '财务部',
    '本月实现营业收入310万元，同比增长10%，环比增长7%。业务呈现复苏态势，市场活跃度逐步提升。',
    '示例公司', '永久', 'internal', 'archived', '2025-05-31 18:00:00', 0
),
-- 季度财务报告
(
    'FR-Q-2025-001', 'DEMO', 'DEMO-FR-QUARTERLY-001', 'DEMO-FR-2025-Q1', 'AC03',
    '2025年第一季度财务报告', 8500000.00, '2025', 'Q1', '2025-03-31',
    '{"reportType": "QUARTERLY", "period": "Q1", "reportNo": "FR-2025-Q1-001", "unit": "财务部", "quarterlyRevenue": 8500000.00, "quarterlyExpenses": 6100000.00, "quarterlyProfit": 2400000.00, "yoyGrowth": "15.2%"}'::jsonb,
    '2025-03-31 18:00:00', '财务部',
    '2025年第一季度实现营业收入850万元，同比增长15.2%，净利润240万元。整体经营情况良好，各项指标均符合预期。',
    '示例公司', '永久', 'internal', 'archived', '2025-03-31 18:00:00', 0
),
(
    'FR-Q-2025-002', 'DEMO', 'DEMO-FR-QUARTERLY-002', 'DEMO-FR-2025-Q2', 'AC03',
    '2025年第二季度财务报告', 9200000.00, '2025', 'Q2', '2025-06-30',
    '{"reportType": "QUARTERLY", "period": "Q2", "reportNo": "FR-2025-Q2-001", "unit": "财务部", "quarterlyRevenue": 9200000.00, "quarterlyExpenses": 6500000.00, "quarterlyProfit": 2700000.00, "yoyGrowth": "18.5%"}'::jsonb,
    '2025-06-30 18:00:00', '财务部',
    '2025年第二季度实现营业收入920万元，同比增长18.5%，净利润270万元。得益于新产品上市和市场份额提升。',
    '示例公司', '永久', 'internal', 'archived', '2025-06-30 18:00:00', 0
),
-- 半年度财务报告
(
    'FR-S-2025-001', 'DEMO', 'DEMO-FR-SEMI-001', 'DEMO-FR-2025-S1', 'AC03',
    '2025年半年度财务报告', 17700000.00, '2025', 'S1', '2025-06-30',
    '{"reportType": "SEMI_ANNUAL", "period": "S1", "reportNo": "FR-2025-S1-001", "unit": "财务部", "semiAnnualRevenue": 17700000.00, "semiAnnualExpenses": 12600000.00, "semiAnnualProfit": 5100000.00, "yoyGrowth": "16.8%"}'::jsonb,
    '2025-06-30 18:00:00', '财务部',
    '2025年上半年实现营业收入1770万元，同比增长16.8%，净利润510万元。公司业务规模持续扩大，市场竞争力稳步提升。',
    '示例公司', '永久', 'internal', 'archived', '2025-06-30 18:00:00', 0
),
-- 年度财务报告
(
    'FR-A-2024-001', 'DEMO', 'DEMO-FR-ANNUAL-001', 'DEMO-FR-2024-ANNUAL', 'AC03',
    '2024年度财务报告', 35000000.00, '2024', '2024', '2024-12-31',
    '{"reportType": "ANNUAL", "period": "2024", "reportNo": "FR-2024-001", "unit": "财务部", "annualRevenue": 35000000.00, "annualExpenses": 26000000.00, "annualProfit": 9000000.00, "totalAssets": 50000000.00, "totalLiabilities": 20000000.00, "shareholdersEquity": 30000000.00}'::jsonb,
    '2024-12-31 18:00:00', '财务部',
    '2024年度公司实现营业收入3500万元，同比增长22%；净利润900万元，同比增长28%。截至年末，总资产5000万元，负债2000万元，股东权益3000万元，资产负债率40%，财务结构稳健。',
    '示例公司', '永久', 'internal', 'archived', '2024-12-31 18:00:00', 0
),
-- 专项财务报告
(
    'FR-SP-2025-001', 'DEMO', 'DEMO-FR-SPECIAL-001', 'DEMO-FR-2025-AUDIT', 'AC03',
    '2024年度审计专项报告', 35000000.00, '2024', 'AUDIT', '2025-03-15',
    '{"reportType": "SPECIAL", "period": "AUDIT-2024", "reportNo": "FR-AUDIT-2024-001", "unit": "审计部", "auditOpinion": "标准无保留意见", "auditFirm": "XX会计师事务所", "auditDate": "2025-03-10"}'::jsonb,
    '2025-03-15 18:00:00', '审计部',
    '根据XX会计师事务所出具的审计报告，公司2024年度财务报表在所有重大方面公允反映了公司的财务状况、经营成果和现金流量，审计意见为标准无保留意见。',
    '示例公司', '永久', 'internal', 'archived', '2025-03-15 18:00:00', 0
)
ON CONFLICT (archive_code) DO NOTHING;

-- 输出插入结果
DO $$
BEGIN
    RAISE NOTICE '财务报告演示数据插入完成！';
    RAISE NOTICE '共插入 10 条财务报告记录：';
    RAISE NOTICE '  - 5 条月度报告';
    RAISE NOTICE '  - 2 条季度报告';
    RAISE NOTICE '  - 1 条半年度报告';
    RAISE NOTICE '  - 1 条年度报告';
    RAISE NOTICE '  - 1 条专项报告';
END $$;
