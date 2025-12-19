-- V66: 优化 ERP 预置模板激活状态 (Medium #11 修复)
-- 将 V60 中预置的标准模板设为非激活状态，防止在未配置真实参数的情况下误触发连接行为。

UPDATE sys_erp_config 
SET is_active = 0 
WHERE name IN ('金蝶云星空 (标准模板)', '泛微 OA (标准模板)', '用友 YonSuite (标准模板)');
