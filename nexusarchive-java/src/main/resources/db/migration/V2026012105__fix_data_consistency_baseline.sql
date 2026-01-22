-- ============================================================================
-- V2026012105__fix_data_consistency_baseline.sql
-- 修复 DataConsistencyValidator 报告的孤儿全宗和未知凭证类型问题
-- ============================================================================

-- 1. 修复 BRJT 孤儿全宗代码 (补全 sys_entity 记录)
-- 原因: V69 演示数据中使用了 BRJT，但基准表中缺少对应的法人实体记录
INSERT INTO sys_entity (id, name, status, description, created_by)
SELECT 'BRJT', '泊冉集团', 'ACTIVE', '系统初始化产生的基准法人实体', 'system'
WHERE NOT EXISTS (SELECT 1 FROM sys_entity WHERE id = 'BRJT');

-- 2. 修复 TRANSFER_VOUCHER 未知凭证类型 (归类到 AC01 原始凭证范畴)
-- 原因: 业务逻辑或演示数据中产生了 TRANSFER_VOUCHER 但字典表中未定义
INSERT INTO sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled)
SELECT 'ovt-transfer', 'AC01', '原始凭证', 'TRANSFER_VOUCHER', '转账凭证', '30Y', 5, true
WHERE NOT EXISTS (SELECT 1 FROM sys_original_voucher_type WHERE type_code = 'TRANSFER_VOUCHER');
