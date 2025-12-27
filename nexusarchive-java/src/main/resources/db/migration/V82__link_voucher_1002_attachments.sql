-- Input: Flyway 迁移引擎
-- Output: 修复演示数据 - 关联 Voucher 1002 的附件
-- Pos: 数据库迁移脚本

-- 由于 V80 仅插入了文件内容表 (arc_file_content)，
-- 但后端逻辑依赖 acc_archive_attachment 表来查找和分类附件，
-- 因此这里补全关联记录。

-- 1. 关联银行回单 (bank_slip)
INSERT INTO acc_archive_attachment (id, archive_id, file_id, attachment_type, relation_desc, created_by, created_at)
SELECT 'link-bank-1002', 'voucher-2024-11-002', 'file-bank-receipt-1002', 'bank_slip', '银行回单附件', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_attachment WHERE id = 'link-bank-1002');

-- 2. 关联报销单 (other -> 附件)
INSERT INTO acc_archive_attachment (id, archive_id, file_id, attachment_type, relation_desc, created_by, created_at)
SELECT 'link-reimb-1002', 'voucher-2024-11-002', 'file-reimbursement-1002', 'other', '员工报销单据', 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_archive_attachment WHERE id = 'link-reimb-1002');
