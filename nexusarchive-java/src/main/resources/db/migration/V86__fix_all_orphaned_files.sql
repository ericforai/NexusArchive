-- Input: Flyway 迁移引擎
-- Output: 全局修复所有演示数据的文件关联丢失问题
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 目标：修复 "暂无关联文件" 问题（不仅仅是 YONSUITE，也包含 BRJT 等其他数据）
-- 原因：部分演示数据导入时，item_id 未正确关联
-- 策略：全局匹配 archival_code

UPDATE arc_file_content f
SET item_id = a.id
FROM acc_archive a
WHERE f.archival_code = a.archive_code
  AND f.item_id IS NULL;
