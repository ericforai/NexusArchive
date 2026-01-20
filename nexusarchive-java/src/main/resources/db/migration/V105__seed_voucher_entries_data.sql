-- V105: 为 V69 种子数据补充完整的会计分录(custom_metadata)
-- 解决全景视图"暂无分录数据"问题
-- 分录格式参考 V102__seed_relationship_demo_data.sql

-- ============================================
-- 1. 付款凭证-餐饮费类
-- ============================================

-- arc-2024-001: 付款凭证-上海米山神鸡餐饮管理有限公司, 201.00
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 201.00, "credit_org": 0, "accsubject": {"code": "6602.01", "name": "管理费用-业务招待费"}, "description": "上海米山神鸡餐饮-业务招待"},
    {"id": "2", "debit_org": 0, "credit_org": 201.00, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "银行付款"}
]'
WHERE id = 'arc-2024-001' AND (custom_metadata IS NULL OR custom_metadata = '{}' OR custom_metadata = '[]');

-- arc-2024-002: 付款凭证-上海市长宁区吴奕聪餐饮店, 156.00
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 156.00, "credit_org": 0, "accsubject": {"code": "6602.01", "name": "管理费用-业务招待费"}, "description": "吴奕聪餐饮店-业务招待"},
    {"id": "2", "debit_org": 0, "credit_org": 156.00, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "银行付款"}
]'
WHERE id = 'arc-2024-002' AND (custom_metadata IS NULL OR custom_metadata = '{}' OR custom_metadata = '[]');

-- ============================================
-- 2. 收款凭证-服务收入类
-- ============================================

-- arc-2024-003: 收款凭证-软件开发服务收入, 35600.00
-- 含增值税13%：不含税收入 31504.42，增值税 4095.58
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 35600.00, "credit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "收到软件开发服务款"},
    {"id": "2", "debit_org": 0, "credit_org": 31504.42, "accsubject": {"code": "6001", "name": "主营业务收入-软件开发"}, "description": "软件开发服务收入"},
    {"id": "3", "debit_org": 0, "credit_org": 4095.58, "accsubject": {"code": "2221.01", "name": "应交税费-应交增值税(销项)"}, "description": "销项税额13%"}
]'
WHERE id = 'arc-2024-003' AND (custom_metadata IS NULL OR custom_metadata = '{}' OR custom_metadata = '[]');

-- arc-2024-007: 收款凭证-咨询服务收入, 18500.00
-- 含增值税6%：不含税收入 17452.83，增值税 1047.17
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 18500.00, "credit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "收到咨询服务款"},
    {"id": "2", "debit_org": 0, "credit_org": 17452.83, "accsubject": {"code": "6001.02", "name": "主营业务收入-咨询服务"}, "description": "咨询服务收入"},
    {"id": "3", "debit_org": 0, "credit_org": 1047.17, "accsubject": {"code": "2221.01", "name": "应交税费-应交增值税(销项)"}, "description": "销项税额6%"}
]'
WHERE id = 'arc-2024-007' AND (custom_metadata IS NULL OR custom_metadata = '{}' OR custom_metadata = '[]');

-- arc-2024-010: 收款凭证-技术服务收入, 45000.00
-- 含增值税6%：不含税收入 42452.83，增值税 2547.17
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 45000.00, "credit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "收到技术服务款"},
    {"id": "2", "debit_org": 0, "credit_org": 42452.83, "accsubject": {"code": "6001.03", "name": "主营业务收入-技术服务"}, "description": "技术服务收入"},
    {"id": "3", "debit_org": 0, "credit_org": 2547.17, "accsubject": {"code": "2221.01", "name": "应交税费-应交增值税(销项)"}, "description": "销项税额6%"}
]'
WHERE id = 'arc-2024-010' AND (custom_metadata IS NULL OR custom_metadata = '{}' OR custom_metadata = '[]');

-- ============================================
-- 3. 转账凭证-费用报销类
-- ============================================

-- arc-2024-004: 转账凭证-员工差旅费报销, 3280.00
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 1800.00, "credit_org": 0, "accsubject": {"code": "6602.02", "name": "管理费用-差旅费"}, "description": "住宿费", "aux_info": "员工: 张三"},
    {"id": "2", "debit_org": 980.00, "credit_org": 0, "accsubject": {"code": "6602.02", "name": "管理费用-差旅费"}, "description": "交通费", "aux_info": "员工: 张三"},
    {"id": "3", "debit_org": 500.00, "credit_org": 0, "accsubject": {"code": "6602.02", "name": "管理费用-差旅费"}, "description": "餐饮补贴", "aux_info": "员工: 张三"},
    {"id": "4", "debit_org": 0, "credit_org": 3280.00, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "报销付款"}
]'
WHERE id = 'arc-2024-004' AND (custom_metadata IS NULL OR custom_metadata = '{}' OR custom_metadata = '[]');

-- ============================================
-- 4. 付款凭证-IT服务费类
-- ============================================

-- arc-2024-005: 付款凭证-阿里云服务器费用, 12800.00
-- 含增值税6%：不含税金额 12075.47，进项税 724.53
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 12075.47, "credit_org": 0, "accsubject": {"code": "6602.05", "name": "管理费用-信息技术费"}, "description": "阿里云ECS服务器费用"},
    {"id": "2", "debit_org": 724.53, "credit_org": 0, "accsubject": {"code": "2221.02", "name": "应交税费-应交增值税(进项)"}, "description": "进项税额6%"},
    {"id": "3", "debit_org": 0, "credit_org": 12800.00, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "银行付款"}
]'
WHERE id = 'arc-2024-005' AND (custom_metadata IS NULL OR custom_metadata = '{}' OR custom_metadata = '[]');

-- ============================================
-- 5. 付款凭证-办公费类
-- ============================================

-- arc-2024-006: 付款凭证-办公用品采购, 2350.00
-- 含增值税13%：不含税金额 2079.65，进项税 270.35
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 2079.65, "credit_org": 0, "accsubject": {"code": "6602.03", "name": "管理费用-办公费"}, "description": "办公用品采购"},
    {"id": "2", "debit_org": 270.35, "credit_org": 0, "accsubject": {"code": "2221.02", "name": "应交税费-应交增值税(进项)"}, "description": "进项税额13%"},
    {"id": "3", "debit_org": 0, "credit_org": 2350.00, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "银行付款"}
]'
WHERE id = 'arc-2024-006' AND (custom_metadata IS NULL OR custom_metadata = '{}' OR custom_metadata = '[]');

-- arc-2024-008: 付款凭证-团队聚餐费用, 1580.00
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 1580.00, "credit_org": 0, "accsubject": {"code": "6602.04", "name": "管理费用-福利费"}, "description": "团队聚餐-团队建设"},
    {"id": "2", "debit_org": 0, "credit_org": 1580.00, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "银行付款"}
]'
WHERE id = 'arc-2024-008' AND (custom_metadata IS NULL OR custom_metadata = '{}' OR custom_metadata = '[]');

-- arc-2024-009: 付款凭证-快递物流费用, 680.00
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 680.00, "credit_org": 0, "accsubject": {"code": "6602.06", "name": "管理费用-邮寄费"}, "description": "顺丰快递费用"},
    {"id": "2", "debit_org": 0, "credit_org": 680.00, "accsubject": {"code": "1001", "name": "库存现金"}, "description": "现金付款"}
]'
WHERE id = 'arc-2024-009' AND (custom_metadata IS NULL OR custom_metadata = '{}' OR custom_metadata = '[]');

-- ============================================
-- 6. 合同类 (AC04) - 补充合同相关元数据
-- ============================================

-- arc-2024-c01: 年度技术服务协议-华为云, 58000.00
UPDATE acc_archive SET custom_metadata = '{
    "contractType": "技术服务协议",
    "vendor": "华为云",
    "serviceType": "云计算服务",
    "period": "2024年度",
    "paymentTerms": "按季度支付"
}'
WHERE id = 'arc-2024-c01' AND (custom_metadata IS NULL OR custom_metadata = '{}' OR custom_metadata = '[]');

-- arc-2024-c02: 办公室租赁合同, 36000.00
UPDATE acc_archive SET custom_metadata = '{
    "contractType": "租赁合同",
    "landlord": "上海泊冉物业管理有限公司",
    "area": "200平方米",
    "period": "2024年度",
    "rentPerMonth": 3000.00
}'
WHERE id = 'arc-2024-c02' AND (custom_metadata IS NULL OR custom_metadata = '{}' OR custom_metadata = '[]');

-- ============================================
-- 7. 财务报告类 (AC03) - 补充报告元数据
-- ============================================

-- arc-2023-r01: 2023年度财务决算报告
UPDATE acc_archive SET custom_metadata = '{
    "reportType": "年度决算报告",
    "fiscalYear": "2023",
    "auditor": "XX会计师事务所",
    "approvalDate": "2024-03-31",
    "status": "已批准"
}'
WHERE id = 'arc-2023-r01' AND (custom_metadata IS NULL OR custom_metadata = '{}' OR custom_metadata = '[]');

-- arc-2024-r01: 2024年第一季度财务报告
UPDATE acc_archive SET custom_metadata = '{
    "reportType": "季度财务报告",
    "fiscalYear": "2024",
    "quarter": "Q1",
    "preparedBy": "刘芳",
    "status": "已归档"
}'
WHERE id = 'arc-2024-r01' AND (custom_metadata IS NULL OR custom_metadata = '{}' OR custom_metadata = '[]');

-- ============================================
-- 8. 会计账簿类 (AC02) - 补充账簿元数据
-- ============================================

-- arc-2023-l01: 2023年度总账
UPDATE acc_archive SET custom_metadata = '{
    "bookType": "总账",
    "fiscalYear": "2023",
    "accountRange": "全部会计科目",
    "period": "全年",
    "printDate": "2024-01-15"
}'
WHERE id = 'arc-2023-l01' AND (custom_metadata IS NULL OR custom_metadata = '{}' OR custom_metadata = '[]');

-- arc-2023-l02: 2023年现金日记账
UPDATE acc_archive SET custom_metadata = '{
    "bookType": "现金日记账",
    "fiscalYear": "2023",
    "currency": "CNY",
    "period": "全年",
    "pageCount": 365
}'
WHERE id = 'arc-2023-l02' AND (custom_metadata IS NULL OR custom_metadata = '{}' OR custom_metadata = '[]');

-- 完成
