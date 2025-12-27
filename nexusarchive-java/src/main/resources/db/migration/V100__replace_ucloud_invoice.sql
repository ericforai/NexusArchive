-- Input: Flyway 迁移引擎
-- Output: 替换 UCloud 发票为吴奕聪餐饮店发票 (用户请求)
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 目标：将 "UCloud" 相关的演示凭证替换为 "吴奕聪餐饮店"
-- 涉及凭证: voucher-2024-11-002 (原 票务/住宿 凭证)
-- 涉及文件: file-invoice-001 (原 25312000000349611002_ba1d.pdf)

-- 1. 更新文件记录 (arc_file_content)
UPDATE arc_file_content
SET 
    storage_path = 'uploads/demo/dzfp_25314000000004648601_上海市长宁区吴奕聪餐饮店_20251025012013.pdf',
    file_name = '电子发票_吴奕聪餐饮店_替换UCloud.pdf',
    file_size = 101657,
    file_hash = '4fe6caa86fdc175a7cb35887ba5e3ee95460250cd00f7c3b84478af3720d696e',
    hash_algorithm = 'SHA-256'
WHERE item_id = 'arc-2024-002'; -- Target by Item ID since file ID is unknown

-- 2. 更新档案/凭证记录 (acc_archive)
UPDATE acc_archive
SET 
    title = '支付业务招待费-吴奕聪餐饮店(替换)',
    amount = 657.00,
    doc_date = '2025-10-25',
    custom_metadata = '[{"id":"1","description":"支付业务招待费-商务宴请","accsubject":{"code":"6602","name":"管理费用-业务招待费"},"debit_org":657.00,"credit_org":0},{"id":"2","description":"银行付款","accsubject":{"code":"1002","name":"银行存款"},"debit_org":0,"credit_org":657.00}]',
    summary = '支付业务招待费-商务宴请',
    last_modified_time = NOW()
WHERE id = 'arc-2024-002';
