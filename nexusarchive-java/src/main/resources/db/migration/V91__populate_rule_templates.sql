-- ============================================================
-- 智能凭证关联规则引擎 - 规则模板初始化
-- V91__populate_rule_templates.sql
-- Author: System | Date: 2025-12-25
-- ============================================================

-- 1. 付款业务 (Payment)
-- 关联: 采购发票 + 银行回单 + 合同
INSERT INTO match_rule_template (id, name, version, scene, config, description)
VALUES (
    'T01_PAYMENT',
    '付款业务关联规则',
    'v1.0',
    'PAYMENT',
    '{
        "mustLink": [
            {
                "evidenceRole": "TAX_EVIDENCE",
                "docTypeKeywords": ["发票", "增值税"], 
                "strategies": ["AMOUNT_EXACT", "DATE_PROXIMITY"]
            },
            {
                "evidenceRole": "SETTLEMENT",
                "docTypeKeywords": ["银行回单", "付款凭证"],
                "strategies": ["AMOUNT_EXACT", "DATE_PROXIMITY"]
            }
        ],
        "shouldLink": [
            {
                "evidenceRole": "CONTRACTUAL_BASIS",
                "docTypeKeywords": ["合同", "协议"],
                "strategies": ["FUZZY_NAME", "REF_NO"]
            }
        ]
    }',
    '适用于标准对公付款业务，需匹配发票和银行回单'
) ON CONFLICT (id) DO UPDATE SET config = EXCLUDED.config;

-- 2. 收款业务 (Receipt)
-- 关联: 银行回单 + 销售发票
INSERT INTO match_rule_template (id, name, version, scene, config, description)
VALUES (
    'T02_RECEIPT',
    '收款业务关联规则',
    'v1.0',
    'RECEIPT',
    '{
        "mustLink": [
            {
                "evidenceRole": "SETTLEMENT",
                "docTypeKeywords": ["银行回单", "收款凭证"],
                "strategies": ["AMOUNT_EXACT", "DATE_PROXIMITY"]
            }
        ],
        "shouldLink": [
            {
                "evidenceRole": "TAX_EVIDENCE",
                "docTypeKeywords": ["发票", "销售发票"],
                "strategies": ["AMOUNT_EXACT", "FUZZY_NAME"]
            }
        ]
    }',
    '适用于标准销售收款业务'
) ON CONFLICT (id) DO UPDATE SET config = EXCLUDED.config;

-- 3. 费用报销 (Expense)
-- 关联: 费用发票 + 报销单
INSERT INTO match_rule_template (id, name, version, scene, config, description)
VALUES (
    'T03_EXPENSE',
    '费用报销关联规则',
    'v1.0',
    'EXPENSE',
    '{
        "mustLink": [
            {
                "evidenceRole": "ACCOUNTING_TRIGGER",
                "docTypeKeywords": ["报销单", "费用申请"],
                "strategies": ["AMOUNT_EXACT", "REF_NO"]
            },
            {
                "evidenceRole": "TAX_EVIDENCE",
                "docTypeKeywords": ["发票", "行程单"],
                "strategies": ["DATE_PROXIMITY"]
            }
        ],
        "mayLink": [
            {
                "evidenceRole": "SETTLEMENT",
                "docTypeKeywords": ["支付凭证"],
                "strategies": ["AMOUNT_EXACT"]
            }
        ]
    }',
    '适用于员工费用报销业务'
) ON CONFLICT (id) DO UPDATE SET config = EXCLUDED.config;

-- 4. 采购入库 (Purchase)
-- 关联: 采购订单 + 入库单 + 发票
INSERT INTO match_rule_template (id, name, version, scene, config, description)
VALUES (
    'T04_PURCHASE',
    '采购供应链关联规则',
    'v1.0',
    'PURCHASE_IN',
    '{
        "mustLink": [
            {
                "evidenceRole": "CONTRACTUAL_BASIS",
                "docTypeKeywords": ["采购订单", "合同"],
                "strategies": ["REF_NO", "FUZZY_NAME"]
            },
            {
                "evidenceRole": "EXECUTION_PROOF",
                "docTypeKeywords": ["入库单", "验收单"],
                "strategies": ["DATE_PROXIMITY", "REF_NO"]
            }
        ],
        "shouldLink": [
            {
                "evidenceRole": "TAX_EVIDENCE",
                "docTypeKeywords": ["发票"],
                "strategies": ["AMOUNT_EXACT"]
            }
        ]
    }',
    '适用于原材料或商品采购入库'
) ON CONFLICT (id) DO UPDATE SET config = EXCLUDED.config;

-- 5. 销售出库 (Sales)
-- 关联: 销售订单 + 出库单
INSERT INTO match_rule_template (id, name, version, scene, config, description)
VALUES (
    'T05_SALES',
    '销售供应链关联规则',
    'v1.0',
    'SALES_OUT',
    '{
        "mustLink": [
            {
                "evidenceRole": "CONTRACTUAL_BASIS",
                "docTypeKeywords": ["销售订单"],
                "strategies": ["REF_NO", "FUZZY_NAME"]
            },
            {
                "evidenceRole": "EXECUTION_PROOF",
                "docTypeKeywords": ["出库单", "发货单"],
                "strategies": ["DATE_PROXIMITY", "REF_NO"]
            }
        ],
        "shouldLink": [
            {
                "evidenceRole": "TAX_EVIDENCE",
                "docTypeKeywords": ["发票"],
                "strategies": ["AMOUNT_EXACT"]
            }
        ]
    }',
    '适用于商品销售出库'
) ON CONFLICT (id) DO UPDATE SET config = EXCLUDED.config;
