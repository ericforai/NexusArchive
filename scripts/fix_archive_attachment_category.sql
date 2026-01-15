-- 修正档案 BR-GROUP-2025-30Y-FIN-AC01-1003 的附件分类
-- 将 "银行回单" 调整为 "原始凭证"

UPDATE public.acc_archive_attachment 
SET attachment_type = 'invoice', 
    relation_desc = '原始凭证'
WHERE id = 'attach-link-003' 
  AND archive_id = 'voucher-2024-11-003';

-- 验证更新结果
SELECT id, archive_id, attachment_type, relation_desc 
FROM public.acc_archive_attachment 
WHERE id = 'attach-link-003';
