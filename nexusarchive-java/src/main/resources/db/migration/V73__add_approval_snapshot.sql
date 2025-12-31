-- Input: Destruction Approval Enhancement
-- Output: Schema change for approval chain
-- Pos: db/migration/V73
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 1. 为销毁申请表添加审批链字段
ALTER TABLE biz_destruction ADD COLUMN IF NOT EXISTS approval_snapshot TEXT;

COMMENT ON COLUMN biz_destruction.approval_snapshot IS '审批链快照(JSON格式，包含初审和复核的完整审批信息)';

-- 2. 更新状态字段注释，明确状态流转
COMMENT ON COLUMN biz_destruction.status IS '状态: PENDING(待审批), FIRST_APPROVED(初审通过), DESTRUCTION_APPROVED(审批通过), REJECTED(已拒绝), EXECUTED(已执行)';

