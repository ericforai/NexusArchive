-- Input: Flyway 迁移引擎
-- Output: 为缺文件的档案生成虚拟文件记录，以便触发 PDF 实时生成
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 目标：彻底解决 "暂无关联文件" 问题
-- 策略：对于 acc_archive 中有记录但 arc_file_content 无记录的，插入一条 "虚拟文件记录"。
--       PoolController 会发现该文件物理不存在，但 source_data 有值，从而调用 VoucherPdfGeneratorService 生成 PDF。

INSERT INTO arc_file_content (
    id, 
    item_id, 
    archival_code, 
    file_name, 
    file_type, 
    file_size,
    storage_path, 
    created_time, 
    source_system, 
    voucher_word,
    doc_date,
    source_data, 
    voucher_type,
    pre_archive_status
)
SELECT 
    -- 1. ID: 生成确定性 ID 以便幂等 (使用 UUID v4 也可以，这里用 md5 模拟)
    md5('virtual-file-' || a.id)::uuid::varchar,
    
    -- 2. Item ID
    a.id,
    
    -- 3. Archival Code
    a.archive_code,
    
    -- 4. File Name
    a.archive_code || '.pdf',
    
    -- 5. File Type
    'pdf',
    
    -- 6. Size (0 for now)
    0,
    
    -- 7. Storage Path (指向量身定制的路径，确保 PoolController 找不到物理文件从而触发生成)
    '/tmp/nexusarchive/generated/' || a.archive_code || '.pdf',
    
    -- 8. Created Time
    NOW(),
    
    -- 9. Source System
    'GENERATED',
    
    -- 10. Voucher Word (截断到64字符以适应列宽限制)
    LEFT(COALESCE(a.archive_code, '未命名'), 64), -- 使用 archive_code 作为 voucher_word
    
    -- 11. Doc Date
    a.doc_date,
    
    -- 12. Source Data (构造 JSON 供 PDF 生成器使用)
    format('{"header": {"displayname": "%s", "maketime": "%s", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "%s", "debit_original": %s, "credit_original": %s, "subjectName": "自动生成科目"}]}', 
           a.archive_code, 
           COALESCE(TO_CHAR(a.doc_date, 'YYYY-MM-DD'), '1970-01-01'),
           REPLACE(COALESCE(a.summary, '自动生成凭证'), '"', '\"'),
           COALESCE(a.amount, 0)::text,
           COALESCE(a.amount, 0)::text
    ),
    
    -- 13. Voucher Type
    'VOUCHER',
    
    -- 14. Status
    'ARCHIVED'

FROM acc_archive a
WHERE NOT EXISTS (SELECT 1 FROM arc_file_content f WHERE f.item_id = a.id);
