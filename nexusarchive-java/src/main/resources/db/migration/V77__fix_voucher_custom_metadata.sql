-- Input: Flyway 迁移引擎
-- Output: 修复演示凭证分录元数据
-- Pos: 数据库迁移脚本

-- 问题：voucher-2024-11-003 的 custom_metadata 可能存在旧数据或格式错误
-- 解决：强制更新为正确的 JSON 格式

-- 修复 voucher-2024-11-003: 收到客户货款
-- 标准会计分录格式：
--   借：银行存款 158,000
--   贷：应收账款—华为技术有限公司 158,000
UPDATE acc_archive 
SET custom_metadata = '[{"id":"1","description":"收到华为技术有限公司货款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":158000.00,"credit_org":0},{"id":"2","description":"核销应收账款","accsubject":{"code":"1122","name":"应收账款"},"debit_org":0,"credit_org":158000.00,"aux_info":"客户:华为技术有限公司"}]'
WHERE id = 'voucher-2024-11-003';
