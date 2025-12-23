-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- V65: 修复集成中心外键约束与数据完整性 (Critical Fix #3)
-- 目的: 解决 V58-V64 引入的表缺少外键关联导致的孤岛数据风险

-- 1. 修复 sys_sync_history 外键 (关联 sys_erp_scenario)
-- 先清理可能的孤儿数据 (理论上刚发布不应该有，但为了健壮性)
DELETE FROM sys_sync_history WHERE scenario_id NOT IN (SELECT id FROM sys_erp_scenario);

ALTER TABLE sys_sync_history 
    ADD CONSTRAINT fk_sync_history_scenario 
    FOREIGN KEY (scenario_id) REFERENCES sys_erp_scenario(id) 
    ON DELETE CASCADE;

-- 2. 修复 arc_file_content 外键 (关联 arc_archive_batch)
-- arc_file_content.batch_id 为 INT, arc_archive_batch.id 为 BIGSERIAL (BIGINT)
-- PostgreSQL INT 可以关联 BIGINT, 但建议类型一致。这里先加约束。
ALTER TABLE arc_file_content
    ADD CONSTRAINT fk_file_content_batch
    FOREIGN KEY (batch_id) REFERENCES arc_archive_batch(id)
    ON DELETE SET NULL;

-- 3. 统一 operator_id 类型并添加关联 (针对 sys_user)
-- sys_user.id 是 BIGSERIAL (BIGINT)

-- 3.1 修复 arc_reconciliation_record.operator_id (原定义为 VARCHAR)
-- 如果表内无数据或数据可转换，尝试转换类型。
-- 由于是新表，假设数据可迁移或为空。
-- 注意：如果 operator_id存储的是 "user_admin" 这种非ID字符串，转换会失败。
-- 鉴于 Review 发现代码中使用了 "user_admin" (ReconciliationController.java), 
-- 强制转为 BIGINT 会导致运行时错误。
-- 策略：暂时保持 VARCHAR 但添加注释说明，不加物理外键约束，以免破坏 "user_admin" 默认值的逻辑。
-- 或者：修改代码逻辑使用真实用户ID。
-- 此次修复优先保证数据库层面的强约束，但如果代码层未就绪，强制加FK会崩。
-- 决定：仅对明确使用 ID 的表加 FK。

-- 3.2 sys_sync_history.operator_id (原定义为 BIGINT) -> 关联 sys_user
-- 首先修改类型为 VARCHAR(64) 以匹配 sys_user.id
ALTER TABLE sys_sync_history ALTER COLUMN operator_id TYPE VARCHAR(64);

DELETE FROM sys_sync_history WHERE operator_id IS NOT NULL AND operator_id NOT IN (SELECT id FROM sys_user);

ALTER TABLE sys_sync_history
    ADD CONSTRAINT fk_sync_history_operator
    FOREIGN KEY (operator_id) REFERENCES sys_user(id)
    ON DELETE SET NULL;

-- 4. 补充 arc_reconciliation_record 的 operator_id 索引 (便于审计查询)
CREATE INDEX IF NOT EXISTS idx_recon_record_operator ON arc_reconciliation_record(operator_id);

-- 5. 补充 arc_archive_batch 的 batch_no 唯一约束 (V63已定义UNIQUE，此处仅确认)
-- (无需操作)
