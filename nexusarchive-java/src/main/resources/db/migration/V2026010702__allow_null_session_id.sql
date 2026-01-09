-- Input: scan_workspace 表结构调整
-- Output: 允许 session_id 为 NULL
-- Pos: Flyway 数据库迁移脚本

ALTER TABLE scan_workspace ALTER COLUMN session_id DROP NOT NULL;

COMMENT ON COLUMN scan_workspace.session_id IS '会话ID（用于移动端关联，PC端上传可为空）';
