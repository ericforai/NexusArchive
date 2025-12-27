-- Input: Flyway 迁移引擎
-- Output: 重置演示用户密码
-- Pos: 数据库迁移脚本

-- 修复：将演示用户 zhangsan 的密码重置为 admin123 (使用与 admin 相同的 Argon2id 哈希)
-- 原因：V71 中的 Bcrypt 哈希可能未被正确识别，或与当前 PasswordEncoder 配置不匹配
UPDATE sys_user 
SET password_hash = '$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA'
WHERE username = 'zhangsan';

-- 顺便也将其他演示用户的密码重置，确保都能登录
UPDATE sys_user 
SET password_hash = '$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA'
WHERE username IN ('lisi', 'wangwu', 'zhaoliu', 'qianqi');
