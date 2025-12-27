-- Input: Flyway 迁移引擎
-- Output: 解锁演示用户
-- Pos: 数据库迁移脚本

-- 修复：解锁 zhangsan 账户 (因多次尝试失败被锁定)
-- 同时确保其他演示账户处于 active 状态
UPDATE sys_user 
SET status = 'active' 
WHERE username IN ('zhangsan', 'lisi', 'wangwu', 'zhaoliu', 'qianqi');
