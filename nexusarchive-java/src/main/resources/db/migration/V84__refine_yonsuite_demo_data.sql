-- Input: Flyway 迁移引擎
-- Output: 深度清洗电子凭证池 YONSUITE 相关演示数据
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 目标：让数据看起来是由专业会计生成的，而不是随机乱填的
-- 场景包括：日常报销、采购付款、销售收款、计提薪酬、税金缴纳

-- 1. 清理旧的、可能不一致的元数据关联 (Safe cleanup)
-- (此处不删除，而是 update，保持 ID 不变)

-- 2. 构造专业数据集
-- 我们使用临时表或 CASE WHEN 逻辑来分布不同的业务场景

-- 更新 1: 管理费用-差旅费 (约 20% 数据)
UPDATE arc_file_content
SET 
  summary = '[管理费用-差旅费] 12月销售部差旅报销 (上海/北京)', 
  voucher_word = '记-202512-005', 
  doc_date = '2025-12-15'
WHERE source_system LIKE '%YONSUITE%' 
  AND abs(hashtext(id)) % 5 = 0;

UPDATE arc_file_metadata_index
SET total_amount = 12580.50, invoice_number = '2310015890'
WHERE file_id IN (SELECT id FROM arc_file_content WHERE source_system LIKE '%YONSUITE%' AND abs(hashtext(id)) % 5 = 0);


-- 更新 2: 销售费用-业务招待 (约 20% 数据)
UPDATE arc_file_content
SET 
  summary = '[销售费用-业务招待费] 客户年终答谢会餐饮费', 
  voucher_word = '记-202512-008', 
  doc_date = '2025-12-18'
WHERE source_system LIKE '%YONSUITE%' 
  AND abs(hashtext(id)) % 5 = 1;

UPDATE arc_file_metadata_index
SET total_amount = 8800.00, invoice_number = '2310015891'
WHERE file_id IN (SELECT id FROM arc_file_content WHERE source_system LIKE '%YONSUITE%' AND abs(hashtext(id)) % 5 = 1);


-- 更新 3: 应付账款-货款 (约 20% 数据)
UPDATE arc_file_content
SET 
  summary = '[应付账款] 支付阿里云计算有限公司服务费', 
  voucher_word = '银付-202512-021', 
  doc_date = '2025-12-20'
WHERE source_system LIKE '%YONSUITE%' 
  AND abs(hashtext(id)) % 5 = 2;

UPDATE arc_file_metadata_index
SET total_amount = 45000.00, invoice_number = '2310015892'
WHERE file_id IN (SELECT id FROM arc_file_content WHERE source_system LIKE '%YONSUITE%' AND abs(hashtext(id)) % 5 = 2);


-- 更新 4: 银行存款-利息 (约 20% 数据)
UPDATE arc_file_content
SET 
  summary = '[财务费用-利息收入] 招商银行季度结息', 
  voucher_word = '银收-202512-001', 
  doc_date = '2025-12-21'
WHERE source_system LIKE '%YONSUITE%' 
  AND abs(hashtext(id)) % 5 = 3;

UPDATE arc_file_metadata_index
SET total_amount = 123.45, invoice_number = 'BANK-001'
WHERE file_id IN (SELECT id FROM arc_file_content WHERE source_system LIKE '%YONSUITE%' AND abs(hashtext(id)) % 5 = 3);


-- 更新 5: 研发支出-设备 (约 20% 数据)
UPDATE arc_file_content
SET 
  summary = '[研发支出-固定资产] 采购高性能计算工作站', 
  voucher_word = '转-202512-033', 
  doc_date = '2025-12-22'
WHERE source_system LIKE '%YONSUITE%' 
  AND abs(hashtext(id)) % 5 = 4;

UPDATE arc_file_metadata_index
SET total_amount = 128000.00, invoice_number = '2310015893'
WHERE file_id IN (SELECT id FROM arc_file_content WHERE source_system LIKE '%YONSUITE%' AND abs(hashtext(id)) % 5 = 4);


-- 3. 兜底逻辑：如果 arc_file_metadata_index 中没有数据，则插入默认数据
INSERT INTO arc_file_metadata_index (id, file_id, total_amount, invoice_number, parsed_time)
SELECT 
  (abs(hashtext(id)) + 100000), -- 简单生成由 ID 衍生的 metadata ID
  id, 
  500.00, 
  'AUTO-GEN', 
  NOW()
FROM arc_file_content 
WHERE source_system LIKE '%YONSUITE%'
  AND NOT EXISTS (SELECT 1 FROM arc_file_metadata_index WHERE file_id = arc_file_content.id);

-- 4. 再次确保所有 summary 都有值 (防止遗漏)
UPDATE arc_file_content
SET summary = '[待分类] 暂无业务摘要', voucher_word = 'TMP-001', doc_date = CURRENT_DATE
WHERE source_system LIKE '%YONSUITE%' AND summary IS NULL;
