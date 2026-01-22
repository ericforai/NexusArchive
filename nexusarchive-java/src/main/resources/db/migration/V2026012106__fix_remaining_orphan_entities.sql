-- ============================================================================
-- V2026012106__fix_remaining_orphan_entities.sql
-- 进一步修复 DataConsistencyValidator 报告或潜在的孤儿全宗问题
-- ============================================================================

-- 为演示数据中的 fonds_code 创建对应的 sys_entity 记录（作为别名/镜像）
-- 这样一致性检查 SELECT FROM arc_original_voucher JOIN sys_entity 就能通过

INSERT INTO sys_entity (id, name, status, description, created_by)
SELECT 'BR-GROUP', '泊冉集团', 'ACTIVE', '基准数据全宗别名', 'system'
WHERE NOT EXISTS (SELECT 1 FROM sys_entity WHERE id = 'BR-GROUP');

INSERT INTO sys_entity (id, name, status, description, created_by)
SELECT 'BR-SALES', '泊冉销售', 'ACTIVE', '基准数据全宗别名', 'system'
WHERE NOT EXISTS (SELECT 1 FROM sys_entity WHERE id = 'BR-SALES');

INSERT INTO sys_entity (id, name, status, description, created_by)
SELECT 'BR-TRADE', '泊冉国贸', 'ACTIVE', '基准数据全宗别名', 'system'
WHERE NOT EXISTS (SELECT 1 FROM sys_entity WHERE id = 'BR-TRADE');

INSERT INTO sys_entity (id, name, status, description, created_by)
SELECT 'BR-MFG', '泊冉制造', 'ACTIVE', '基准数据全宗别名', 'system'
WHERE NOT EXISTS (SELECT 1 FROM sys_entity WHERE id = 'BR-MFG');

INSERT INTO sys_entity (id, name, status, description, created_by)
SELECT 'DEMO', '演示全宗', 'ACTIVE', '基准数据全宗别名', 'system'
WHERE NOT EXISTS (SELECT 1 FROM sys_entity WHERE id = 'DEMO');
