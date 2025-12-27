-- ============================================================
-- 智能凭证关联规则引擎 - 数据库迁移脚本
-- V85__matching_engine_tables.sql
-- Author: System | Date: 2025-12-25
-- ============================================================

-- 1. 预置规则包
CREATE TABLE IF NOT EXISTS cfg_preset_kit (
    id              VARCHAR(50) PRIMARY KEY,
    industry        VARCHAR(50) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    is_default      BOOLEAN DEFAULT FALSE,
    created_time    TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE cfg_preset_kit IS '行业预置规则包';
COMMENT ON COLUMN cfg_preset_kit.industry IS '行业类型：GENERAL/TRADE/MANUFACTURING';

-- 2. 科目角色预置规则（正则）
CREATE TABLE IF NOT EXISTS cfg_account_role_preset (
    id              BIGSERIAL PRIMARY KEY,
    kit_id          VARCHAR(50) NOT NULL REFERENCES cfg_preset_kit(id),
    account_pattern VARCHAR(100) NOT NULL,
    account_role    VARCHAR(30) NOT NULL,
    priority        INT DEFAULT 0,
    created_time    TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_account_preset_kit ON cfg_account_role_preset(kit_id);
COMMENT ON TABLE cfg_account_role_preset IS '科目角色预置规则（正则匹配）';

-- 3. 单据类型预置规则
CREATE TABLE IF NOT EXISTS cfg_doc_type_preset (
    id              BIGSERIAL PRIMARY KEY,
    kit_id          VARCHAR(50) NOT NULL REFERENCES cfg_preset_kit(id),
    doc_type_pattern VARCHAR(100) NOT NULL,
    keywords        TEXT[],
    evidence_role   VARCHAR(30) NOT NULL,
    priority        INT DEFAULT 0,
    created_time    TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_doc_preset_kit ON cfg_doc_type_preset(kit_id);
COMMENT ON TABLE cfg_doc_type_preset IS '单据类型预置规则';

-- 4. 科目角色映射（客户级别）
CREATE TABLE IF NOT EXISTS cfg_account_role_mapping (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT NOT NULL,
    account_code    VARCHAR(50) NOT NULL,
    aux_type        VARCHAR(50),
    account_role    VARCHAR(30) NOT NULL,
    source          VARCHAR(20) DEFAULT 'PRESET',
    created_time    TIMESTAMP DEFAULT NOW(),
    updated_time    TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uk_account_role UNIQUE (company_id, account_code, aux_type)
);

CREATE INDEX IF NOT EXISTS idx_account_mapping_company ON cfg_account_role_mapping(company_id);
COMMENT ON TABLE cfg_account_role_mapping IS '客户科目角色映射';
COMMENT ON COLUMN cfg_account_role_mapping.aux_type IS '辅助核算类别：PERSONAL/COMPANY/PROJECT/NONE';
COMMENT ON COLUMN cfg_account_role_mapping.source IS '来源：PRESET/MANUAL';

-- 5. 单据类型映射（客户级别）
CREATE TABLE IF NOT EXISTS cfg_doc_type_mapping (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT NOT NULL,
    customer_doc_type VARCHAR(100) NOT NULL,
    evidence_role   VARCHAR(30) NOT NULL,
    display_name    VARCHAR(100),
    source          VARCHAR(20) DEFAULT 'PRESET',
    created_time    TIMESTAMP DEFAULT NOW(),
    updated_time    TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uk_doc_type UNIQUE (company_id, customer_doc_type)
);

CREATE INDEX IF NOT EXISTS idx_doc_mapping_company ON cfg_doc_type_mapping(company_id);
COMMENT ON TABLE cfg_doc_type_mapping IS '客户单据类型映射';

-- 6. 规则模板
CREATE TABLE IF NOT EXISTS match_rule_template (
    id              VARCHAR(50) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    version         VARCHAR(20) NOT NULL,
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    scene           VARCHAR(50) NOT NULL,
    config          JSONB NOT NULL,
    description     TEXT,
    created_time    TIMESTAMP DEFAULT NOW(),
    updated_time    TIMESTAMP DEFAULT NOW(),
    updated_by      BIGINT
);

CREATE INDEX IF NOT EXISTS idx_template_scene ON match_rule_template(scene);
CREATE INDEX IF NOT EXISTS idx_template_status ON match_rule_template(status);
COMMENT ON TABLE match_rule_template IS '智能关联规则模板';

-- 7. 匹配结果
CREATE TABLE IF NOT EXISTS voucher_match_result (
    id              BIGSERIAL PRIMARY KEY,
    task_id         VARCHAR(50),
    batch_task_id   VARCHAR(50),
    match_batch_id  VARCHAR(50) NOT NULL,
    voucher_id      VARCHAR(50) NOT NULL,
    voucher_hash    VARCHAR(64),
    config_hash     VARCHAR(64),
    template_id     VARCHAR(50),
    template_version VARCHAR(20),
    scene           VARCHAR(50),
    confidence      DECIMAL(5,4),
    status          VARCHAR(30) NOT NULL,
    match_details   JSONB,
    missing_docs    TEXT[],
    is_latest       BOOLEAN DEFAULT TRUE,
    created_time    TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_result_voucher ON voucher_match_result(voucher_id);
CREATE INDEX IF NOT EXISTS idx_result_task ON voucher_match_result(task_id);
CREATE INDEX IF NOT EXISTS idx_result_batch_task ON voucher_match_result(batch_task_id);
CREATE INDEX IF NOT EXISTS idx_result_latest ON voucher_match_result(voucher_id, is_latest) WHERE is_latest = TRUE;
COMMENT ON TABLE voucher_match_result IS '凭证匹配结果';

-- 8. 凭证-源单关联
CREATE TABLE IF NOT EXISTS voucher_source_link (
    id              BIGSERIAL PRIMARY KEY,
    match_batch_id  VARCHAR(50),
    voucher_id      VARCHAR(50) NOT NULL,
    source_doc_id   VARCHAR(50) NOT NULL,
    evidence_role   VARCHAR(30),
    link_type       VARCHAR(20) NOT NULL,
    match_score     INT,
    match_reasons   TEXT[],
    allocated_amount DECIMAL(18,2),
    is_auto         BOOLEAN DEFAULT TRUE,
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    created_time    TIMESTAMP DEFAULT NOW(),
    created_by      BIGINT,
    CONSTRAINT uk_voucher_source_batch UNIQUE (voucher_id, source_doc_id, match_batch_id)
);

CREATE INDEX IF NOT EXISTS idx_link_voucher ON voucher_source_link(voucher_id);
CREATE INDEX IF NOT EXISTS idx_link_source ON voucher_source_link(source_doc_id);
CREATE INDEX IF NOT EXISTS idx_link_status ON voucher_source_link(status);
COMMENT ON TABLE voucher_source_link IS '凭证-源单关联关系';
COMMENT ON COLUMN voucher_source_link.link_type IS 'must_link/should_link/may_link';

-- 9. 匹配日志（审计）
CREATE TABLE IF NOT EXISTS match_log (
    id              BIGSERIAL PRIMARY KEY,
    match_batch_id  VARCHAR(50),
    voucher_id      VARCHAR(50),
    action          VARCHAR(30) NOT NULL,
    evidence_role   VARCHAR(30),
    source_doc_id   VARCHAR(50),
    score           INT,
    reasons         TEXT[],
    before_state    JSONB,
    after_state     JSONB,
    is_manual_override BOOLEAN DEFAULT FALSE,
    operator_id     BIGINT,
    operator_name   VARCHAR(100),
    client_ip       VARCHAR(50),
    operation_time  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_match_log_voucher ON match_log(voucher_id);
CREATE INDEX IF NOT EXISTS idx_match_log_time ON match_log(operation_time);
COMMENT ON TABLE match_log IS '匹配日志（审计与自学习）';

-- 10. 候选查询优化索引（在已有表上）
CREATE INDEX IF NOT EXISTS idx_orig_voucher_scene_search 
    ON arc_original_voucher(voucher_type, business_date, amount);

-- 10.1 金额优先索引（适用于先按金额范围过滤的场景）
CREATE INDEX IF NOT EXISTS idx_orig_voucher_amount_date 
    ON arc_original_voucher(amount, business_date);

-- 10.2 复合索引优化：voucher_id + status + is_latest
CREATE INDEX IF NOT EXISTS idx_result_voucher_status 
    ON voucher_match_result(voucher_id, status, is_latest);

-- 10.3 match_log 分区预留注释（如果日志量大，考虑按月分区）
-- PARTITION BY RANGE (operation_time)

-- 11. 初始化默认预置包
INSERT INTO cfg_preset_kit (id, industry, name, description, is_default)
VALUES ('KIT_GENERAL', 'GENERAL', '通用行业预置包', '适用于大多数企业的默认规则', TRUE)
ON CONFLICT (id) DO NOTHING;

-- 12. 初始化科目角色预置规则
INSERT INTO cfg_account_role_preset (kit_id, account_pattern, account_role, priority) VALUES
('KIT_GENERAL', '^1001.*', 'CASH', 100),
('KIT_GENERAL', '^1002.*', 'BANK', 100),
('KIT_GENERAL', '^1122.*', 'RECEIVABLE', 100),
('KIT_GENERAL', '^1123.*', 'RECEIVABLE', 90),
('KIT_GENERAL', '^2202.*', 'PAYABLE', 100),
('KIT_GENERAL', '^2203.*', 'PAYABLE', 90),
('KIT_GENERAL', '^2211.*', 'SALARY', 100),
('KIT_GENERAL', '^2221.*', 'TAX', 100),
('KIT_GENERAL', '^1601.*', 'ASSET', 100),
('KIT_GENERAL', '^1602.*', 'ASSET', 90),
('KIT_GENERAL', '^6601.*', 'EXPENSE', 100),
('KIT_GENERAL', '^6602.*', 'EXPENSE', 100),
('KIT_GENERAL', '^6603.*', 'EXPENSE', 100),
('KIT_GENERAL', '^6001.*', 'REVENUE', 100),
('KIT_GENERAL', '^6051.*', 'REVENUE', 90)
ON CONFLICT DO NOTHING;

-- 13. 初始化单据类型预置规则
INSERT INTO cfg_doc_type_preset (kit_id, doc_type_pattern, keywords, evidence_role, priority) VALUES
('KIT_GENERAL', '付款.*', ARRAY['付款审批', '付款申请', '付款指令'], 'AUTHORIZATION', 100),
('KIT_GENERAL', '银行.*|回单.*', ARRAY['银行回单', '转账回单', '支付凭证', '网银回单'], 'SETTLEMENT', 100),
('KIT_GENERAL', '发票.*|增值税.*', ARRAY['发票', '增值税专用发票', '增值税普通发票'], 'TAX_EVIDENCE', 100),
('KIT_GENERAL', '合同.*|协议.*|订单.*', ARRAY['合同', '协议', '订单', '框架协议'], 'CONTRACTUAL_BASIS', 100),
('KIT_GENERAL', '入库.*|验收.*|签收.*', ARRAY['入库单', '验收单', '签收单', '收货单'], 'EXECUTION_PROOF', 100),
('KIT_GENERAL', '报销.*|费用.*', ARRAY['报销单', '费用申请', '差旅报销'], 'ACCOUNTING_TRIGGER', 100)
ON CONFLICT DO NOTHING;
