-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- V61: 同步历史合规增强 - 增加四性检测摘要
-- 支持在同步详情中直接展示合规雷达图数据

ALTER TABLE sys_sync_history ADD COLUMN IF NOT EXISTS four_nature_summary TEXT;
COMMENT ON COLUMN sys_sync_history.four_nature_summary IS 'JSON格式的四性检测统计摘要(真实性、完整性、可用性、安全性通过率)';
