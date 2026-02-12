-- ============================================================
-- V2026021202: 修复发票链路编码不一致（arc_file_content vs arc_original_voucher）
-- ============================================================
-- Input: 历史迁移后可能残留的 INV-* 编码不一致数据
-- Output: 统一的发票编码，确保关系查询/全景视图稳定
-- Pos: 数据库迁移脚本
--
-- 问题现象：
-- 1) arc_file_content.archival_code 与 arc_original_voucher.voucher_no 不一致
-- 2) 前端/后端按 INV-* 查询时，可能命中不同链路，造成偶发 404
--
-- 修复策略（幂等）：
-- A. 仅针对 INV-* 记录，对齐 arc_file_content.archival_code -> ov.voucher_no
-- B. source_doc_id 若本应存放 INV-* 且与 voucher_no 不一致，则对齐

-- Step A: 修复文件记录中的发票编码
WITH mismatch AS (
    SELECT
        afc.id AS file_id,
        afc.archival_code AS old_code,
        ov.voucher_no AS new_code
    FROM arc_file_content afc
    JOIN arc_original_voucher ov
      ON ov.id = afc.item_id
     AND ov.deleted = 0
    WHERE afc.archival_code LIKE 'INV-%'
      AND ov.voucher_no LIKE 'INV-%'
      AND afc.archival_code <> ov.voucher_no
)
UPDATE arc_file_content afc
SET archival_code = m.new_code
FROM mismatch m
WHERE afc.id = m.file_id;

-- Step B: 对齐原始凭证 source_doc_id（仅限 INV-* 语义）
UPDATE arc_original_voucher ov
SET source_doc_id = ov.voucher_no
WHERE ov.deleted = 0
  AND ov.voucher_no LIKE 'INV-%'
  AND (
      ov.source_doc_id IS NULL
      OR ov.source_doc_id = ''
      OR (ov.source_doc_id LIKE 'INV-%' AND ov.source_doc_id <> ov.voucher_no)
  );

