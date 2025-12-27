-- Input: Flyway 迁移引擎
-- Output: 修复演示附件存储路径
-- Pos: 数据库迁移脚本

-- 问题：arc_file_content 中的 storage_path 是旧数据，指向不存在的路径
-- 解决：强制更新为正确的 uploads/demo/ 路径

-- file-invoice-001: 差旅费发票
UPDATE arc_file_content 
SET storage_path = 'uploads/demo/25312000000349611002_ba1d.pdf',
    file_hash = '4c40ce396c10762acfd891c897f986c7646cecc335ced88a7c8d9e10cac44f02',
    hash_algorithm = 'SHA-256'
WHERE id = 'file-invoice-001';

-- file-invoice-002: 办公用品发票
UPDATE arc_file_content 
SET storage_path = 'uploads/demo/dzfp_25314000000004648601_上海市长宁区吴奕聪餐饮店_20251025012013.pdf',
    file_hash = '4fe6caa86fdc175a7cb35887ba5e3ee95460250cd00f7c3b84478af3720d696e',
    hash_algorithm = 'SHA-256'
WHERE id = 'file-invoice-002';

-- file-invoice-003: 销售发票
UPDATE arc_file_content 
SET storage_path = 'uploads/demo/上海米山神鸡餐饮管理有限公司_发票金额201.00元.pdf',
    file_hash = 'b88176ca3d3dcc0ddd3e9da3cda5c8712ad0c2abde9e6293679dbab5177d562e',
    hash_algorithm = 'SHA-256'
WHERE id = 'file-invoice-003';
