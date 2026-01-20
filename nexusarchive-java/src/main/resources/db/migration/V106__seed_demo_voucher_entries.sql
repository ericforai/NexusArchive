-- V106: 为 demo-2025 系列记账凭证补充会计分录
-- 解决全景视图显示"暂无分录数据"的问题

-- ============================================
-- 1. demo-2025-br-group-voucher-001: 记账凭证-融资手续费, 800000.00
-- ============================================
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 800000.00, "credit_org": 0, "accsubject": {"code": "6603", "name": "财务费用-融资手续费"}, "description": "银行借款融资手续费"},
    {"id": "2", "debit_org": 0, "credit_org": 800000.00, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "银行付款"}
]'
WHERE id = 'demo-2025-br-group-voucher-001' AND (custom_metadata IS NULL OR custom_metadata::text = '{}' OR custom_metadata::text = '[]');

-- ============================================
-- 2. demo-2025-br-mfg-voucher-001: 记账凭证-原材料采购, 98000.00
-- 含增值税13%：不含税 86725.66，进项税 11274.34
-- ============================================
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 86725.66, "credit_org": 0, "accsubject": {"code": "1403", "name": "原材料"}, "description": "原材料采购入库", "aux_info": "供应商: 华东钢铁"},
    {"id": "2", "debit_org": 11274.34, "credit_org": 0, "accsubject": {"code": "2221.02", "name": "应交税费-应交增值税(进项)"}, "description": "进项税额13%"},
    {"id": "3", "debit_org": 0, "credit_org": 98000.00, "accsubject": {"code": "2202", "name": "应付账款"}, "description": "应付华东钢铁货款", "aux_info": "供应商: 华东钢铁"}
]'
WHERE id = 'demo-2025-br-mfg-voucher-001' AND (custom_metadata IS NULL OR custom_metadata::text = '{}' OR custom_metadata::text = '[]');

-- ============================================
-- 3. demo-2025-br-sales-voucher-001: 记账凭证-销售回款, 126500.00
-- 含增值税13%：不含税 111946.90，销项税 14553.10
-- ============================================
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 126500.00, "credit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "收到华东客户销售回款"},
    {"id": "2", "debit_org": 0, "credit_org": 111946.90, "accsubject": {"code": "6001", "name": "主营业务收入-产品销售"}, "description": "产品销售收入", "aux_info": "客户: 华东客户"},
    {"id": "3", "debit_org": 0, "credit_org": 14553.10, "accsubject": {"code": "2221.01", "name": "应交税费-应交增值税(销项)"}, "description": "销项税额13%"}
]'
WHERE id = 'demo-2025-br-sales-voucher-001' AND (custom_metadata IS NULL OR custom_metadata::text = '{}' OR custom_metadata::text = '[]');

-- ============================================
-- 4. demo-2025-br-trade-voucher-001: 记账凭证-进口采购, 452000.00
-- 进口商品含关税：货物成本 400000，进口关税 52000
-- ============================================
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 400000.00, "credit_org": 0, "accsubject": {"code": "1405", "name": "库存商品"}, "description": "进口商品入库"},
    {"id": "2", "debit_org": 52000.00, "credit_org": 0, "accsubject": {"code": "6401", "name": "销售费用-关税"}, "description": "进口关税"},
    {"id": "3", "debit_org": 0, "credit_org": 452000.00, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "支付进口采购款"}
]'
WHERE id = 'demo-2025-br-trade-voucher-001' AND (custom_metadata IS NULL OR custom_metadata::text = '{}' OR custom_metadata::text = '[]');

-- ============================================
-- 5. demo-2025-brjt-voucher-001: 记账凭证-项目进度款, 260000.00
-- 工程进度款：含6%增值税，不含税 245283.02，税额 14716.98
-- ============================================
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 260000.00, "credit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "收到项目一期进度款"},
    {"id": "2", "debit_org": 0, "credit_org": 245283.02, "accsubject": {"code": "6001.04", "name": "主营业务收入-工程服务"}, "description": "工程服务收入", "aux_info": "项目: 信息化建设一期"},
    {"id": "3", "debit_org": 0, "credit_org": 14716.98, "accsubject": {"code": "2221.01", "name": "应交税费-应交增值税(销项)"}, "description": "销项税额6%"}
]'
WHERE id = 'demo-2025-brjt-voucher-001' AND (custom_metadata IS NULL OR custom_metadata::text = '{}' OR custom_metadata::text = '[]');

-- ============================================
-- 6. demo-2025-comp001-voucher-001: 记账凭证-差旅报销, 3280.00
-- ============================================
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 1800.00, "credit_org": 0, "accsubject": {"code": "6602.02", "name": "管理费用-差旅费"}, "description": "住宿费", "aux_info": "员工: 王明"},
    {"id": "2", "debit_org": 1080.00, "credit_org": 0, "accsubject": {"code": "6602.02", "name": "管理费用-差旅费"}, "description": "交通费-高铁", "aux_info": "员工: 王明"},
    {"id": "3", "debit_org": 400.00, "credit_org": 0, "accsubject": {"code": "6602.02", "name": "管理费用-差旅费"}, "description": "餐饮补贴", "aux_info": "员工: 王明"},
    {"id": "4", "debit_org": 0, "credit_org": 3280.00, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "报销转账付款"}
]'
WHERE id = 'demo-2025-comp001-voucher-001' AND (custom_metadata IS NULL OR custom_metadata::text = '{}' OR custom_metadata::text = '[]');

-- ============================================
-- 7. demo-2025-demo-voucher-001: 记账凭证-审计服务费, 120000.00
-- 审计服务费6%税率：不含税 113207.55，税额 6792.45
-- ============================================
UPDATE acc_archive SET custom_metadata = '[
    {"id": "1", "debit_org": 113207.55, "credit_org": 0, "accsubject": {"code": "6602.07", "name": "管理费用-审计费"}, "description": "年度审计服务费"},
    {"id": "2", "debit_org": 6792.45, "credit_org": 0, "accsubject": {"code": "2221.02", "name": "应交税费-应交增值税(进项)"}, "description": "进项税额6%"},
    {"id": "3", "debit_org": 0, "credit_org": 120000.00, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "支付审计服务费", "aux_info": "供应商: XX会计师事务所"}
]'
WHERE id = 'demo-2025-demo-voucher-001' AND (custom_metadata IS NULL OR custom_metadata::text = '{}' OR custom_metadata::text = '[]');

-- 完成
