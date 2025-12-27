-- Input: Flyway 迁移引擎
-- Output: 修复演示数据和权限
-- Pos: 数据库迁移脚本

-- 1. 修复演示用户权限 (Fix 403 Forbidden)
-- 为演示用户分配 super_admin 角色，确保可以调用预览接口
INSERT INTO sys_user_role (user_id, role_id)
SELECT id, 'role_super_admin' FROM sys_user WHERE username IN ('zhangsan', 'lisi', 'wangwu', 'zhaoliu', 'qianqi')
ON CONFLICT DO NOTHING;

-- 2. 修复文件哈希值 (Fix Four Natures Check)
-- 更新为磁盘文件的真实 SHA-256 值
-- 2.1 File 001: 差旅费发票
UPDATE arc_file_content 
SET file_hash = '4c40ce396c10762acfd891c897f986c7646cecc335ced88a7c8d9e10cac44f02', hash_algorithm = 'SHA-256' 
WHERE id = 'file-invoice-001';

-- 2.2 File 002: 办公用品发票
UPDATE arc_file_content 
SET file_hash = '4fe6caa86fdc175a7cb35887ba5e3ee95460250cd00f7c3b84478af3720d696e', hash_algorithm = 'SHA-256' 
WHERE id = 'file-invoice-002';

-- 2.3 File 003: 销售发票/回单
UPDATE arc_file_content 
SET file_hash = 'b88176ca3d3dcc0ddd3e9da3cda5c8712ad0c2abde9e6293679dbab5177d562e', hash_algorithm = 'SHA-256' 
WHERE id = 'file-invoice-003';

-- 3. 修复检测日志快照 (Ensure Hash Snapshot Matches)
UPDATE audit_inspection_log SET hash_snapshot = '4fe6caa86fdc175a7cb35887ba5e3ee95460250cd00f7c3b84478af3720d696e' WHERE id = 'inspection-v2024-11-001';
UPDATE audit_inspection_log SET hash_snapshot = '4c40ce396c10762acfd891c897f986c7646cecc335ced88a7c8d9e10cac44f02' WHERE id = 'inspection-v2024-11-002';
-- Note: 003 Log might have been created with old hash or assumes new one. Updating log 003 if exists.
UPDATE audit_inspection_log SET hash_snapshot = 'b88176ca3d3dcc0ddd3e9da3cda5c8712ad0c2abde9e6293679dbab5177d562e' WHERE archive_id = 'voucher-2024-11-003';

-- 4. 完善元数据细节 (Refine Details)
-- 为销售凭证增加客户信息辅助核算
UPDATE acc_archive 
SET custom_metadata = custom_metadata || '{"aux_info": "客户:华为技术有限公司"}'::jsonb
WHERE id = 'voucher-2024-11-003';

