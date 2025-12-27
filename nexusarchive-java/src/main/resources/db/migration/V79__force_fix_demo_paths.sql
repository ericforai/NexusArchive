-- Input: Flyway 迁移引擎
-- Output: 修复演示数据 - 强制修正附件存储路径
-- Pos: 数据库迁移脚本

-- 问题：后端日志显示 file-invoice-001 等文件的 storage_path 仍然是错误的 attachments/... 路径
-- 原因：可能是之前的 V76 迁移未正确生效或被由于某种原因回滚/覆盖
-- 解决：再次强制更新所有演示发票的 storage_path

-- 1. 差旅费发票 (voucher-2024-11-002 / 1002)
UPDATE arc_file_content 
SET storage_path = 'uploads/demo/25312000000349611002_ba1d.pdf',
    file_hash = '4c40ce396c10762acfd891c897f986c7646cecc335ced88a7c8d9e10cac44f02',
    hash_algorithm = 'SHA-256'
WHERE id = 'file-invoice-001';

-- 2. 业务招待费-吴奕聪 (voucher-2024-11-001 / 1001)
UPDATE arc_file_content 
SET storage_path = 'uploads/demo/dzfp_25314000000004648601_上海市长宁区吴奕聪餐饮店_20251025012013.pdf',
    file_hash = '4fe6caa86fdc175a7cb35887ba5e3ee95460250cd00f7c3b84478af3720d696e',
    hash_algorithm = 'SHA-256'
WHERE id = 'file-invoice-002';

-- 3. 业务招待费-米山神鸡 (voucher-2024-11-003 / 1003)
UPDATE arc_file_content 
SET storage_path = 'uploads/demo/上海米山神鸡餐饮管理有限公司_发票金额201.00元.pdf',
    file_hash = 'b88176ca3d3dcc0ddd3e9da3cda5c8712ad0c2abde9e6293679dbab5177d562e',
    hash_algorithm = 'SHA-256'
WHERE id = 'file-invoice-003';
