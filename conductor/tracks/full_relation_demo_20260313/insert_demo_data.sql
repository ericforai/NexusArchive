-- 全链路演示数据构建脚本 (v4)
-- 目标：修正存储路径并补全上下游关系

-- 1. 清理
DELETE FROM public.acc_archive_relation WHERE id LIKE 'demo-rel-%';
DELETE FROM public.arc_file_content WHERE id LIKE 'f-demo-%';
DELETE FROM public.acc_archive WHERE id LIKE 'demo-%';

-- 2. 插入档案记录 (acc_archive)
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, destruction_status)
VALUES 
('demo-payment-001', 'DEMO', 'FK-202311-001', 'AC04', '11月办公用品采购付款单', '2023', '11', '30Y', '演示公司', '张三', 'archived', 58000.00, '2023-11-04', 'NORMAL'),
('demo-app-001',     'DEMO', 'SQ-202311-001', 'AC04', '办公用品采购申请单',     '2023', '11', '30Y', '演示公司', '李四', 'archived', 58000.00, '2023-10-28', 'NORMAL'),
('demo-con-001',     'DEMO', 'HT-202311-001', 'AC04', '办公用品年度供应合同',     '2023', '11', '30Y', '演示公司', '王五', 'archived', 200000.00, '2023-01-01', 'NORMAL'),
('demo-inv-001',     'DEMO', 'FP-202311-001', 'AC04', '增值税专用发票-办公用品',   '2023', '11', '30Y', '演示公司', '供应商', 'archived', 58000.00, '2023-11-02', 'NORMAL'),
('demo-rec-001',     'DEMO', 'HD-202311-001', 'AC04', '招商银行电子回单-001',   '2023', '11', '30Y', '演示公司', '系统', 'archived', 58000.00, '2023-11-05', 'NORMAL');

-- 3. 插入关联关系 (形成连续链路)
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc)
VALUES 
('demo-rel-1', 'seed-voucher-001', 'demo-payment-001', 'RELATED', '付款单据'),
('demo-rel-2', 'demo-payment-001', 'demo-app-001',     'RELATED', '申请依据'),
('demo-rel-3', 'demo-app-001',     'demo-con-001',     'RELATED', '合同依据'),
('demo-rel-4', 'demo-con-001',     'demo-inv-001',     'RELATED', '关联发票'),
('demo-rel-5', 'demo-inv-001',     'demo-rec-001',     'RELATED', '支付回单');

-- 4. 插入文件内容 (使用相对路径 ./data/archives/...)
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, storage_path, item_id, pre_archive_status)
VALUES 
('f-demo-payment-001', 'FK-202311-001', '付款单_FK-202311-001.pdf', 'pdf', 104331, './data/archives/demo/FK-202311-001.pdf', 'demo-payment-001', 'COMPLETED'),
('f-demo-app-001',     'SQ-202311-001', '付款申请_SQ-202311-001.pdf', 'pdf', 104331, './data/archives/demo/SQ-202311-001.pdf', 'demo-app-001',     'COMPLETED'),
('f-demo-con-001',     'HT-202311-001', '采购合同_HT-202311-001.pdf', 'pdf', 104331, './data/archives/demo/HT-202311-001.pdf', 'demo-con-001',     'COMPLETED'),
('f-demo-inv-001',     'FP-202311-001', '增值税发票_FP-202311-001.pdf', 'pdf', 104331, './data/archives/demo/FP-202311-001.pdf', 'demo-inv-001',     'COMPLETED'),
('f-demo-rec-001',     'HD-202311-001', '银行回单_HD-202311-001.pdf', 'pdf', 104331, './data/archives/demo/HD-202311-001.pdf', 'demo-rec-001',     'COMPLETED');
